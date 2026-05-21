"""
RAG 检索入口 — 编排 retrieval pipeline
"""
import time
import logging
from app.services.retriever import hybrid_search
from app.services.embedding import embed_text

logger = logging.getLogger("rag")


async def retrieve(
    query: str,
    top_k: int = 10,
    category: str | None = None,
    price_min: float | None = None,
    price_max: float | None = None,
    exclude_brands: list[str] | None = None,
    exclude_categories: list[str] | None = None,
    exclude_attributes: dict[str, str] | None = None,
) -> dict:
    """
    RAG 检索主入口:
    1. query → embedding
    2. 向量 + 元数据混合检索（含否定排除）
    3. 返回 {'chunks': [...], 'latency_ms': float}
    """
    t0 = time.monotonic()

    vector = await embed_text(query)
    chunks, search_ms = await hybrid_search(
        query_vector=vector,
        query_text=query,
        category=category,
        price_min=price_min,
        price_max=price_max,
        exclude_brands=exclude_brands,
        exclude_categories=exclude_categories,
        exclude_attributes=exclude_attributes,
        top_k=top_k,
    )

    total_ms = (time.monotonic() - t0) * 1000
    logger.info(
        "RAG retrieve: '%s' → %d chunks (embed+search=%.0fms)",
        query[:60], len(chunks), total_ms,
    )

    return {
        "chunks": chunks,
        "latency_ms": round(total_ms, 1),
        "query": query,
    }
