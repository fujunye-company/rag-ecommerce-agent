"""
意图识别 + 槽位填充 + 查询改写 — LLM-based
"""
import json
import logging
from enum import Enum
from app.services.llm_client import chat_completion

logger = logging.getLogger("intent")


class IntentType(str, Enum):
    RECOMMEND = "commodity_recommend"
    COMPARE = "commodity_compare"
    DETAIL = "commodity_detail"
    SCENARIO = "scenario_shopping"
    AFTERSALE = "after_sales"
    CHITCHAT = "chitchat"
    ANTI_SELECTION = "anti_selection"  # 反选/排除场景（场景4）
    CART = "cart_operation"  # 购物车操作（场景6）
    IMAGE_SEARCH = "image_search"  # 拍照找货（场景7）


INTENT_CLASSIFY_PROMPT = """你是一个电商导购意图分类器。分析用户输入，输出JSON。

意图类型（9选1）：
- commodity_recommend: 推荐商品（"推荐降噪耳机"、"3000以内手机推荐"、"想买个蓝牙音箱"）
- commodity_compare: 对比商品（"XM5和QC45哪个好"、"iPhone和华为怎么选"）
- commodity_detail: 了解商品参数/详情（"这款降噪多少分贝"、"续航多久"）
- scenario_shopping: 场景化购物（"送女朋友礼物"、"北方冬天通勤鞋"、"学生用性价比高的"）
- after_sales: 售后/保修/退换（"保修多久"、"支持7天无理由吗"、"坏了怎么办"）
- chitchat: 闲聊/问候/无关话题（"你好"、"今天天气怎么样"、"谢谢"）
- anti_selection: 反选/排除条件（"不要含酒精的"、"除了日系品牌"、"非白色的"、"不含香精的"）
- cart_operation: 购物车操作（"加入购物车"、"查看购物车"、"删除第三个"、"清空购物车"、"下单"、"结算"）
- image_search: 拍照找货/图片搜索（"这个是什么"、"帮我找同款"、"拍照搜同款"）

用户输入：{query}

只输出JSON（不要markdown代码块）：{"intent": "...", "confidence": 0.XX}"""


SLOT_EXTRACT_PROMPT = """从用户输入中提取购物结构化信息。未知填null。

用户输入：{query}
意图：{intent}

输出JSON（不要markdown代码块）：
{
  "category": "品类" or null,
  "price_min": 数字 or null,
  "price_max": 数字 or null,
  "brand_preference": "品牌" or null,
  "attributes": {"属性名":"属性值"} or {},
  "scenario": "场景" or null,
  "missing_slots": ["缺少的关键信息"]
}"""


QUERY_REWRITE_PROMPT = """将用户口语化查询改写为适合向量检索的结构化查询。保留所有关键信息，去掉语气词。

用户输入：{query}
槽位信息：{slots}

只输出改写后的查询文本（一行纯文本，不要JSON）。"""


NEGATION_EXTRACT_PROMPT = """从用户输入中提取否定/排除条件。用户明确表示"不要"、"除了"、"非"、"不含"的内容。

用户输入：{query}

输出JSON（不要markdown代码块）：
{
  "exclude_brands": ["排除的品牌"] or [],
  "exclude_categories": ["排除的品类"] or [],
  "exclude_attributes": {"属性名":"排除的属性值"} or {},
  "positive_query": "去掉否定词后的正向查询意图"
}"""


async def classify_intent(query: str) -> dict:
    """
    LLM-based 意图分类。
    返回 {"intent": "commodity_recommend", "confidence": 0.92}
    LLM失败时回退到关键词规则。
    """
    try:
        prompt = INTENT_CLASSIFY_PROMPT.format(query=query)
        raw = await chat_completion(
            messages=[{"role": "user", "content": prompt}],
            temperature=0.1,
            max_tokens=100,
        )
        # 清理 markdown 代码块和多余空白
        raw = raw.strip()
        if raw.startswith("```"):
            raw = raw.split("\n", 1)[-1].rsplit("\n```", 1)[0] if "\n```" in raw else raw[3:]
        result = json.loads(raw.strip())
        intent = result.get("intent", "commodity_recommend")
        confidence = float(result.get("confidence", 0.5))
        logger.info("Intent: %s (confidence=%.2f)", intent, confidence)
        return {"intent": intent, "confidence": confidence}
    except Exception as e:
        logger.warning("LLM intent classify failed, fallback to keyword: %s", e)
        return _keyword_classify(query)


