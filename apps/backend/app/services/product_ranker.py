"""
多维加权商品排序器

根据用户意图动态调整各维度权重，对商品列表计算匹配分并排序。
接口: (products, user_prefs, intent) → ranked list with match_score
"""
import math
import logging
from typing import List, Dict, Optional, Tuple
from enum import Enum

logger = logging.getLogger("product_ranker")


class Intent(str, Enum):
    RECOMMEND = "commodity_recommend"
    COMPARE = "commodity_compare"
    DETAIL = "commodity_detail"
    SCENARIO = "scenario_shopping"
    AFTER_SALES = "after_sales"
    ANTI_SELECTION = "anti_selection"


# === 维度权重矩阵 ===
# 不同意图下，各维度的权重不同
INTENT_WEIGHTS: Dict[Intent, Dict[str, float]] = {
    Intent.RECOMMEND: {
        "semantic": 0.40, "price": 0.20, "rating": 0.15, "brand": 0.10, "attributes": 0.15
    },
    Intent.COMPARE: {
        "semantic": 0.20, "price": 0.25, "rating": 0.20, "brand": 0.10, "attributes": 0.25
    },
    Intent.DETAIL: {
        "semantic": 0.15, "price": 0.10, "rating": 0.15, "brand": 0.05, "attributes": 0.55
    },
    Intent.SCENARIO: {
        "semantic": 0.35, "price": 0.30, "rating": 0.10, "brand": 0.10, "attributes": 0.15
    },
    Intent.AFTER_SALES: {
        "semantic": 0.20, "price": 0.05, "rating": 0.10, "brand": 0.05, "attributes": 0.60
    },
    Intent.ANTI_SELECTION: {
        "semantic": 0.30, "price": 0.20, "rating": 0.10, "brand": 0.25, "attributes": 0.15
    },
}


