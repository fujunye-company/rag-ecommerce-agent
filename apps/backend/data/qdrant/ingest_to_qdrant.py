"""商品数据 + 评价 → 向量化 → Qdrant 入库 (bge-large-zh-v1.5, 1024维)

运行环境: Python 3.11+, ~/.hermes-venv
依赖: pip install qdrant-client sentence-transformers
用法:
  python ingest_to_qdrant.py                        # 重建: 从 seed_products.json 全量导入
  python ingest_to_qdrant.py --input expanded.jsonl  # 追加: 从 JSONL 追加新商品(upsert)
"""

import argparse
import json
import os
import uuid
from qdrant_client import QdrantClient
from qdrant_client.http import models
from sentence_transformers import SentenceTransformer

# ── 配置 ──────────────────────────────────────────────────
QDRANT_URL = os.environ.get("QDRANT_URL", "http://localhost:6333")
COLLECTION_NAME = os.environ.get("QDRANT_COLLECTION", "products")
EMBEDDING_MODEL = "BAAI/bge-large-zh-v1.5"      # 1024 维，无需投影
BATCH_SIZE = 32                                   # GPU 推理批次大小
BASE_DIR = os.path.dirname(os.path.abspath(__file__))

# UUID namespace for deterministic point IDs — avoids md5 hash collisions
QDRANT_NAMESPACE = uuid.UUID("6ba7b810-9dad-11d1-80b4-00c04fd430c8")


def product_id_to_uuid(product_id: str) -> str:
    """Deterministic UUID v5 from product_id — no collisions, safe for upsert."""
    return str(uuid.uuid5(QDRANT_NAMESPACE, product_id))

# ── 主流程 ────────────────────────────────────────────────

def build_document_text(product, reviews):
    """构造用于向量化的文本"""
    attrs = " ".join(f"{k}:{v}" for k, v in product.get("attributes", {}).items())
    highlights = " ".join(product.get("highlights", []))
    scenarios = " ".join(product.get("scenarios", []))
    review_texts = " ".join(
        f"评分{r['rating']}星: {r['text']}" for r in reviews[:5]
    )
    return (
        f"商品名称: {product['title']} "
        f"品牌: {product['brand']} "
        f"分类: {product['category']} "
        f"价格: {product['price']}元 "
        f"评分: {product['rating']}分 "
        f"属性: {attrs} "
        f"亮点: {highlights} "
        f"场景: {scenarios} "
        f"用户评价: {review_texts}"
    )


def build_doc_text_from_record(prod):
    """从 ProductRecord (JSONL 行) 构造向量化文本，无需额外 reviews 文件"""
    attrs = " ".join(f"{k}:{v}" for k, v in prod.get("attributes", {}).items())
    highlights = " ".join(prod.get("highlights", []))
    scenarios = " ".join(prod.get("scenarios", []))
    description = prod.get("description", "")
    review_texts = prod.get("review_summary", "")
    return (
        f"商品名称: {prod['title']} "
        f"品牌: {prod.get('brand', '')} "
        f"分类: {prod['category']} "
        f"价格: {prod['price']}元 "
        f"评分: {prod['rating']}分 "
        f"属性: {attrs} "
        f"亮点: {highlights} "
        f"场景: {scenarios} "
        f"描述: {description} "
        f"用户评价: {review_texts}"
    )


def build_payload_from_record(prod):
    """从 ProductRecord 构建 Qdrant payload"""
    return {
        "product_id": prod["product_id"],
        "title": prod["title"],
        "category": prod["category"],
        "brand": prod.get("brand", ""),
        "price": prod.get("price", 0),
        "rating": prod.get("rating", 3.0),
        "rating_count": prod.get("rating_count", 0),
        "attributes": prod.get("attributes", {}),
        "highlights": prod.get("highlights", []),
        "scenarios": prod.get("scenarios", []),
        "image_url": prod.get("image_url") or (prod.get("image_urls", [None]) or [None])[0],
        "image_urls": prod.get("image_urls", []),
        "source": prod.get("source", ""),
    }


def ingest_jsonl(client, model, jsonl_path):
    """从 JSONL 追加新商品 (upsert，不重建 collection)"""
    products = []
    with open(jsonl_path, "r", encoding="utf-8") as f:
        for line in f:
            line = line.strip()
            if line:
                products.append(json.loads(line))
    print(f"读取 JSONL: {len(products)} 条")

    texts = [build_doc_text_from_record(p) for p in products]
    payloads = [build_payload_from_record(p) for p in products]

    print(f"向量化 (batch_size={BATCH_SIZE}) ...")
    embeddings = model.encode(
        texts,
        batch_size=BATCH_SIZE,
        show_progress_bar=True,
        normalize_embeddings=True,
    )

    points = [
        models.PointStruct(
            id=product_id_to_uuid(p["product_id"]),
            vector=emb.tolist(),
            payload=pl,
        )
        for emb, p, pl in zip(embeddings, products, payloads)
    ]

    client.upsert(collection_name=COLLECTION_NAME, points=points)
    print(f"追加入库完成: {len(points)} 条 (upsert)")


