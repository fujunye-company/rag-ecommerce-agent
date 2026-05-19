import os
from qdrant_client import QdrantClient

os.environ["NO_PROXY"] = "localhost,127.0.0.1,::1"
os.environ["no_proxy"] = "localhost,127.0.0.1,::1"

for key in ["HTTP_PROXY", "HTTPS_PROXY", "ALL_PROXY", "http_proxy", "https_proxy", "all_proxy"]:
    os.environ.pop(key, None)

COLLECTION_NAME = "products_superlinked_100k"


def main():
    client = QdrantClient(url="http://127.0.0.1:6333", timeout=300)

    seed_items = client.retrieve(
        collection_name=COLLECTION_NAME,
        ids=[0],
        with_vectors=True,
        with_payload=True,
    )

    if not seed_items:
        raise RuntimeError("没有找到 point_id=0，请确认导入是否成功。")

    seed = seed_items[0]

    print("种子商品：")
    print("标题:", seed.payload.get("title"))
    print("类目:", seed.payload.get("main_category"))
    print("价格:", seed.payload.get("price"))
    print("评分:", seed.payload.get("average_rating"))

    response = client.query_points(
        collection_name=COLLECTION_NAME,
        query=seed.vector,
        limit=10,
        with_payload=True,
        with_vectors=False,
    )

    results = response.points

    print("\n相似商品召回结果：")
    for idx, item in enumerate(results, start=1):
        payload = item.payload
        print("=" * 80)
        print("排名:", idx)
        print("相似度:", item.score)
        print("Point ID:", item.id)
        print("ASIN:", payload.get("parent_asin"))
        print("标题:", payload.get("title"))
        print("类目:", payload.get("main_category"))
        print("价格:", payload.get("price"))
        print("评分:", payload.get("average_rating"))
        print("图片:", payload.get("image_url"))


if __name__ == "__main__":
    main()