def _keyword_classify(query: str) -> dict:
    """关键词规则回退"""
    q = query.lower()
    if any(kw in q for kw in ["对比", "比较", "区别", "哪个好", "怎么选"]):
        return {"intent": "commodity_compare", "confidence": 0.6}
    if any(kw in q for kw in ["售后", "退货", "维修", "保修", "退换"]):
        return {"intent": "after_sales", "confidence": 0.6}
    if any(kw in q for kw in ["送", "礼物", "生日", "节日", "场合", "场景"]):
        return {"intent": "scenario_shopping", "confidence": 0.6}
    if any(kw in q for kw in ["多少", "参数", "配置", "续航", "降噪", "重量", "尺寸"]):
        return {"intent": "commodity_detail", "confidence": 0.6}
    if any(kw in q for kw in ["推荐", "建议", "买", "购", "想搞", "想买", "整一个"]):
        return {"intent": "commodity_recommend", "confidence": 0.6}
    if any(kw in q for kw in ["不要", "除了", "非", "不含", "排除", "去掉"]):
        return {"intent": "anti_selection", "confidence": 0.7}
    if any(kw in q for kw in ["购物车", "加购", "加入", "下单", "结算", "清空"]):
        return {"intent": "cart_operation", "confidence": 0.7}
    if len(q) <= 4 or any(kw in q for kw in ["你好", "谢谢", "天气", "再见"]):
        return {"intent": "chitchat", "confidence": 0.8}
    return {"intent": "commodity_recommend", "confidence": 0.4}


async def extract_slots(query: str, intent: str) -> dict:
    """
    LLM-based 槽位填充。
    返回 {"category": "耳机", "price_max": 3000, ...}
    """
    try:
        prompt = SLOT_EXTRACT_PROMPT.format(query=query, intent=intent)
        raw = await chat_completion(
            messages=[{"role": "user", "content": prompt}],
            temperature=0.1,
            max_tokens=200,
        )
        raw = raw.strip()
        if raw.startswith("```"):
            raw = raw.split("\n", 1)[-1].rsplit("\n```", 1)[0] if "\n```" in raw else raw[3:]
        result = json.loads(raw.strip())
        logger.info("Slots: category=%s, price=%s-%s",
                    result.get("category"), result.get("price_min"), result.get("price_max"))
        return result
    except Exception as e:
        logger.warning("LLM slot extract failed: %s", e)
        return {"category": None, "price_min": None, "price_max": None,
                "brand_preference": None, "attributes": {}, "scenario": None, "missing_slots": []}


async def extract_negation_slots(query: str) -> dict:
    """
    LLM-based 否定条件提取。
    返回 {"exclude_brands": [], "exclude_categories": [], "exclude_attributes": {}, "positive_query": "..."}
    """
    try:
        prompt = NEGATION_EXTRACT_PROMPT.format(query=query)
        raw = await chat_completion(
            messages=[{"role": "user", "content": prompt}],
            temperature=0.1,
            max_tokens=200,
        )
        raw = raw.strip()
        if raw.startswith("```"):
            raw = raw.split("\n", 1)[-1].rsplit("\n```", 1)[0] if "\n```" in raw else raw[3:]
        result = json.loads(raw.strip())
        logger.info("Negation slots: exclude_brands=%s, exclude_attrs=%s",
                    result.get("exclude_brands"), result.get("exclude_attributes"))
        return result
    except Exception as e:
        logger.warning("LLM negation extract failed: %s", e)
        return _keyword_extract_negation(query)


def _keyword_extract_negation(query: str) -> dict:
    """关键词规则提取否定条件（回退）"""
    import re
    exclude_brands = []
    exclude_categories = []
    exclude_attributes = {}

    # 提取"不要XX品牌"、"除了XX"
    brand_patterns = [
        r'(?:不要|除了|非|不含|排除)(?:的|了)?(?:[^，。,\.\s]{1,6})(?:品牌|牌子|系)',
        r'(?:不要|除了|非|不含|排除)\s*([一-鿿]{2,10})',
    ]
    for pat in brand_patterns:
        for m in re.finditer(pat, query):
            word = m.group(1) if m.lastindex else m.group(0)
            exclude_brands.append(word.strip("的了的"))

    # 提取"不要XX颜色/属性"
    attr_patterns = [
        r'(?:不要|非|不含)\s*(\S+?)(?:的|色|款|版)',
    ]
    for pat in attr_patterns:
        for m in re.finditer(pat, query):
            exclude_attributes[m.group(1)] = m.group(0)

    # 提取正向查询（去掉否定部分）
    positive = query
    neg_markers = ["不要", "除了", "非", "不含", "排除"]
    for marker in neg_markers:
        if marker in positive:
            # 取标记之前的正向部分
            idx = positive.find(marker)
            before = positive[:idx].strip("，,。. ")
            after = positive[idx:].split("，")[0] if "，" in positive[idx:] else positive[idx:]
            # 尝试取否定后的其余条件
            rest = positive[idx + len(after):].strip("，,。. ")
            positive = (before + " " + rest).strip()

    return {
        "exclude_brands": exclude_brands,
        "exclude_categories": exclude_categories,
        "exclude_attributes": exclude_attributes,
        "positive_query": positive or query,
    }


async def rewrite_query(query: str, slots: dict) -> str:
    """
    LLM-based 查询改写：口语 → 结构化检索语句。
    """
    try:
        prompt = QUERY_REWRITE_PROMPT.format(query=query, slots=json.dumps(slots, ensure_ascii=False))
        rewritten = await chat_completion(
            messages=[{"role": "user", "content": prompt}],
            temperature=0.1,
            max_tokens=150,
        )
        rewritten = rewritten.strip().strip('"').strip("'")
        if rewritten and rewritten != query:
            logger.info("Query rewritten: '%s' → '%s'", query[:40], rewritten[:60])
            return rewritten
    except Exception as e:
        logger.warning("LLM query rewrite failed: %s", e)
    return query
