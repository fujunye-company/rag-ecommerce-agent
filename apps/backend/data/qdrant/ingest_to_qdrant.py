"""知识库管理 API —— 外部动态调用更新 Qdrant (4096维)

运行环境: Python 3.11+
依赖: pip install fastapi uvicorn qdrant-client transformers torch numpy pydantic
"""
import json
import os
import numpy as np
import torch
from contextlib import asynccontextmanager
from typing import Optional

from transformers import AutoTokenizer, AutoModel

from qdrant_client import QdrantClient
from qdrant_client.models import Distance, VectorParams, PointStruct

from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
import uvicorn

# ── 配置 ──────────────────────────────────────────────────
QDRANT_URL = os.environ.get("QDRANT_URL", "http://localhost:6333")
COLLECTION_NAME = os.environ.get("COLLECTION_NAME", "products")
BASE_DIR = os.path.dirname(os.path.abspath(__file__))
MODEL_PATH = os.path.join(BASE_DIR, "RAGchat", "bge-small-zh")

BASE_DIM = 512
TARGET_DIM = 4096
BATCH_SIZE = 8

# ── 全局单例 ──────────────────────────────────────────────
client: QdrantClient = None
tokenizer = None
transformer = None
projection = None


def _init_embedding():
    global tokenizer, transformer, projection
    tokenizer = AutoTokenizer.from_pretrained(MODEL_PATH)
    transformer = AutoModel.from_pretrained(MODEL_PATH)
    transformer.eval()
    rng = np.random.RandomState(42)
    matrix = rng.randn(BASE_DIM, TARGET_DIM).astype(np.float32)
    norms = np.linalg.norm(matrix, axis=0, keepdims=True)
    projection = matrix / norms


def mean_pooling(last_hidden, attention_mask):
    mask = attention_mask.unsqueeze(-1).float()
    return (last_hidden * mask).sum(dim=1) / mask.sum(dim=1).clamp(min=1e-9)


def encode_texts(texts):
    all_emb = []
    for i in range(0, len(texts), BATCH_SIZE):
        batch = texts[i : i + BATCH_SIZE]
        enc = tokenizer(batch, padding=True, truncation=True, max_length=512, return_tensors="pt")
        with torch.no_grad():
            out = transformer(**enc)
            emb = mean_pooling(out.last_hidden_state, enc["attention_mask"])
            emb = emb / emb.norm(dim=1, keepdim=True)
        all_emb.append(emb.numpy())
    return np.vstack(all_emb)


def embed_to_4096(texts):
    base = encode_texts(texts)
    proj = base @ projection
    proj = proj / np.linalg.norm(proj, axis=1, keepdims=True)
    return proj


def build_document_text(product, reviews):
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


def point_id(pid):
    return abs(hash(str(pid))) % (10 ** 12)


# ── 生命周期 ──────────────────────────────────────────────
@asynccontextmanager
async def lifespan(app: FastAPI):
    global client
    _init_embedding()
    client = QdrantClient(url=QDRANT_URL, timeout=60)
    if not client.collection_exists(COLLECTION_NAME):
        client.create_collection(
            collection_name=COLLECTION_NAME,
            vectors_config=VectorParams(size=TARGET_DIM, distance=Distance.COSINE),
        )
    print(f"Connected to Qdrant at {QDRANT_URL}, collection '{COLLECTION_NAME}' ready")
    yield


app = FastAPI(title="知识库管理 API", version="1.0", lifespan=lifespan)

# ── 请求/响应模型 ─────────────────────────────────────────
class Review(BaseModel):
    rating: int
    nickname: str = ""
    title: str = ""
    text: str = ""
    verified_purchase: bool = True
    helpful_votes: int = 0
    date: str = ""


class ProductInput(BaseModel):
    product_id: str
    title: str
    category: str
    brand: str
    price: float
    rating: float
    rating_count: int = 0
    attributes: dict = {}
    highlights: list[str] = []
    scenarios: list[str] = []
    reviews: list[Review] = []


class BatchInput(BaseModel):
    products: list[ProductInput]


class ProductResponse(BaseModel):
    status: str
    product_id: str
    message: str


# ── 端点 ──────────────────────────────────────────────────

@app.get("/health")
def health():
    exists = client.collection_exists(COLLECTION_NAME)
    count = client.count(collection_name=COLLECTION_NAME, exact=True).count if exists else 0
    return {"status": "ok", "collection": COLLECTION_NAME, "product_count": count}


