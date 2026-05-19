import os
from fastapi import FastAPI, Query
from qdrant_client import QdrantClient

# 防止 localhost 请求走系统代理
os.environ["NO_PROXY"] = "localhost,127.0.0.1,::1"
os.environ["no_proxy"] = "localhost,127.0.0.1,::1"

for key in [
    "HTTP_PROXY",
    "HTTPS_PROXY",
    "ALL_PROXY",
    "http_proxy",
    "https_proxy",
    "all_proxy",
]:
    os.environ.pop(key, None)

COLLECTION_NAME = "products_superlinked_100k"

app = FastAPI(title="RAG Ecommerce Product Vector API")

client = QdrantClient(
    url="http://127.0.0.1:6333",
    timeout=300,
)


@app.get("/health")
def health():
    info = client.get_collection(COLLECTION_NAME)

    return {
        "status": "ok",
        "qdrant_status": str(info.status),
        "collection": COLLECTION_NAME,
        "points_count": info.points_count,
        "vector_config": str(info.config.params.vectors),
    }


@app.get("/products/by-id/{point_id}")
def get_product(point_id: int):
    items = client.retrieve(
        collection_name=COLLECTION_NAME,
        ids=[point_id],
        with_payload=True,
        with_vectors=False,
    )

    if not items:
        return {
            "error": "product not found",
            "point_id": point_id,
        }

    return {
        "point_id": point_id,
        "product": items[0].payload,
    }


@app.get("/products/similar")
def similar_products(
    point_id: int = Query(..., description="Qdrant 商品 point id"),
    limit: int = Query(10, ge=1, le=50),
):
    seed_items = client.retrieve(
        collection_name=COLLECTION_NAME,
        ids=[point_id],
        with_vectors=True,
        with_payload=True,
    )

    if not seed_items:
        return {
            "error": "point_id not found",
            "point_id": point_id,
        }

    seed = seed_items[0]

    # 多查 1 个，因为第 1 个通常是自己
    response = client.query_points(
        collection_name=COLLECTION_NAME,
        query=seed.vector,
        limit=limit + 1,
        with_payload=True,
        with_vectors=False,
    )

    results = []

    for item in response.points:
        if item.id == point_id:
            continue

        payload = item.payload or {}

        results.append(
            {
                "point_id": item.id,
                "score": item.score,
                "product": {
                    "parent_asin": payload.get("parent_asin"),
                    "title": payload.get("title"),
                    "main_category": payload.get("main_category"),
                    "categories": payload.get("categories"),
                    "price": payload.get("price"),
                    "average_rating": payload.get("average_rating"),
                    "rating_number": payload.get("rating_number"),
                    "description": payload.get("description"),
                    "image_url": payload.get("image_url"),
                },
            }
        )

        if len(results) >= limit:
            break

    return {
        "seed": {
            "point_id": point_id,
            "product": seed.payload,
        },
        "results": results,
    }