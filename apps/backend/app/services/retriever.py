"""
混合检索 — 向量检索 + 关键词检索 + 元数据过滤
"""
import time
import logging
from qdrant_client import AsyncQdrantClient
from qdrant_client.models import Filter, FieldCondition, MatchValue, MatchAny, Range
from app.core.config import settings

logger = logging.getLogger("retriever")

_qdrant = None


def _get_qdrant():
    """懒加载 Qdrant 客户端（避免模块导入时崩溃）"""
    global _qdrant
    if _qdrant is None:
        _qdrant = AsyncQdrantClient(url=settings.QDRANT_URL)
    return _qdrant


async def hybrid_search(
    query_vector: list[float],
    query_text: str = "",
    category: str | None = None,
    price_min: float | None = None,
    price_max: float | None = None,
    exclude_brands: list[str] | None = None,
    exclude_categories: list[str] | None = None,
    exclude_attributes: dict[str, str] | None = None,
    top_k: int = 10,
) -> tuple[list[dict], float]:
    """
    混合检索:
    1. 向量语义检索 (dense)
    2. 元数据过滤 (category / price range)
    3. 否定排除过滤 (场景4: 排除品牌/品类/属性)
    返回: (结果列表, 检索耗时ms)
    """
    t0 = time.monotonic()

    must_filters = []
    must_not_filters = []

    if category:
        must_filters.append(
            FieldCondition(key="category", match=MatchValue(value=category))
        )
    if price_min is not None or price_max is not None:
        price_range = {}
        if price_min is not None:
            price_range["gte"] = price_min
        if price_max is not None:
            price_range["lte"] = price_max
        must_filters.append(
            FieldCondition(key="price", range=Range(**price_range))
        )

    # 否定排除过滤 (场景4: 反选/排除)
    if exclude_brands:
        must_not_filters.append(
            FieldCondition(key="brand", match=MatchAny(any=exclude_brands))
        )
    if exclude_categories:
        must_not_filters.append(
            FieldCondition(key="category", match=MatchAny(any=exclude_categories))
        )
    if exclude_attributes:
        for attr_key, attr_val in exclude_attributes.items():
            # JSONB 属性排除 — 使用 payload 字段匹配
            must_not_filters.append(
                FieldCondition(key=f"attributes.{attr_key}", match=MatchValue(value=attr_val))
            )

    qdrant_filter = None
    if must_filters or must_not_filters:
        qdrant_filter = Filter(
            must=must_filters if must_filters else None,
            must_not=must_not_filters if must_not_filters else None,
        )

    try:
        results = await _get_qdrant().query_points(
            collection_name=settings.QDRANT_COLLECTION,
            query=query_vector,
            limit=top_k,
            query_filter=qdrant_filter,
            with_payload=True,
        )
    except Exception as e:
        logger.error("Qdrant query failed in hybrid_search: %s", e)
        elapsed_ms = (time.monotonic() - t0) * 1000
        return [], elapsed_ms

    elapsed_ms = (time.monotonic() - t0) * 1000
    items = [
        {"id": hit.id, "score": hit.score, "payload": hit.payload}
        for hit in results.points
    ]

    logger.info(
        "Retrieve: query='%s' → %d results in %.0fms (category=%s, price=%s-%s, exclude_br=%s)",
        query_text[:50], len(items), elapsed_ms,
        category or "*", price_min or "*", price_max or "*",
        exclude_brands or [],
    )

    return items, elapsed_ms


async def search_similar_products(
    query_text: str,
    top_k: int = 8,
) -> list[dict]:
    """
    拍照找货专用：文本查询 → Embedding → Qdrant 向量检索 → 结构化商品列表

    Args:
        query_text: 由 VLM 提取的商品描述文本
        top_k: 返回数量

    Returns:
        [{"product_id":..., "title":..., "price":..., "rating":...,
          "match_score":..., "highlights":..., "image_url":...}, ...]
    """
    from app.services.embedding import embed_text

    query_vector = await embed_text(query_text)

    try:
        results = await _get_qdrant().query_points(
            collection_name=settings.QDRANT_COLLECTION,
            query=query_vector,
            limit=top_k,
            with_payload=True,
        )
    except Exception as e:
        logger.error("Qdrant query failed in search_similar_products: %s", e)
        return []

    products = []
    for hit in results.points:
        p = hit.payload or {}
        products.append({
            "product_id": p.get("product_id", ""),
            "title": p.get("title", ""),
            "price": p.get("price", 0),
            "rating": p.get("rating", 0),
            "brand": p.get("brand", ""),
            "category": p.get("category", ""),
            "match_score": round(hit.score, 4) if hit.score else 0.0,
            "score": round(hit.score, 4) if hit.score else 0.0,
            "highlights": p.get("highlights", [])[:3],
            "image_url": p.get("image_url"),
            "image_urls": p.get("image_urls", []),
        })

    logger.info("Similar search: '%s' → %d products", query_text[:60], len(products))
    return products
