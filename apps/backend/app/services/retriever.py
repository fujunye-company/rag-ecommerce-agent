"""
混合检索 — 向量检索 + 关键词检索 + 元数据过滤
"""
import logging
from qdrant_client import AsyncQdrantClient
from qdrant_client.models import Filter, FieldCondition, MatchValue
from app.core.config import settings

logger = logging.getLogger("retriever")

_qdrant = AsyncQdrantClient(url=settings.QDRANT_URL)


async def hybrid_search(
    query_vector: list[float],
    query_text: str = "",
    category: str | None = None,
    price_min: float | None = None,
    price_max: float | None = None,
    top_k: int = 5,
) -> list[dict]:
    """
    混合检索:
    1. 向量语义检索 (dense)
    2. 元数据过滤 (category/price)
    返回: [{id, score, payload}, ...]
    """
    must_filters = []
    if category:
        must_filters.append(
            FieldCondition(key="category", match=MatchValue(value=category))
        )

    qdrant_filter = Filter(must=must_filters) if must_filters else None

    results = await _qdrant.search(
        collection_name=settings.QDRANT_COLLECTION,
        query_vector=query_vector,
        limit=top_k,
        query_filter=qdrant_filter,
        with_payload=True,
    )

    return [
        {"id": hit.id, "score": hit.score, "payload": hit.payload}
        for hit in results
    ]
