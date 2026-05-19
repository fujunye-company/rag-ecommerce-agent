"""
商品打分排序 — 价格/偏好/证据匹配分
"""
import logging

logger = logging.getLogger("product_ranker")


async def rank_products(
    products: list[dict],
    user_prefs: dict | None = None,
    budget: float | None = None,
) -> list[dict]:
    """
    对候选商品综合打分:
    - 价格匹配度 (预算范围内加分)
    - 偏好匹配度 (场景/标签匹配)
    - 证据置信度 (检索 score)
    返回: 带 match_score 的排序列表
    """
    for p in products:
        score = p.get("score", 0.5)
        # TODO: 价格和偏好匹配逻辑
        p["match_score"] = round(score, 3)

    products.sort(key=lambda p: p["match_score"], reverse=True)
    logger.info("Product ranking: %d candidates ranked", len(products))
    return products