@app.post("/products/add", response_model=ProductResponse)
def add_product(p: ProductInput):
    """新增或更新单个商品（product_id 相同则覆盖）"""
    prod = p.model_dump()
    reviews = prod.pop("reviews", [])
    review_texts = [r["text"] for r in reviews]
    text = build_document_text(prod, reviews)
    vec = embed_to_4096([text])[0]

    payload = {
        "product_id": prod["product_id"],
        "title": prod["title"],
        "category": prod["category"],
        "brand": prod["brand"],
        "price": prod["price"],
        "rating": prod["rating"],
        "rating_count": prod["rating_count"],
        "attributes": prod.get("attributes", {}),
        "highlights": prod.get("highlights", []),
        "scenarios": prod.get("scenarios", []),
        "reviews": review_texts,
    }

    client.upsert(
        collection_name=COLLECTION_NAME,
        points=[PointStruct(id=point_id(p.product_id), vector=vec.tolist(), payload=payload)],
    )
    return ProductResponse(status="ok", product_id=p.product_id, message="upserted")


@app.post("/products/batch", response_model=ProductResponse)
def add_products_batch(batch: BatchInput):
    """批量新增/更新商品"""
    if not batch.products:
        raise HTTPException(status_code=400, detail="products list is empty")

    texts, pts = [], []
    for p in batch.products:
        prod = p.model_dump()
        reviews = prod.pop("reviews", [])
        review_texts = [r["text"] for r in reviews]
        texts.append(build_document_text(prod, reviews))
        payload = {
            "product_id": prod["product_id"],
            "title": prod["title"],
            "category": prod["category"],
            "brand": prod["brand"],
            "price": prod["price"],
            "rating": prod["rating"],
            "rating_count": prod["rating_count"],
            "attributes": prod.get("attributes", {}),
            "highlights": prod.get("highlights", []),
            "scenarios": prod.get("scenarios", []),
            "reviews": review_texts,
        }
        pts.append(payload)

    vecs = embed_to_4096(texts)
    points = [
        PointStruct(id=point_id(p["product_id"]), vector=vecs[i].tolist(), payload=p)
        for i, p in enumerate(pts)
    ]
    client.upsert(collection_name=COLLECTION_NAME, points=points)
    return ProductResponse(status="ok", product_id="batch", message=f"{len(pts)} products upserted")


@app.delete("/products/{product_id}")
def delete_product(product_id: str):
    """根据 product_id 删除商品"""
    pid = point_id(product_id)
    client.delete(collection_name=COLLECTION_NAME, points_selector=[pid])
    return {"status": "ok", "product_id": product_id, "deleted_point_id": pid}


@app.post("/products/reload")
def reload_from_files():
    """从 seed_products.json + seed_reviews.json 全量重载"""
    with open(os.path.join(BASE_DIR, "seed_products.json"), "r", encoding="utf-8") as f:
        products = json.load(f)
    with open(os.path.join(BASE_DIR, "seed_reviews.json"), "r", encoding="utf-8") as f:
        reviews_list = json.load(f)
    reviews_map = {r["product_id"]: r["reviews"] for r in reviews_list}

    client.delete_collection(COLLECTION_NAME)
    client.create_collection(
        collection_name=COLLECTION_NAME,
        vectors_config=VectorParams(size=TARGET_DIM, distance=Distance.COSINE),
    )

    texts, payloads = [], []
    for prod in products:
        pid = prod["product_id"]
        revs = reviews_map.get(pid, [])
        texts.append(build_document_text(prod, revs))
        payloads.append(
            {
                "product_id": pid,
                "title": prod["title"],
                "category": prod["category"],
                "brand": prod["brand"],
                "price": prod["price"],
                "rating": prod["rating"],
                "rating_count": prod["rating_count"],
                "attributes": prod.get("attributes", {}),
                "highlights": prod.get("highlights", []),
                "scenarios": prod.get("scenarios", []),
                "reviews": [r["text"] for r in revs],
            }
        )

    vecs = embed_to_4096(texts)
    points = [
        PointStruct(id=point_id(p["product_id"]), vector=vecs[i].tolist(), payload=p)
        for i, p in enumerate(payloads)
    ]
    client.upsert(collection_name=COLLECTION_NAME, points=points)
    return {"status": "ok", "message": f"{len(points)} products reloaded"}


# ── 启动入口 ──────────────────────────────────────────────
if __name__ == "__main__":
    uvicorn.run(app, host="0.0.0.0", port=8100)