class ProductRanker:
    """多维加权商品排序器"""

    def __init__(self):
        self.default_weights = INTENT_WEIGHTS[Intent.RECOMMEND]

    def rank(
        self,
        products: List[Dict],
        user_prefs: Optional[Dict] = None,
        intent: str = "commodity_recommend",
        top_k: int = 10
    ) -> List[Dict]:
        """
        对商品列表计算匹配分并排序。

        Args:
            products: [{"title":..., "price":..., "rating":..., "brand":...,
                        "attributes":{...}, "semantic_score":...}, ...]
            user_prefs: {"price_min":..., "price_max":..., "brand_preference":...,
                         "attributes":{...}, "exclude_brands":[...], "exclude_attributes":{...}}
            intent: 意图类型
            top_k: 返回 Top-K

        Returns:
            排序后的商品列表（浅拷贝），新增 match_score, dimension_scores, rank_reason 字段
        """
        # 防御：None 输入
        if products is None:
            return []

        if user_prefs is None:
            user_prefs = {}

        try:
            intent_enum = Intent(intent)
        except ValueError:
            intent_enum = Intent.RECOMMEND

        weights = INTENT_WEIGHTS.get(intent_enum, self.default_weights)

        budget_min = user_prefs.get("price_min")
        budget_max = user_prefs.get("price_max")

        # p_min > p_max 守卫：交换非法对
        if budget_min is not None and budget_max is not None and budget_min > budget_max:
            budget_min, budget_max = budget_max, budget_min

        preferred_brand = user_prefs.get("brand_preference")
        preferred_attrs = user_prefs.get("attributes", {})

        # 排除项
        exclude_brands = set(user_prefs.get("exclude_brands") or [])
        exclude_attrs = user_prefs.get("exclude_attributes", {})

        scored = []
        for prod in products:
            # 排除过滤
            if exclude_brands and prod.get("brand") in exclude_brands:
                continue
            if exclude_attrs:
                prod_attrs = prod.get("attributes", {})
                excluded = any(
                    k in prod_attrs and str(prod_attrs[k]) == str(v)
                    for k, v in exclude_attrs.items()
                )
                if excluded:
                    continue

            dims = {}

            # 1. 语义匹配分（来自向量检索或 reranker）
            dims["semantic"] = float(prod.get("semantic_score",
                                       prod.get("rerank_score",
                                       prod.get("final_score",
                                       prod.get("score", 0.5)))))

            # 2. 价格匹配分
            dims["price"] = self._price_score(
                float(prod.get("price", 0)), budget_min, budget_max
            )

            # 3. 评分
            rating = float(prod.get("rating", 3.0))
            dims["rating"] = min(rating / 5.0, 1.0)

            # 4. 品牌偏好
            prod_brand = prod.get("brand", "")
            dims["brand"] = 1.0 if (preferred_brand and prod_brand == preferred_brand) else 0.5

            # 5. 属性匹配
            dims["attributes"] = self._attribute_score(
                prod.get("attributes", {}), preferred_attrs
            )

            # 加权总分
            total = sum(weights[k] * dims[k] for k in weights)

            # ANTI_SELECTION：反转评分 — 与偏好差异越大排名越高
            if intent_enum == Intent.ANTI_SELECTION:
                total = 1.0 - total

            # 生成排序理由
            reason = self._generate_reason(dims, weights, intent_enum)

            # 浅拷贝避免污染原数据
            entry = {**prod, "dimension_scores": dims, "match_score": round(total, 4),
                     "rank_reason": reason}
            scored.append(entry)

        # 带二级排序键（semantic_score 作为 tie-breaker）
        ranked = sorted(scored, key=lambda x: (x["match_score"],
                                                x.get("dimension_scores", {}).get("semantic", 0)),
                        reverse=True)

        result = ranked[:top_k]
        logger.info("ProductRanker: intent=%s, %d → %d results",
                    intent, len(products), len(result))
        return result

    def _price_score(
        self,
        price: float,
        p_min: Optional[float],
        p_max: Optional[float]
    ) -> float:
        """价格匹配度：在预算内=1.0，超出越多分越低"""
        if price <= 0:
            return 0.5

        if p_min is not None and p_max is not None:
            if p_min <= price <= p_max:
                return 1.0
            mid = (p_min + p_max) / 2
            deviation = abs(price - mid) / mid if mid > 0 else 1.0
            return max(0.1, math.exp(-deviation))

        if p_max is not None:
            if price <= p_max:
                return 1.0
            return max(0.1, p_max / price)

        if p_min is not None:
            if price >= p_min:
                return 1.0
            return max(0.1, price / p_min)

        return 0.7

    def _attribute_score(
        self,
        prod_attrs: Dict,
        preferred_attrs: Dict
    ) -> float:
        """属性匹配度：所需属性中有多少满足"""
        if not preferred_attrs:
            return 0.5
        matches = 0
        for k, v in preferred_attrs.items():
            if k in prod_attrs and str(prod_attrs[k]) == str(v):
                matches += 1
        return matches / len(preferred_attrs) if preferred_attrs else 0.5

    def _generate_reason(self, dims: Dict, weights: Dict, intent_enum: Intent = None) -> str:
        """生成简短的排序理由"""
        if intent_enum == Intent.ANTI_SELECTION:
            return "反向推荐：与偏好差异较大"
        parts = []
        for dim, weight in sorted(weights.items(), key=lambda x: x[1], reverse=True):
            if weight < 0.1:
                continue
            if dim == "semantic" and dims["semantic"] > 0.7:
                parts.append("语义高度匹配")
            elif dim == "price" and dims["price"] >= 1.0:
                parts.append("在预算内")
            elif dim == "price" and dims["price"] >= 0.8:
                parts.append("价格接近预算")
            elif dim == "rating" and dims["rating"] > 0.8:
                parts.append("高评分")
            elif dim == "brand" and dims["brand"] >= 1.0:
                parts.append("品牌匹配")
            elif dim == "attributes" and dims["attributes"] >= 0.8:
                parts.append("属性匹配")
        return "、".join(parts[:3]) if parts else "综合匹配"


# 单例
_ranker: Optional[ProductRanker] = None


def get_ranker() -> ProductRanker:
    global _ranker
    if _ranker is None:
        _ranker = ProductRanker()
    return _ranker


def rank_products(
    products: List[Dict],
    user_prefs: Optional[Dict] = None,
    intent: str = "commodity_recommend",
    top_k: int = 10
) -> List[Dict]:
    """便捷函数：对商品打分排序"""
    return get_ranker().rank(products, user_prefs, intent, top_k)
