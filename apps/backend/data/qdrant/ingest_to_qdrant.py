"""商品数据 + 评价 → 向量化 → Qdrant 入库 (bge-large-zh-v1.5, 1024维)

运行环境: Python 3.11+, ~/.hermes-venv
依赖: pip install qdrant-client sentence-transformers
用法: python ingest_to_qdrant.py
"""

import json
import os
from qdrant_client import QdrantClient
from qdrant_client.http import models
from sentence_transformers import SentenceTransformer

# ── 配置 ──────────────────────────────────────────────────
QDRANT_URL = os.environ.get("QDRANT_URL", "http://localhost:6333")
COLLECTION_NAME = os.environ.get("QDRANT_COLLECTION", "products")
EMBEDDING_MODEL = "BAAI/bge-large-zh-v1.5"      # 1024 维，无需投影
BATCH_SIZE = 32                                   # GPU 推理批次大小
BASE_DIR = os.path.dirname(os.path.abspath(__file__))

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


def main():
    print(f"加载 Embedding 模型: {EMBEDDING_MODEL} ...")
    model = SentenceTransformer(EMBEDDING_MODEL)
    dim = model.get_sentence_embedding_dimension()
    print(f"  向量维度: {dim}")

    print(f"连接 Qdrant: {QDRANT_URL} ...")
    client = QdrantClient(url=QDRANT_URL, timeout=60)

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
            size=dim,
            distance=models.Distance.COSINE,
        ),
    )
    print(f"Collection '{COLLECTION_NAME}' 已重建 ({dim}维)")

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
            "image_urls": prod.get("image_urls", []),
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
            id=abs(hash(p["product_id"])) % (10 ** 12),
            vector=emb.tolist(),
            payload=p,
        )
        for emb, p in zip(embeddings, payloads)
    ]

    client.upsert(collection_name=COLLECTION_NAME, points=points)
    print(f"入库完成: {len(points)} 件商品")

    # 验证
    count = client.count(collection_name=COLLECTION_NAME, exact=True).count
    print(f"验证: Qdrant 中现有 {count} 个向量")
    print("✅ 全部完成！访问 http://localhost:6333/dashboard 查看")


if __name__ == "__main__":
    main()