def rebuild_from_seed(client, model):
    """重建 collection 并从 seed_products.json + seed_reviews.json 全量导入"""
    # 读取种子数据
    products_path = os.path.join(BASE_DIR, "seed_products.json")
    reviews_path = os.path.join(BASE_DIR, "seed_reviews.json")

    with open(products_path, "r", encoding="utf-8") as f:
        products = json.load(f)
    with open(reviews_path, "r", encoding="utf-8") as f:
        reviews_list = json.load(f)

    reviews_map = {r["product_id"]: r["reviews"] for r in reviews_list}
    print(f"读取: {len(products)} 件商品, {len(reviews_list)} 组评价")

    # 重建 Collection（清空旧数据）
    if client.collection_exists(COLLECTION_NAME):
        client.delete_collection(COLLECTION_NAME)
    client.create_collection(
        collection_name=COLLECTION_NAME,
        vectors_config=models.VectorParams(
            size=model.get_sentence_embedding_dimension(),
            distance=models.Distance.COSINE,
        ),
    )
    print(f"Collection '{COLLECTION_NAME}' 已重建 ({model.get_sentence_embedding_dimension()}维)")

    # 向量化 + 入库
    texts = []
    payloads = []
    for prod in products:
        pid = prod["product_id"]
        revs = reviews_map.get(pid, [])
        texts.append(build_document_text(prod, revs))
        payloads.append({
            "product_id": pid,
            "title": prod["title"],
            "category": prod["category"],
            "brand": prod["brand"],
            "price": prod["price"],
            "rating": prod["rating"],
            "rating_count": prod.get("rating_count", 0),
            "attributes": prod.get("attributes", {}),
            "highlights": prod.get("highlights", []),
            "scenarios": prod.get("scenarios", []),
            "image_url": prod.get("image_url") or (prod.get("image_urls", [None]) or [None])[0],
            "image_urls": prod.get("image_urls", []),
            "source": prod.get("source", ""),
        })

    print(f"向量化 {len(texts)} 件商品 (batch_size={BATCH_SIZE}) ...")
    embeddings = model.encode(
        texts,
        batch_size=BATCH_SIZE,
        show_progress_bar=True,
        normalize_embeddings=True,
    )

    points = [
        models.PointStruct(
            id=product_id_to_uuid(p["product_id"]),
            vector=emb.tolist(),
            payload=p,
        )
        for emb, p in zip(embeddings, payloads)
    ]

    client.upsert(collection_name=COLLECTION_NAME, points=points)
    print(f"入库完成: {len(points)} 件商品")


def main():
    parser = argparse.ArgumentParser(description="商品数据向量化 → Qdrant 入库")
    parser.add_argument("--input", type=str, default=None,
                        help="JSONL 文件路径 (追加模式，不重建 collection)")
    args = parser.parse_args()

    print(f"加载 Embedding 模型: {EMBEDDING_MODEL} ...")
    model = SentenceTransformer(EMBEDDING_MODEL)
    dim = model.get_sentence_embedding_dimension()
    print(f"  向量维度: {dim}")

    print(f"连接 Qdrant: {QDRANT_URL} ...")
    client = QdrantClient(url=QDRANT_URL, timeout=60)

    if args.input:
        # 追加模式：upsert 新商品到现有 collection
        if not client.collection_exists(COLLECTION_NAME):
            print(f"Collection '{COLLECTION_NAME}' 不存在，先创建 ...")
            client.create_collection(
                collection_name=COLLECTION_NAME,
                vectors_config=models.VectorParams(
                    size=dim,
                    distance=models.Distance.COSINE,
                ),
            )
        before_count = client.count(collection_name=COLLECTION_NAME, exact=True).count
        print(f"Qdrant 当前向量数: {before_count}")

        ingest_jsonl(client, model, args.input)

        after_count = client.count(collection_name=COLLECTION_NAME, exact=True).count
        print(f"Qdrant 更新后向量数: {after_count} (新增约 {after_count - before_count} 条)")
    else:
        # 重建模式：删除旧 collection，从 seed_products.json 全量导入
        rebuild_from_seed(client, model)
        count = client.count(collection_name=COLLECTION_NAME, exact=True).count
        print(f"验证: Qdrant 中现有 {count} 个向量")

    print("全部完成！访问 http://localhost:6333/dashboard 查看")


if __name__ == "__main__":
    main()
