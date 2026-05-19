import os
import time
from pathlib import Path
from typing import Any

import pyarrow.dataset as ds
from qdrant_client import QdrantClient
from qdrant_client.models import Distance, VectorParams, PointStruct
from tqdm import tqdm

# 禁止 localhost 走系统代理
os.environ["NO_PROXY"] = "localhost,127.0.0.1,::1"
os.environ["no_proxy"] = "localhost,127.0.0.1,::1"

for key in ["HTTP_PROXY", "HTTPS_PROXY", "ALL_PROXY", "http_proxy", "https_proxy", "all_proxy"]:
    os.environ.pop(key, None)

BASE_DIR = Path(__file__).resolve().parents[1]

DATA_DIR = (
    BASE_DIR
    / "data"
    / "raw"
    / "superlinked_external_benchmarking"
    / "benchmark-100k"
)

COLLECTION_NAME = "products_superlinked_100k"
VECTOR_SIZE = 4154
BATCH_SIZE = 32
MAX_RETRIES = 5


def safe_value(value: Any):
    if value is None:
        return None
    if hasattr(value, "as_py"):
        value = value.as_py()
    return value


def clean_payload(row: dict) -> dict:
    return {
        "parent_asin": safe_value(row.get("parent_asin")),
        "main_category": safe_value(row.get("main_category")),
        "title": safe_value(row.get("title")),
        "average_rating": safe_value(row.get("average_rating")),
        "rating_number": safe_value(row.get("rating_number")),
        "description": safe_value(row.get("description")),
        "price": safe_value(row.get("price")),
        "categories": safe_value(row.get("categories")),
        "image_url": safe_value(row.get("image_url")),
    }


def upsert_with_retry(client: QdrantClient, points: list[PointStruct]):
    for attempt in range(1, MAX_RETRIES + 1):
        try:
            client.upsert(
                collection_name=COLLECTION_NAME,
                points=points,
                wait=True,
            )
            return
        except Exception as e:
            print(f"\nupsert 失败，第 {attempt}/{MAX_RETRIES} 次重试：{e}")
            time.sleep(5 * attempt)

    raise RuntimeError("多次重试后仍然 upsert 失败，请检查 Qdrant 容器或代理设置。")


def main():
    if not DATA_DIR.exists():
        raise FileNotFoundError(f"找不到数据目录：{DATA_DIR}")

    parquet_files = list(DATA_DIR.glob("*.parquet"))
    if len(parquet_files) != 100:
        raise RuntimeError(f"parquet 文件数量不是 100，当前为：{len(parquet_files)}")

    print("数据目录:", DATA_DIR)
    print("连接 Qdrant: http://127.0.0.1:6333")

    client = QdrantClient(url="http://127.0.0.1:6333", timeout=300)

    if not client.collection_exists(COLLECTION_NAME):
        print(f"collection 不存在，创建 collection: {COLLECTION_NAME}")
        client.create_collection(
            collection_name=COLLECTION_NAME,
            vectors_config=VectorParams(
                size=VECTOR_SIZE,
                distance=Distance.COSINE,
            ),
        )
        start_index = 0
    else:
        info = client.get_collection(COLLECTION_NAME)
        start_index = int(info.points_count or 0)
        print(f"collection 已存在，当前 points_count={start_index}，将从该位置继续导入。")

    dataset = ds.dataset(str(DATA_DIR), format="parquet")
    total_rows = dataset.count_rows()
    print("总行数:", total_rows)

    if start_index >= total_rows:
        print("已经导入完成，不需要继续。")
        return

    points = []
    current_index = 0

    scanner = dataset.scanner(batch_size=BATCH_SIZE)

    print("开始断点续传导入 Qdrant...")
    with tqdm(total=total_rows, initial=start_index) as pbar:
        for record_batch in scanner.to_batches():
            rows = record_batch.to_pylist()

            for row in rows:
                point_id = current_index

                if current_index < start_index:
                    current_index += 1
                    continue

                vector = row.get("value")

                if vector is not None and len(vector) == VECTOR_SIZE:
                    points.append(
                        PointStruct(
                            id=point_id,
                            vector=vector,
                            payload=clean_payload(row),
                        )
                    )

                current_index += 1
                pbar.update(1)

                if len(points) >= BATCH_SIZE:
                    upsert_with_retry(client, points)
                    points = []

    if points:
        upsert_with_retry(client, points)

    info = client.get_collection(COLLECTION_NAME)
    print("导入结束。")
    print("points_count:", info.points_count)
    print("status:", info.status)


if __name__ == "__main__":
    main()