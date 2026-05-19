"""
意图识别 + 查询改写
负责: 用户口语 → 结构化意图 + 检索条件
"""
from enum import Enum


class IntentType(str, Enum):
    RECOMMEND = "recommend"       # 推荐商品
    COMPARE = "compare"           # 商品对比
    CONSULT = "consult"           # 参数咨询
    AFTERSALE = "aftersale"       # 售后问题
    CLARIFY = "clarify"           # 需要追问


async def classify_intent(query: str) -> IntentType:
    """识别用户意图 (MVP: 关键词规则, 全量: LLM分类)"""
    query_lower = query.lower()
    if any(kw in query_lower for kw in ["推荐", "建议", "买什么", "哪个好"]):
        return IntentType.RECOMMEND
    if any(kw in query_lower for kw in ["对比", "比较", "区别"]):
        return IntentType.COMPARE
    if any(kw in query_lower for kw in ["售后", "退货", "维修"]):
        return IntentType.AFTERSALE
    return IntentType.CONSULT


async def extract_slots(query: str) -> dict:
    """抽取需求槽位: 品类/价格/品牌/场景/偏好"""
    # TODO: MVP用规则, 全量用LLM
    return {"category": None, "price_min": None, "price_max": None, "brand": None, "scenario": None}


async def rewrite_query(query: str, slots: dict) -> str:
    """查询改写: 口语 → 结构化检索语句"""
    return query
