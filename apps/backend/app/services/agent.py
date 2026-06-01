"""
Agent 编排 — LangGraph StateGraph 工作流
流程: classify_intent → (missing_slots? → clarify → generate | retrieve → generate) → SSE stream
"""
import hashlib
import json
import logging
import time
from typing import TypedDict, AsyncGenerator
from langgraph.graph import StateGraph, END
from app.services.intent import classify_intent, extract_slots, rewrite_query, extract_negation_slots
from app.services.rag import retrieve as rag_retrieve
from app.services.reranker import rerank_async
from app.services.llm_client import chat_completion
from app.schemas.sse_events import TextDeltaEvent, ProductCardEvent, DoneEvent, ErrorEvent, ProgressEvent, WebSearchResultEvent
from app.services.web_search import search_web, format_search_results, WEB_SEARCH_PROMPT, WEB_SEARCH_FALLBACK_PROMPT
from app.services import cache
from app.services import state_manager as sm
from app.services import cart_service
import re

logger = logging.getLogger("agent")


# ── State ──

class AgentState(TypedDict, total=False):
    query: str
    session_id: str
    intent: str
    confidence: float
    slots: dict
    negation_slots: dict  # 否定条件：exclude_brands, exclude_categories, exclude_attributes
    rewritten_query: str
    retrieved_chunks: list
    latency_ms: float
    response: str
    product_cards: list
    cart_action: str  # cart operation: add/remove/view/clear/checkout
    cart_product_id: str
    cart_quantity: int
    error: str
    history: list  # 多轮对话历史 [{role, content}, ...]


# ── Nodes ──

async def node_classify_intent(state: AgentState) -> AgentState:
    """意图分类 + 槽位填充 + 短词扩展"""
    query = state["query"]

    if not query or not query.strip():
        state["intent"] = "chitchat"
        state["confidence"] = 1.0
        state["slots"] = {}
        return state

    # 短词扩展：≤4 字符的查询添加扩展上下文，提升检索效果
    expanded = query
    if len(query.strip()) <= 4:
        expanded = await _expand_short_query(query)
        logger.info("Short query expanded: '%s' → '%s'", query, expanded)

    # 标记是否已扩展，供下游 clarify 判断跳过追问
    state["_query_was_expanded"] = (expanded != query)

    history = state.get("history", []) or []
    ctx = ""
    if history:
        recent = history[-4:]  # 最近4条
        lines = [f"{'用户' if m.get('role')=='user' else '助手'}：{(m.get('content') or '')[:120]}" for m in recent]
        ctx = "\n对话上文：\n" + "\n".join(lines)
    intent_result = await classify_intent(expanded if expanded != query else query, context=ctx)
    state["intent"] = intent_result["intent"]
    state["confidence"] = intent_result["confidence"]

    # 非闲聊意图才提取槽位（用原始查询，关键词堆会污染 LLM 槽位提取）
    if state["intent"] != "chitchat":
        slots = await extract_slots(query, state["intent"])

        # 合并历史上下文：保留上一轮的排除条件，本轮的偏好覆盖历史
        prev_slots = _previous_slots_from_state(state)
        merged = {}
        # 排除类字段：累积（不丢失上一轮的排除条件）
        for key in ("exclude_brands", "exclude_categories", "exclude_attributes"):
            prev_val = prev_slots.get(key)
            new_val = slots.get(key)
            if isinstance(prev_val, list) and isinstance(new_val, list):
                merged[key] = list(set(prev_val + new_val))
            elif isinstance(prev_val, dict) and isinstance(new_val, dict):
                merged[key] = {**prev_val, **new_val}
            else:
                merged[key] = new_val or prev_val
        # 偏好类字段：本轮覆盖历史（跳过 None，不覆盖历史有效值）
        for key in slots:
            if key not in merged and slots[key] is not None:
                merged[key] = slots[key]
        # 保留本轮未涉及的历史偏好（跳过 None）
        for key in prev_slots:
            if key not in merged and prev_slots[key] is not None:
                merged[key] = prev_slots[key]

        state["slots"] = merged

        # 否定语义：任何含否定关键词的查询都提取排除条件（不只 anti_selection）
        neg_keywords = ["不要", "除了", "非", "不含", "排除", "拒绝", "去掉", "避开", "别"]
        has_negation = any(kw in query for kw in neg_keywords)
        if state["intent"] == "anti_selection" or has_negation:
            negation = await extract_negation_slots(query)
            # 合并而非覆盖 — 保留 merge loop 已累积的历史排除条件
            neg_brands = negation.get("exclude_brands") or []
            existing_brands = state["slots"].get("exclude_brands") or []
            state["slots"]["exclude_brands"] = list(set(existing_brands + neg_brands))

            neg_cats = negation.get("exclude_categories") or []
            existing_cats = state["slots"].get("exclude_categories") or []
            state["slots"]["exclude_categories"] = list(set(existing_cats + neg_cats))

            neg_attrs = negation.get("exclude_attributes") or {}
            existing_attrs = state["slots"].get("exclude_attributes") or {}
            state["slots"]["exclude_attributes"] = {**existing_attrs, **neg_attrs}

            # 补充关键词提取的文本级排除词（LLM 可能不返回此字段）
            from app.services.intent import _keyword_extract_negation
            kw_neg = _keyword_extract_negation(query)
            state["slots"]["exclude_text_terms"] = kw_neg.get("exclude_text_terms", [])
            # LLM 未覆盖的排除项，用关键词结果补充
            if not state["slots"]["exclude_brands"] and kw_neg.get("exclude_brands"):
                state["slots"]["exclude_brands"] = list(set(state["slots"]["exclude_brands"] + kw_neg["exclude_brands"]))
            if not state["slots"]["exclude_attributes"] and kw_neg.get("exclude_attributes"):
                state["slots"]["exclude_attributes"] = {**state["slots"]["exclude_attributes"], **kw_neg["exclude_attributes"]}

            logger.info("Negation extracted: brands=%s, attrs=%s, text_terms=%s",
                        state["slots"]["exclude_brands"],
                        state["slots"]["exclude_attributes"],
                        state["slots"].get("exclude_text_terms"))

        if _should_inherit_category_for_negation(query, state["slots"], prev_slots, has_negation, negation if has_negation else {}):
            state["slots"]["category"] = prev_slots["category"]

        # 品类隔离：排除品牌只影响当前品类，不越界到其他品类
        _normalize_exclusions(state["slots"])

        # 改写查询：排除类多轮请求使用继承后的品类召回，避免被排除品牌词牵引向量检索。
        base = _build_rewrite_base(query, expanded, state["slots"], has_negation, negation if has_negation else {})
        rewritten = await rewrite_query(base, state["slots"])
        state["rewritten_query"] = rewritten
    else:
        state["slots"] = {}

    return state


async def _expand_short_query(query: str) -> str:
    """将短关键词扩展为包含主要子品类的检索查询，提升向量召回率。

    "鞋" → "运动鞋 休闲鞋 皮鞋 跑步鞋 鞋类推荐"
    "平板" → "平板电脑 iPad 安卓平板 华为平板 推荐"
    """
    prompt = f"""用户输入了一个非常短的商品搜索词：「{query}」

这个搜索词太短，缺乏语义上下文，会导致商品检索效果很差。请你分析这个词可能指代的商品品类，列出该品类下最常见的 5-8 个子品类或相关热门搜索词，用空格分隔。

例如：
- "鞋" → "运动鞋 休闲鞋 皮鞋 跑步鞋 篮球鞋 帆布鞋 鞋类推荐"
- "平板" → "平板电脑 iPad 安卓平板 华为平板 小米平板 学习平板"
- "耳机" → "蓝牙耳机 降噪耳机 无线耳机 头戴式耳机 入耳式耳机 运动耳机"

只输出扩展后的搜索词（一行纯文本），不要输出其他内容。"""

    try:
        from app.services.llm_client import fast_chat_completion
        result = await fast_chat_completion(
            messages=[{"role": "user", "content": prompt}],
            temperature=0.3,
            max_tokens=80,
        )
        expanded = result.strip().strip('"').strip("'")
        if expanded and len(expanded) > len(query) + 2:
            logger.info("Short query expanded: '%s' → '%s'", query, expanded)
            return expanded
    except Exception as e:
        logger.warning("Short query expansion failed for '%s': %s", query, e)

    return query


async def node_clarify(state: AgentState) -> AgentState:
    """反问节点：缺失关键信息时，生成追问问题"""
    slots = state.get("slots", {})
    missing = slots.get("missing_slots", [])
    category = slots.get("category", "")

    if not missing and not category:
        # 极短模糊词：无品类无缺失槽位，生成通用追问
        query = state.get("query", "")
        prompt = f"""你是一个电商导购助手。用户说：「{query}」，没有指定具体想买什么。

请生成一个简短、友好的追问问题（1-2句话，不超过60字），引导用户说出想购买的商品品类或需求。
只输出问题本身，不要加任何解释、不要打招呼。"""
        try:
            from app.services.llm_client import fast_chat_completion
            raw = await fast_chat_completion(
                messages=[{"role": "user", "content": prompt}],
                temperature=0.7,
                max_tokens=100,
            )
            state["response"] = raw.strip()
            if state["response"]:
                return state
        except Exception as e:
            logger.warning("Clarify LLM failed for ultra-vague: %s", e)
        state["response"] = "能再具体说说您的需求吗？比如想买什么品类、预算多少呢？"
        return state

    query = state["query"]
    intent = state.get("intent", "commodity_recommend")

    if missing:
        missing_str = "、".join(missing)
        prompt = f"""你是一个电商导购助手。用户刚才说：「{query}」，但缺少以下关键信息：{missing_str}。

请生成一个简短、友好的追问问题（1-2句话，不超过60字），引导用户补充缺失的信息。
只输出问题本身，不要加任何解释、不要打招呼，不要使用"当然可以""好的"等开头。"""
    else:
        # missing 为空但有 category：仅含品类短词，追问细化需求
        prompt = f"""你是一个电商导购助手。用户想买{category}类商品，但没说具体需求。

请生成一个简短、友好的追问问题（1-2句话，不超过60字），引导用户细化需求（如预算、用途、偏好等）。
只输出问题本身，不要加任何解释、不要打招呼，不要使用"当然可以""好的"等开头。"""

    try:
        from app.services.llm_client import fast_chat_completion
        raw = await fast_chat_completion(
            messages=[{"role": "user", "content": prompt}],
            temperature=0.7,
            max_tokens=100,
        )
        state["response"] = raw.strip()
    except Exception as e:
        logger.warning("Clarify LLM failed, using template: %s", e)
        if missing:
            clarify_map = {
                "品类": "请问您想购买什么品类的商品呢？",
                "价格": "请问您的预算大概是多少呢？",
                "品牌": "请问您有偏好的品牌吗？",
                "场景": "请问是买来做什么用的呢？",
            }
            question = None
            for key, tmpl in clarify_map.items():
                if any(key in m for m in missing):
                    question = tmpl
                    break
            state["response"] = question or f"能再具体说说您的需求吗？比如{'、'.join(missing)}。"
        else:
            state["response"] = f"您想要什么类型的{category}呢？比如预算、用途方面有什么偏好吗？"

    logger.info("Clarify: missing=%s category=%s → response=%s", missing, category, state["response"])
    return state


# ── 常见场景 → 子品类映射（用于 LLM 失败时规则回退）──
_SCENARIO_FALLBACK_MAP = {
    "度假": ["防晒霜", "沙滩鞋", "墨镜", "泳衣", "遮阳帽"],
    "旅行": ["双肩包", "行李箱", "旅行枕", "防晒霜"],
    "三亚": ["防晒霜", "沙滩裙", "墨镜", "泳衣", "遮阳帽"],
    "通勤": ["双肩包", "衬衫", "皮鞋", "外套"],
    "露营": ["帐篷", "睡袋", "户外鞋", "头灯", "野餐垫"],
    "户外": ["冲锋衣", "登山鞋", "双肩包", "水壶"],
    "穿搭": ["T恤", "外套", "裤装", "运动鞋"],
    "送礼": ["蓝牙耳机", "手表", "香薰", "巧克力"],
    "办公": ["键盘", "台灯", "办公椅", "笔记本支架"],
    "健身": ["瑜伽垫", "跑鞋", "运动服", "水壶"],
    "出差": ["行李箱", "衬衫", "充电宝", "降噪耳机"],
}


async def _decompose_scenario(query: str, scenario: str) -> list[str]:
    """将场景化需求分解为 2-3 个商品子类别。"""
    prompt = f"""用户描述了这样一个场景：「{query}」。
请将这个场景拆解为 2-3 个具体的商品搜索关键词，每行一个。
只输出关键词，不要编号、不要解释。每个关键词应该是具体的商品品类。
例如：
输入"三亚度假穿搭"
输出：
防晒霜
沙滩凉鞋
墨镜"""
    try:
        from app.services.llm_client import fast_chat_completion
        raw = await fast_chat_completion(
            messages=[{"role": "user", "content": prompt}],
            temperature=0.3,
            max_tokens=80,
        )
        lines = [l.strip() for l in raw.strip().split("\n") if l.strip()]
        # Filter out numbered/bullet prefixes and markdown artifacts
        import re
        lines = [re.sub(r'^[\d]+[\.\)、\s]+', '', l).strip() for l in lines]
        lines = [re.sub(r'^[-•\*●]\s*', '', l).strip() for l in lines]
        lines = [l for l in lines if len(l) >= 1 and len(l) <= 30]
        logger.info("Scenario decomposed: %s → %s", query, lines[:3])
        if lines:
            return lines[:3]
    except Exception as e:
        logger.warning("Scenario decomposition LLM failed: %s", e)

    # Rule-based fallback: scan query for known scenarios
    q_lower = query.lower()
    merged = []
    for keyword, categories in _SCENARIO_FALLBACK_MAP.items():
        if keyword in q_lower:
            merged.extend(categories)
    if merged:
        # Deduplicate preserving order, take first 3
        seen = set()
        result = []
        for c in merged:
            if c not in seen:
                seen.add(c)
                result.append(c)
                if len(result) >= 3:
                    break
        logger.info("Scenario fallback decomposition: %s → %s", query, result)
        return result

    return [query]


async def node_retrieve(state: AgentState) -> AgentState:
    """RAG 检索"""
    if state["intent"] == "chitchat":
        state["retrieved_chunks"] = []
        return state

    query = state.get("rewritten_query") or state["query"]
    slots = state.get("slots", {})

    # 场景化购物：分解为多类目分别检索后合并
    if state.get("intent") == "scenario_shopping":
        scenario = slots.get("scenario", query)
        sub_queries = await _decompose_scenario(query, scenario)
        logger.info("Scenario shopping: sub_queries=%s", sub_queries)

        all_chunks = []
        seen_ids = set()
        total_latency = 0.0
        for sq in sub_queries:
            result = await rag_retrieve(
                query=sq,
                top_k=5,
                category=None,  # 让各子查询跨类目检索
                price_min=slots.get("price_min"),
                price_max=slots.get("price_max"),
            )
            total_latency += result["latency_ms"]
            for chunk in result["chunks"]:
                pid = chunk.get("payload", {}).get("product_id") or chunk.get("id")
                if pid not in seen_ids:
                    seen_ids.add(pid)
                    all_chunks.append(chunk)

        state["retrieved_chunks"] = all_chunks
        state["latency_ms"] = total_latency
    else:
        result = await rag_retrieve(
            query=query,
            top_k=10,
            category=slots.get("category"),
            price_min=slots.get("price_min"),
            price_max=slots.get("price_max"),
            exclude_brands=_scoped_exclude_brands(slots),
            exclude_categories=slots.get("exclude_categories"),
            exclude_attributes=slots.get("exclude_attributes"),
            strict_category=bool(slots.get("category") and (
                _scoped_exclude_brands(slots)
                or slots.get("exclude_categories")
                or slots.get("exclude_attributes")
                or state.get("history")
            )),
        )

        state["retrieved_chunks"] = result["chunks"]
        state["latency_ms"] = result["latency_ms"]

    if slots.get("category") and state["retrieved_chunks"]:
        state["retrieved_chunks"] = _filter_chunks_by_requested_category(
            state["retrieved_chunks"], slots.get("category")
        )

    # 文本级兜底过滤：排除 title/highlights 中含否定词的商品
    excluded_brand_terms = {str(b).strip().lower() for b in _scoped_exclude_brands(slots)}
    text_terms = [
        t for t in (slots.get("exclude_text_terms", []) or [])
        if str(t).strip().lower() not in excluded_brand_terms
    ]
    if text_terms and state["retrieved_chunks"]:
        filtered = []
        for chunk in state["retrieved_chunks"]:
            p = chunk.get("payload", {})
            haystack = (p.get("title", "") + " " + " ".join(p.get("highlights", []))).lower()
            if not any(t.lower() in haystack for t in text_terms):
                filtered.append(chunk)
        dropped = len(state["retrieved_chunks"]) - len(filtered)
        if dropped > 0:
            logger.info("Text filter: dropped %d chunks containing %s", dropped, text_terms)
        state["retrieved_chunks"] = filtered

    # ── Precision@K 监控 ──
    chunks_before_rerank = len(state["retrieved_chunks"])
    if state["retrieved_chunks"]:
        scores = []
        categories_seen = set()
        for c in state["retrieved_chunks"][:10]:
            p = c.get("payload", {})
            s = c.get("score") or p.get("score", 0)
            scores.append(round(float(s), 3))
            cat = p.get("category", "")
            if cat:
                categories_seen.add(cat)
        logger.info("Retrieval quality: n=%d top_score=%.3f avg_score=%.3f categories=%d",
                    chunks_before_rerank,
                    max(scores) if scores else 0,
                    sum(scores) / len(scores) if scores else 0,
                    len(categories_seen))

    # ── Reranker 精排（lifespan 已预加载模型，不再阻塞首请求） ──
    if state["retrieved_chunks"] and state.get("intent") != "chitchat":
        try:
            query_text = state.get("rewritten_query") or state["query"]
            n_before = len(state["retrieved_chunks"])
            state["retrieved_chunks"] = await rerank_async(
                query_text, state["retrieved_chunks"], top_k=10
            )
            logger.info("Reranker applied: %d → %d chunks re-ranked", n_before, len(state["retrieved_chunks"]))
        except Exception as e:
            logger.warning("Reranker unavailable, using raw retrieval: %s", e)

    logger.info("Agent retrieved %d chunks in %.0fms", len(state["retrieved_chunks"]), state["latency_ms"])
    return state


# ── 检索结果校验 ────────────────────────────────────────

MIN_MATCH_SCORE = 0.25  # 最低匹配度阈值，低于此值的商品不推荐

def _validate_ranked_products(ranked: list) -> tuple[list, bool]:
    """校验排序后的商品列表，过滤无效数据。

    Returns:
        (valid_products, is_reliable): 有效商品列表 + 是否可靠（最佳匹配>阈值）
    """
    valid = []
    for r in ranked:
        # 必要字段检查
        if not r.get("title") or not r.get("product_id"):
            logger.warning("Skipping product with missing title or product_id: %s", r)
            continue
        # 价格合理性 (安全转换，兼容字符串型价格)
        try:
            price = float(r.get("price", 0))
        except (TypeError, ValueError):
            price = 0.0
        if price <= 0:
            logger.warning("Skipping product with invalid price: %s (¥%s)", r.get("title"), price)
            continue
        valid.append(r)

    if not valid:
        return [], False

    best_score = valid[0].get("match_score", 0)
    is_reliable = best_score >= MIN_MATCH_SCORE
    if not is_reliable:
        logger.warning("Best match score %.2f below threshold %.2f", best_score, MIN_MATCH_SCORE)

    return valid, is_reliable


# ═══════════════════════════════════════════════════════
# 共享辅助函数 — 消除 node_generate / generate_response 重复
# ═══════════════════════════════════════════════════════

def _extract_raw_products(chunks: list, limit: int = 10) -> list[dict]:
    """从 Qdrant chunks 提取原始商品属性列表"""
    raw = []
    for chunk in chunks[:limit]:
        p = chunk["payload"]
        image_urls = p.get("image_urls") or []
        if not image_urls and p.get("image_url"):
            image_urls = [p["image_url"]]
        raw.append({
            "product_id": p.get("product_id", ""),
            "title": p.get("title"),
            "price": p.get("price"),
            "rating": p.get("rating"),
            "brand": p.get("brand"),
            "category": p.get("category"),
            "attributes": p.get("attributes", {}),
            "semantic_score": round(chunk.get("final_score", chunk.get("score", 0.5)), 4),
            "highlights": p.get("highlights", []),
            "image_url": image_urls[0] if image_urls else None,
            "image_urls": image_urls,
        })
    return raw


_SLOT_KEYS = {
    "category", "price_min", "price_max", "brand_preference", "attributes",
    "scenario", "exclude_brands", "exclude_categories", "exclude_attributes",
    "exclude_text_terms", "exclude_by_category", "missing_slots",
}


def _build_rewrite_base(query: str, expanded: str, slots: dict, has_negation: bool, negation: dict) -> str:
    if has_negation and slots.get("category"):
        return f"{slots['category']} 热门推荐 同类商品"
    positive_q = (negation or {}).get("positive_query", "") if has_negation else ""
    return positive_q or expanded or query


def _should_inherit_category_for_negation(
    query: str,
    slots: dict,
    prev_slots: dict,
    has_negation: bool,
    negation: dict,
) -> bool:
    """Keep the previous product category for chip-style requests like '排除 Apple'."""
    if not has_negation or not prev_slots.get("category"):
        return False

    current_category = (slots.get("category") or "").strip()
    previous_category = (prev_slots.get("category") or "").strip()
    if not current_category:
        return True
    if current_category == previous_category:
        return False

    positive_query = ((negation or {}).get("positive_query") or "").strip()
    compact_query = query.strip()
    chip_like = re.fullmatch(
        r"(?:不要|除了|非|不含|排除|拒绝|去掉|避开|别要|别买)\s*[\w\u4e00-\u9fff ._+-]{1,40}",
        compact_query,
        flags=re.IGNORECASE,
    )
    return bool(chip_like and (not positive_query or positive_query == compact_query))


def _previous_slots_from_state(state: dict) -> dict:
    """Read slots from current nested state and legacy top-level session fields."""
    nested = state.get("slots")
    if isinstance(nested, dict):
        prev = dict(nested)
    else:
        prev = {}
    for key in _SLOT_KEYS:
        if key not in prev and state.get(key) is not None:
            prev[key] = state[key]
    return prev


def _product_key_from_chunk(chunk: dict) -> str:
    payload = chunk.get("payload", {}) or {}
    return str(payload.get("product_id") or chunk.get("id") or payload.get("title") or "")


def _merge_product_chunks(primary: list[dict], supplements: list[dict]) -> list[dict]:
    merged = []
    seen = set()
    for chunk in (primary or []) + (supplements or []):
        key = _product_key_from_chunk(chunk)
        if not key or key in seen:
            continue
        seen.add(key)
        merged.append(chunk)
    return merged


def _category_matches_request(product_category: str, requested_category: str) -> bool:
    if not requested_category:
        return True
    product_category = (product_category or "").strip()
    requested_category = requested_category.strip()
    if product_category == requested_category:
        return True
    aliases = {
        "平板": {"平板", "平板电脑"},
        "平板电脑": {"平板", "平板电脑"},
    }
    return product_category in aliases.get(requested_category, {requested_category})


def _needs_strict_category_guard(requested_category: str) -> bool:
    return (requested_category or "").strip() in {"平板", "平板电脑"}


def _filter_chunks_by_requested_category(chunks: list[dict], requested_category: str) -> list[dict]:
    if not _needs_strict_category_guard(requested_category):
        return chunks or []
    filtered = [
        chunk for chunk in (chunks or [])
        if _category_matches_request((chunk.get("payload", {}) or {}).get("category", ""), requested_category)
    ]
    dropped = len(chunks or []) - len(filtered)
    if dropped:
        logger.info("Category guard: dropped %d non-%s candidates", dropped, requested_category)
    return filtered


def _filter_products_by_requested_category(products: list[dict], requested_category: str) -> list[dict]:
    if not _needs_strict_category_guard(requested_category):
        return products or []
    return [
        product for product in (products or [])
        if _category_matches_request(product.get("category", ""), requested_category)
    ]


async def _retrieve_same_category_supplements(query: str, slots: dict, existing_chunks: list[dict]) -> list[dict]:
    """Expand the candidate pool inside the current category after exclusions."""
    category = slots.get("category")
    if not category:
        return existing_chunks or []

    result = await rag_retrieve(
        query=f"{category} 热门 推荐 {query}",
        top_k=30,
        category=category,
        price_min=slots.get("price_min"),
        price_max=slots.get("price_max"),
        exclude_brands=_scoped_exclude_brands(slots),
        exclude_categories=slots.get("exclude_categories"),
        exclude_attributes=slots.get("exclude_attributes"),
        strict_category=True,
    )
    merged = _merge_product_chunks(existing_chunks or [], result.get("chunks", []))
    return _filter_chunks_by_requested_category(merged, category)


def _scoped_exclude_brands(slots: dict) -> list:
    """Return exclude_brands scoped to the current category, so brand exclusion
    doesn't leak across different categories."""
    category = slots.get("category", "")
    if category:
        by_cat = slots.get("exclude_by_category", {})
        if by_cat and category in by_cat:
            return by_cat[category]
    return slots.get("exclude_brands") or []


def _normalize_exclusions(slots: dict) -> None:
    """Move flat exclude_brands into category-scoped exclude_by_category dict."""
    category = slots.get("category", "")
    flat = slots.pop("exclude_brands", None)
    if not flat or not category:
        return
    by_cat = dict(slots.get("exclude_by_category") or {})
    existing = set(by_cat.get(category, []))
    existing.update(b for b in flat if b)
    by_cat[category] = list(existing)
    slots["exclude_by_category"] = by_cat


def _build_exclusion_hint(slots: dict) -> str:
    """Build a prompt hint about excluded brands/categories so the LLM can acknowledge them."""
    parts = []
    excluded_brands = _scoped_exclude_brands(slots)
    if excluded_brands:
        parts.append(f"用户已排除品牌：{'、'.join(excluded_brands)}")
    excluded_cats = slots.get("exclude_categories") or []
    if excluded_cats:
        parts.append(f"用户已排除品类：{'、'.join(excluded_cats)}")
    if parts:
        return "用户约束（必须在回复中体现）：" + "；".join(parts) + "\n   → 不要在「结语」中追问已排除的品牌或品类，如需调整建议提醒用户当前已排除的品牌。\n"
    return ""


def _build_user_prefs(slots: dict) -> dict:
    """从 slots 构造用户偏好字典"""
    return {
        "price_min": slots.get("price_min"),
        "price_max": slots.get("price_max"),
        "brand_preference": slots.get("brand_preference"),
        "attributes": slots.get("attributes", {}),
        "exclude_brands": _scoped_exclude_brands(slots),
        "exclude_attributes": slots.get("exclude_attributes", {}),
    }


def _assemble_cards(valid_ranked: list) -> list[dict]:
    """将排序校验后的商品列表组装为最终卡片格式"""
    cards = []
    for r in valid_ranked:
        cards.append({
            "product_id": r.get("product_id") or r.get("id") or "",
            "title": r["title"],
            "price": r.get("price"),
            "category": r.get("category", ""),
            "brand": r.get("brand"),
            "rating": r.get("rating"),
            "image_url": r.get("image_url") or (r.get("image_urls", [None]) or [None])[0],
            "image_urls": r.get("image_urls", []) if r.get("image_urls") else ([r.get("image_url")] if r.get("image_url") else []),
            "highlights": r.get("highlights", []),
            "match_score": r.get("match_score", 0.5),
            "rank_reason": r.get("rank_reason", ""),
        })
    return cards


def _build_generation_prompt(message: str, slots: dict, valid_ranked: list, is_reliable: bool, intent: str, history: list[dict] | None = None) -> str:
    """构建 LLM 生成推荐回复的 prompt（统一版本，支持多轮上下文）"""
    reliability_hint = ""
    if not is_reliable:
        reliability_hint = "\n⚠️ 注意：以下商品与用户需求的匹配度较低，请在回复中诚实告知用户，建议其调整搜索条件。\n"

    scenario_hint = ""
    if intent == "scenario_shopping":
        scenario_hint = "⚠️ 场景化推荐模式：当前用户描述了一个完整场景，检索结果可能涵盖多个品类。请按品类分组推荐，说明每件商品在场景中的作用，并给出搭配建议。\n"

    # 多轮对话历史
    history_text = ""
    if history:
        history_lines = []
        for h in history:
            role_label = "用户" if h["role"] == "user" else "助手"
            history_lines.append(f"{role_label}：{h['content']}")
        history_text = "\n对话历史：\n" + "\n".join(history_lines) + "\n\n请结合以上对话历史理解当前用户需求。比如用户说「便宜一点的」是在前面推荐的基础上筛选，用户说「要红色的」是在补充颜色偏好。如果当前问题含义模糊，根据历史推断具体意图。\n"

    context_parts = []
    for i, r in enumerate(valid_ranked):
        context_parts.append(
            f"[{i+1}] {r.get('title')} | ¥{r.get('price')} | ★{r.get('rating')} | "
            f"{' '.join(r.get('highlights', [])[:2])} | {r.get('rank_reason', '')}"
        )
    context = "\n".join(context_parts)
    n = len(valid_ranked)

    return f"""你是一个电商导购助手。基于检索到的商品信息，为用户生成推荐回答。
{reliability_hint}
{scenario_hint}{history_text}⚠️ 严禁事项（违反将导致推荐无效）:
1. 不得编造任何商品名称、型号、价格 — 只能引用上方[1][2]...标记的商品
2. 不得编造优惠券、满减、折扣、赠品、限时活动
3. 不得编造不存在的功能参数、认证标识
4. 不得编造用户评价、销量排名、市场占有率、对比结论

校验规则:
- 你提到的每个价格数字，必须在检索数据中出现过
- 你提到的每个品牌名、产品名，必须在检索数据中出现过
- 如果所有商品与用户需求的匹配度均低于60%，应诚实告知"当前没有很匹配的商品，建议调整条件"
- 违反以上任一条，不要输出该推荐

用户需求：{message}
用户预算：{slots.get('price_min', '不限')}-{slots.get('price_max', '不限')}
用户品类偏好：{slots.get('category', '未指定')}
{_build_exclusion_hint(slots)}
检索到的商品（共{n}款，必须全部推荐，不可遗漏）：
{context}

输出格式要求（严格遵守，违反将导致推荐无效）：

第一步：开头用一句话总结，列出全部商品名称（用顿号分隔），以句号结尾。
第二步：紧接着依次用「商品1」「商品2」「商品3」分别展开每款商品，每款商品用「商品N」独占一行作为标题。
第三步：以「结语」收尾，简短追问用户偏好（1句话，不超过20字）。

每款商品的展开格式：
「商品N」
商品全名 | 综合匹配度 XX%
① 匹配依据：...
② 品质亮点：...
③ 适用场景：...

格式示例：
为您找到3款降噪耳机：索尼WH-1000XM5、Bose QC45、AirPods Pro，分别适合不同场景。

「商品1」
索尼 WH-1000XM5 | 综合匹配度 92%
① 匹配依据：顶级降噪，适合通勤与办公
② 品质亮点：30小时续航、LDAC高清音频、多点连接
③ 适用场景：地铁通勤、办公室专注工作

「商品2」
Bose QC45 | 综合匹配度 88%
① 匹配依据：性价比突出的降噪标杆，通勤续航双优
② 品质亮点：24小时续航、TriPort声学结构、蓝牙5.1
③ 适用场景：日常通勤与长时间佩戴

「商品3」
AirPods Pro | 综合匹配度 85%
① 匹配依据：苹果生态无缝切换，自适应降噪
② 品质亮点：H2芯片、个性化空间音频、IPX4防水
③ 适用场景：苹果用户日常全场景使用

「结语」需要进一步筛选品牌或预算吗？

要求：
- 必须逐一推荐以上全部 {n} 款商品，不可跳过任何一款
- 每款商品必须独占一行使用「商品N」标记
- 总字数控制在300字以内
- 禁止使用"非常好""很不错"等模糊词，必须引用具体数字"""


async def node_generate(state: AgentState) -> AgentState:
    """生成回答 + 商品卡片"""
    if state.get("intent") == "cart_operation" and state.get("response"):
        state["product_cards"] = state.get("product_cards", [])
        return state

    if state["intent"] == "chitchat":
        state["response"] = "你可以告诉我具体的需求，比如「推荐一款降噪耳机」「300元以内的运动鞋」「送女朋友的生日礼物」，我会帮你找到合适的商品～"
        state["product_cards"] = []
        return state

    chunks = state.get("retrieved_chunks", [])
    if not chunks:
        state["response"] = "抱歉，暂时没有找到符合您要求的商品。可以试试调整条件重新搜索吗？"
        state["product_cards"] = []
        return state

    from app.services.product_ranker import rank_products

    raw_products = _extract_raw_products(chunks)
    raw_products = _filter_products_by_requested_category(raw_products, state.get("slots", {}).get("category", ""))
    user_prefs = _build_user_prefs(state.get("slots", {}))
    ranked = rank_products(raw_products, user_prefs, state["intent"], top_k=3)
    valid_ranked, is_reliable = _validate_ranked_products(ranked)

    if not valid_ranked:
        state["response"] = "抱歉，暂时没有找到符合您要求的商品。可以试试调整条件重新搜索吗？"
        state["product_cards"] = []
        return state

    cards = _assemble_cards(valid_ranked)
    state["product_cards"] = cards
    state["_is_reliable"] = is_reliable

    prompt = _build_generation_prompt(state["query"], state.get("slots", {}), valid_ranked, is_reliable, state["intent"], history=state.get("history", []))

    try:
        stream = await chat_completion(
            messages=[{"role": "user", "content": prompt}],
            temperature=0.7,
            max_tokens=400,
            stream=True,
        )
        response_text = ""
        async for chunk in stream:
            if chunk.choices and chunk.choices[0].delta.content:
                response_text += chunk.choices[0].delta.content
        state["response"] = response_text
    except Exception as e:
        logger.error("LLM generate failed: %s", e)
        state["response"] = f"为您找到 {len(cards)} 款相关商品。如需进一步筛选，请告诉我您的偏好。"
    return state


# ── Cart Helpers ──

def _extract_cart_action(query: str) -> str:
    """从用户查询中提取购物车操作类型"""
    q = query.lower()
    if any(kw in q for kw in ["数量", "改成", "改为", "设为", "设置为"]):
        return "quantity"
    if any(kw in q for kw in ["清空", "全部删除", "全部移除"]):
        return "clear"
    if any(kw in q for kw in ["下单", "结算", "结账", "支付", "买单", "确认下单"]):
        return "checkout"
    if any(kw in q for kw in ["删除", "移除", "去掉", "不要第"]):
        return "remove"
    if any(kw in q for kw in ["查看", "看看", "显示", "我的购物车", "购物车里有"]):
        return "view"
    if any(kw in q for kw in ["加入", "加购", "加到", "添加", "放入", "加一个", "加第"]):
        return "add"
    return "view"  # 默认查看


def _parse_quantity(query: str) -> int | None:
    """解析自然语言数量，如“数量改成2”“改为两个”“×3”."""
    cn_nums = {
        "一": 1, "二": 2, "两": 2, "三": 3, "四": 4, "五": 5,
        "六": 6, "七": 7, "八": 8, "九": 9, "十": 10,
    }
    m = re.search(r"(?:数量|改成|改为|设为|设置为|加到|减到|x|×)\s*([0-9]+|[一二两三四五六七八九十])", query, re.I)
    if not m:
        return None
    raw = m.group(1)
    if raw.isdigit():
        return max(0, int(raw))
    return cn_nums.get(raw)


async def _find_product_for_cart(query: str, state: "AgentState") -> dict | None:
    """从查询中识别用户要加购的商品。
    优先级：1. 序号匹配 product_cards  2. 商品名匹配 product_cards  3. Qdrant 搜索
    """
    # 获取可用的 product_cards
    product_cards = state.get("product_cards", []) or []
    slots = state.get("slots", {})
    prev_cards = slots.get("product_cards", [])
    if not product_cards and prev_cards:
        product_cards = prev_cards

    # 1. 序号匹配: "第一个"、"第二个"、"第1个"
    ordinal_map = {
        "一": 1, "二": 2, "三": 3, "四": 4, "五": 5,
        "1": 1, "2": 2, "3": 3, "4": 4, "5": 5,
    }
    for word in sorted(ordinal_map.keys(), key=len, reverse=True):
        if word in query:
            idx = ordinal_map[word]
            if product_cards and idx <= len(product_cards):
                card = product_cards[idx - 1]
                return {
                    "id": card.get("id", ""),
                    "title": card.get("title", ""),
                    "price": card.get("price", 0),
                }

    # 2. 按商品名匹配 product_cards（标题前几个字出现在 query 中）
    if product_cards:
        for card in product_cards:
            title = card.get("title", "")
            if title and len(title) >= 3 and title[:4] in query:
                return {
                    "id": card.get("id", ""),
                    "title": title,
                    "price": card.get("price", 0),
                }

    # 3. Qdrant 搜索：从 "加入购物车" 前的文本提取搜索词
    for marker in ["加入购物车", "加到购物车", "加购", "添加到购物车"]:
        if marker in query:
            prefix = query.split(marker)[0].strip()
            # 去掉 "把" "将" 等引导词
            for lead in ["把", "将", "这个", "这款", "那个"]:
                if prefix.startswith(lead):
                    prefix = prefix[len(lead):].strip()
            if prefix and len(prefix) >= 2:
                try:
                    from app.services.rag import retrieve as rag_retrieve
                    result = await rag_retrieve(query=prefix, top_k=1)
                    chunks = result.get("chunks", [])
                    if chunks:
                        p = chunks[0].get("payload", {})
                        pid = str(p.get("product_id", "")).strip()
                        # 验证 product_id 是合法 UUID
                        try:
                            import uuid as _uuid
                            _uuid.UUID(pid)
                            return {
                                "id": pid,
                                "title": p.get("title", ""),
                                "price": p.get("price", 0),
                            }
                        except (ValueError, AttributeError):
                            logger.warning("Cart Qdrant fallback: invalid product_id '%s'", pid[:20])
                            pass
                except Exception as e:
                    logger.warning("Cart Qdrant fallback lookup failed: %s", e)
            break

    return None


async def _remove_from_cart(query: str, session_id: str, db) -> str:
    """从购物车中删除商品，按序号或名字匹配"""
    items = await cart_service.get_cart(db, session_id)
    if not items:
        return "购物车是空的，没有可删除的商品。"

    # 1. 序号匹配
    ordinal_map = {
        "一": 1, "二": 2, "三": 3, "四": 4, "五": 5,
        "1": 1, "2": 2, "3": 3, "4": 4, "5": 5,
    }
    for word in sorted(ordinal_map.keys(), key=len, reverse=True):
        if word in query:
            idx = ordinal_map[word]
            if idx <= len(items):
                item = items[idx - 1]
                await cart_service.remove_from_cart(db, session_id, str(item.product_id))
                return f"✅ 已从购物车删除「{item.title}」。"
            else:
                return f"购物车只有 {len(items)} 件商品，没有第 {idx} 个。"

    # 2. 按商品名匹配
    for item in items:
        if item.title and len(item.title) >= 3 and item.title[:4] in query:
            await cart_service.remove_from_cart(db, session_id, str(item.product_id))
            return f"✅ 已从购物车删除「{item.title}」。"

    return f"没有找到要删除的商品。当前购物车有 {len(items)} 件商品，请指定序号或商品名。"


async def _update_cart_quantity(query: str, session_id: str, db) -> str:
    """按序号或商品名修改购物车数量。"""
    quantity = _parse_quantity(query)
    if quantity is None:
        return "请告诉我想改成几件，例如「把第一个数量改成 2」。"

    items = await cart_service.get_cart(db, session_id)
    if not items:
        return "购物车是空的，暂时没有可修改数量的商品。"

    ordinal_map = {
        "一": 1, "二": 2, "三": 3, "四": 4, "五": 5,
        "1": 1, "2": 2, "3": 3, "4": 4, "5": 5,
    }
    target = None
    for word in sorted(ordinal_map.keys(), key=len, reverse=True):
        if word in query:
            idx = ordinal_map[word]
            if idx <= len(items):
                target = items[idx - 1]
                break
            return f"购物车只有 {len(items)} 件商品，没有第 {idx} 个。"

    if target is None:
        for item in items:
            if item.title and len(item.title) >= 3 and item.title[:4] in query:
                target = item
                break

    if target is None:
        return "没有找到要修改数量的商品，请指定序号或商品名。"

    if quantity == 0:
        await cart_service.remove_from_cart(db, session_id, str(target.product_id))
        return f"已将「{target.title}」数量改为 0，并从购物车移除。"

    await cart_service.update_quantity(db, session_id, str(target.product_id), quantity)
    return f"已将「{target.title}」数量改为 {quantity} 件。"


# ── Cart Node ──

async def node_cart(state: "AgentState") -> "AgentState":
    """购物车操作节点：调用 cart_service.py 执行 CRUD"""
    query = state["query"]
    session_id = state.get("session_id", "")

    # 确定 cart_action
    cart_action = _extract_cart_action(query)
    state["cart_action"] = cart_action
    logger.info("Cart node: action=%s, session=%s", cart_action, session_id[:8] if session_id else "none")

    if not session_id:
        state["response"] = "会话未初始化，无法操作购物车。请刷新页面重试。"
        state["product_cards"] = []
        return state

    try:
        from app.core.database import AsyncSessionLocal
        async with AsyncSessionLocal() as db:
            if cart_action == "view":
                items = await cart_service.get_cart(db, session_id)
                total = await cart_service.get_cart_total(db, session_id)
                if items:
                    item_lines = [f"{i+1}. {it.title} ×{it.quantity}  ¥{it.price:.0f}"
                                  for i, it in enumerate(items)]
                    state["response"] = (
                        f"🛒 购物车（{len(items)}件，合计 ¥{total:.0f}）：\n"
                        + "\n".join(item_lines)
                        + "\n\n输入「删除第N个」可移除商品，「清空购物车」可清空，「下单」可结算。"
                    )
                else:
                    state["response"] = "购物车是空的。请先搜索商品，然后说「把第一个加入购物车」来添加。"
                state["product_cards"] = []

            elif cart_action == "add":
                product = await _find_product_for_cart(query, state)
                if product and product.get("id") and len(product.get("id","")) > 10:
                    await cart_service.add_to_cart(
                        db, session_id,
                        product["id"], product["title"], product["price"]
                    )
                    items = await cart_service.get_cart(db, session_id)
                    total = await cart_service.get_cart_total(db, session_id)
                    state["response"] = (
                        f"✅ 已将「{product['title']}」(¥{product['price']:.0f}) 加入购物车。\n"
                        f"当前购物车共 {len(items)} 件，合计 ¥{total:.0f}。"
                    )
                    await db.commit()
                else:
                    state["response"] = (
                        "抱歉，没有找到要添加的商品。请先搜索商品（如「推荐蓝牙耳机」），"
                        "然后说「把第一个加入购物车」来添加。"
                    )
                state["product_cards"] = []

            elif cart_action == "quantity":
                state["response"] = await _update_cart_quantity(query, session_id, db)
                await db.commit()
                state["product_cards"] = []

            elif cart_action == "remove":
                state["response"] = await _remove_from_cart(query, session_id, db)
                await db.commit()
                state["product_cards"] = []

            elif cart_action == "clear":
                await cart_service.clear_cart(db, session_id)
                await db.commit()
                state["response"] = "🗑️ 购物车已清空。"
                state["product_cards"] = []

            elif cart_action == "checkout":
                items = await cart_service.get_cart(db, session_id)
                total = await cart_service.get_cart_total(db, session_id)
                if not items:
                    state["response"] = "购物车是空的，无法下单。请先添加商品。"
                    state["product_cards"] = []
                    return state

                # 判断是"查看订单"还是"确认下单"
                is_confirm = any(kw in query for kw in ["确认下单", "确认", "是的", "确定", "没错"])
                if is_confirm:
                    from app.services import order_service as osvc
                    items_snapshot = [
                        {"product_id": it.product_id, "title": it.title,
                         "price": it.price, "quantity": it.quantity}
                        for it in items
                    ]
                    order = await osvc.create_order(db, session_id, items_snapshot, total)
                    item_list = "\n".join(
                        [f"  {it.title} ×{it.quantity}  ¥{it.price * it.quantity:.0f}"
                         for it in items]
                    )
                    state["response"] = (
                        f"✅ 下单成功！\n\n📦 订单号：{order.order_no}\n"
                        f"{item_list}\n"
                        f"━━━━━━━━━━━━━━\n"
                        f"💰 实付：¥{total:.0f}\n\n"
                        f"（演示环境，不会实际扣款。感谢您的购买！）"
                    )
                    await cart_service.clear_cart(db, session_id)
                    await db.commit()
                    logger.info("Order confirmed: %s, cart cleared for session %s", order.order_no, session_id[:8])
                else:
                    item_lines = [f"{i+1}. {it.title} ×{it.quantity} — ¥{it.price * it.quantity:.0f}"
                                  for i, it in enumerate(items)]
                    state["response"] = (
                        "📋 订单确认：\n" + "\n".join(item_lines)
                        + f"\n\n💰 合计：¥{total:.0f}\n\n"
                        + "输入「确认下单」完成购买（演示环境，不会实际扣款）。"
                    )
                state["product_cards"] = []

    except Exception as e:
        logger.error("Cart operation failed: %s", e)
        state["response"] = f"购物车操作失败：{str(e)}"
        state["product_cards"] = []

    return state


async def node_web_search(state: AgentState) -> AgentState:
    """联网搜索节点 — DuckDuckGo 搜索 + LLM 知识兜底"""
    query = state.get("rewritten_query") or state.get("query", "")
    try:
        results = await search_web(query)
        if results:
            formatted = format_search_results(results)
            prompt = WEB_SEARCH_PROMPT.format(query=query, search_results=formatted)
            from app.services.llm_client import chat_completion as cc
            response = await cc(messages=[{"role": "user", "content": prompt}], temperature=0.7, max_tokens=300, stream=False)
            state["response"] = response
            state["_web_results"] = results
            state["_is_fallback"] = False
        else:
            # DuckDuckGo 不可用，使用 LLM 训练知识兜底
            logger.info("Web search returned 0 results, using LLM knowledge fallback")
            prompt = WEB_SEARCH_FALLBACK_PROMPT.format(query=query)
            from app.services.llm_client import chat_completion as cc
            response = await cc(messages=[{"role": "user", "content": prompt}], temperature=0.7, max_tokens=300, stream=False)
            state["response"] = response
            state["_web_results"] = []
            state["_is_fallback"] = True
    except Exception as e:
        logger.warning("Web search node failed: %s", e)
        # 最底层兜底
        prompt = WEB_SEARCH_FALLBACK_PROMPT.format(query=query)
        try:
            from app.services.llm_client import chat_completion as cc
            response = await cc(messages=[{"role": "user", "content": prompt}], temperature=0.7, max_tokens=200, stream=False)
            state["response"] = response
        except Exception:
            state["response"] = "抱歉，联网搜索暂时不可用。你可以试试在本地商品库中搜索具体的商品需求。"
        state["_web_results"] = []
        state["_is_fallback"] = True
    state["product_cards"] = []
    return state


# ── Router ──

def route_after_intent(state: AgentState) -> str:
    """意图路由：闲聊 → generate, 联网搜索 → web_search, 缺失信息 → clarify, 购物车 → cart, 其他 → retrieve"""
    if state.get("intent") == "chitchat":
        return "generate"

    if state.get("intent") == "web_search":
        return "web_search"

    if state.get("intent") == "cart_operation":
        return "cart"

    slots = state.get("slots", {})
    missing = slots.get("missing_slots", [])
    has_category = bool(slots.get("category"))
    has_scenario = bool(slots.get("scenario"))
    has_budget = bool(slots.get("price_min") or slots.get("price_max"))
    has_attrs = bool(slots.get("attributes"))
    confidence = state.get("confidence", 0.5)
    intent_type = state.get("intent", "")
    original_query = state.get("query", "").strip()

    ultra_vague_words = {"推荐", "买东西", "推荐一下", "买什么", "买啥", "有什么", "推荐个"}
    is_ultra_vague = len(original_query) <= 3 or original_query in ultra_vague_words

    missing_everything = bool(missing) and not (has_category or has_scenario or has_budget or has_attrs)
    short_category_only = len(original_query) <= 4 and has_category \
        and not (has_scenario or has_budget or has_attrs)
    low_confidence = confidence < 0.5 and intent_type == "commodity_recommend"

    if (is_ultra_vague or missing_everything or short_category_only or low_confidence) \
            and intent_type not in ("chitchat", "anti_selection", "cart_operation"):
        return "clarify"

    return "retrieve"


# ── Graph Builder ──

def build_agent_graph() -> StateGraph:
    workflow = StateGraph(AgentState)

    workflow.add_node("classify_intent", node_classify_intent)
    workflow.add_node("clarify", node_clarify)
    workflow.add_node("retrieve", node_retrieve)
    workflow.add_node("cart", node_cart)
    workflow.add_node("web_search", node_web_search)
    workflow.add_node("generate", node_generate)

    workflow.set_entry_point("classify_intent")
    workflow.add_conditional_edges("classify_intent", route_after_intent, {
        "retrieve": "retrieve",
        "clarify": "clarify",
        "cart": "cart",
        "web_search": "web_search",
        "generate": "generate",
    })
    workflow.add_edge("clarify", "generate")
    workflow.add_edge("retrieve", "generate")
    workflow.add_edge("cart", "generate")
    workflow.add_edge("web_search", "generate")
    workflow.add_edge("generate", END)

    return workflow


agent_graph = build_agent_graph().compile()


async def run_agent(query: str, session_id: str = "") -> dict:
    """
    内部评测入口 — 运行完整 Agent 图并返回最终状态。
    供 evaluator.py 等内部模块调用，不走 HTTP/SSE。
    """
    initial_state: AgentState = {
        "query": query,
        "session_id": session_id,
        "slots": {},
    }
    final_state = await agent_graph.ainvoke(initial_state)
    return final_state


def _safe_emit_boundary(unemitted: str) -> int:
    """返回 unemitted 中可以安全输出的位置 — 保留最后一句完整句子用于三特征检测。"""
    if len(unemitted) < 40:
        return 0
    boundaries = []
    for sep in ("。", "！", "？"):
        pos = -1
        while True:
            pos = unemitted.find(sep, pos + 1)
            if pos < 0:
                break
            boundaries.append(pos + 1)  # 包含标点
    if len(boundaries) < 2:
        return 0
    boundaries.sort()
    return boundaries[-2]  # 保留最后一句，输出倒数第二句之前的所有内容


async def _stream_interleaved(stream, cards: list) -> AsyncGenerator:
    """真流式交错输出 — 逐 token 发送文字，边收边检测卡片插入点。

    检测策略：
    1. 「商品N」标记扫描 — 最精确，立即在标记处插入卡片
    2. 三特征检测 — 匹配依据/品质亮点/适用场景 全部出现在第N款商品时，
       等待适用场景句号到达后插入第N张卡片
    3. 安全输出：只输出倒数第二句号之前的文本，保留最后一句在缓冲区中
       以确保卡片边界检测能完整捕获"适用场景。"的句号。
    """
    buffer = ""           # 全部已接收文本
    emitted_pos = 0       # 已发送到客户端的缓冲位置
    emitted_indices = set()
    closing_seen = False

    marker_re = re.compile(r"「商品\s*(\d+)\s*」|\[PRODUCT_(\d+)\]|【PRODUCT_(\d+)】")
    tag_re = re.compile(r"「(?:商品\s*\d+\s*」|结语」)|\[(?:SUMMARY|PRODUCT_\d+|CLOSING)\]|【(?:SUMMARY|PRODUCT_\d+|CLOSING)】", re.IGNORECASE)
    closing_re = re.compile(r"「结语」|\[CLOSING\]|【CLOSING】", re.IGNORECASE)

    def _card(idx: int):
        if idx >= len(cards):
            return None
        c = cards[idx]
        return {
            "event": "product_cards",
            "data": ProductCardEvent(
                product_id=c.get("product_id", c.get("id", "")),
                title=c.get("title", ""), price=c.get("price", 0),
                rating=c.get("rating", 0),
                match_score=c.get("match_score", c.get("score", 0.5)),
                highlights=c.get("highlights", []),
                image_url=c.get("image_url"), image_urls=c.get("image_urls", []),
                brand=c.get("brand"), category=c.get("category", ""),
                index=idx + 1, total=len(cards),
            ).model_dump_json(),
        }

    def _emit_up_to(pos: int):
        """输出 emitted_pos 到 pos 之间的文本（清理标记后）。"""
        nonlocal emitted_pos
        if pos <= emitted_pos:
            return
        seg = buffer[emitted_pos:pos]
        clean = tag_re.sub("", seg)
        if clean.strip():
            yield_me = {"event": "text_delta", "data": TextDeltaEvent(content=clean).model_dump_json()}
            emitted_pos = pos
            return yield_me
        emitted_pos = pos
        return None

    async for chunk in stream:
        delta = chunk.choices[0].delta
        if not delta.content:
            continue

        buffer += delta.content

        # ── Closing 之后：直接透传 ──
        if closing_seen:
            evt = _emit_up_to(len(buffer))
            if evt:
                yield evt
            continue

        # ── Closing 检测 ──
        cm = closing_re.search(buffer, emitted_pos)
        if cm:
            evt = _emit_up_to(cm.start())
            if evt:
                yield evt
            next_idx = len(emitted_indices)
            if next_idx < len(cards):
                ce = _card(next_idx)
                if ce:
                    emitted_indices.add(next_idx)
                    yield ce
            emitted_pos = cm.end()
            closing_seen = True
            continue

        # ── 策略1: 「商品N」标记 ──
        # 标记表示下一款商品的开头，不是当前卡片的插入点。
        # 因此在「商品2」处发送商品1卡片，在「商品3」处发送商品2卡片。
        m = marker_re.search(buffer, emitted_pos)
        if m:
            evt = _emit_up_to(m.start())
            if evt:
                yield evt
            card_num = int(m.group(1) or m.group(2) or m.group(3))
            prev_idx = card_num - 2
            if 0 <= prev_idx < len(cards) and prev_idx not in emitted_indices:
                ce = _card(prev_idx)
                if ce:
                    emitted_indices.add(prev_idx)
                    yield ce
            elif card_num > 1:
                next_idx = len(emitted_indices)
                if next_idx < min(card_num - 1, len(cards)):
                    ce = _card(next_idx)
                    if ce:
                        emitted_indices.add(next_idx)
                        yield ce
            emitted_pos = m.end()
            continue

        # ── 策略2: 三特征检测（匹配依据 + 品质亮点 + 适用场景） ──
        count_a = buffer.count("匹配依据")
        count_b = buffer.count("品质亮点")
        count_c = buffer.count("适用场景")
        min_count = min(count_a, count_b, count_c)

        while min_count > len(emitted_indices):
            # 找到第 len(emitted_indices)+1 次"适用场景"后的句号位置
            target = len(emitted_indices) + 1
            nth = _nth_occurrence(buffer, "适用场景", target)
            end = buffer.find("。", nth)
            if end < 0:
                break  # 句号还没到，继续等

            card_pos = end + 1  # 卡片插在句号之后
            evt = _emit_up_to(card_pos)
            if evt:
                yield evt

            # 按顺序发下一张未发送的卡片
            next_idx = len(emitted_indices)
            if next_idx < len(cards):
                ce = _card(next_idx)
                if ce:
                    emitted_indices.add(next_idx)
                    yield ce
            else:
                break

        # ── 安全输出：只发倒数第二句号之前的文本，保留最后一句在缓冲中 ──
        unemitted = buffer[emitted_pos:]
        safe = _safe_emit_boundary(unemitted)
        if safe > 0:
            evt = _emit_up_to(emitted_pos + safe)
            if evt:
                yield evt

    # ── 流结束：输出剩余文本 + 兜底卡片 ──
    evt = _emit_up_to(len(buffer))
    if evt:
        yield evt

    for idx in range(len(cards)):
        if idx not in emitted_indices:
            ce = _card(idx)
            if ce:
                yield ce


def _find_card_boundaries(text: str, cards: list) -> list:
    """在完整响应文本中定位每张卡片的插入点，返回 [(position, card_idx), ...] 按位置排序。

    策略优先级：
    1. 「商品N」标记 — 最精确
    2. 三特征检测（匹配依据 + 品质亮点 + 适用场景）— LLM 几乎一定会输出
    3. 标题模糊匹配 — 兜底
    """
    marker_re = re.compile(r"「商品\s*(\d+)\s*」|\[PRODUCT_(\d+)\]|【PRODUCT_(\d+)】")

    # 策略1: 标记
    # 「商品N」是第 N 款商品详情的开头。卡片应出现在上一款详情文本之后：
    # 「商品2」前插商品1卡，「商品3」前插商品2卡，最后一款放在正文末尾。
    boundaries = []
    marker_matches = list(marker_re.finditer(text))
    for m in marker_matches:
        card_num = int(m.group(1) or m.group(2) or m.group(3))
        card_idx = card_num - 2
        if 0 <= card_idx < len(cards):
            boundaries.append((m.start(), card_idx))
    if marker_matches:
        last_card_num = int(marker_matches[-1].group(1) or marker_matches[-1].group(2) or marker_matches[-1].group(3))
        last_card_idx = last_card_num - 1
        if 0 <= last_card_idx < len(cards):
            boundaries.append((len(text), last_card_idx))
    if boundaries:
        boundaries.sort(key=lambda x: x[0])
        return boundaries

    # 策略2: 三特征检测 — 匹配依据、品质亮点、适用场景 同时出现第N次 → 第N张卡片
    # 卡片放在适用场景段落末尾（句号之后）
    for card_idx in range(len(cards)):
        target = card_idx + 1
        pos_a = _nth_occurrence(text, "匹配依据", target)
        pos_b = _nth_occurrence(text, "品质亮点", target)
        pos_c = _nth_occurrence(text, "适用场景", target)
        if pos_a < 0 or pos_b < 0 or pos_c < 0:
            break
        # 卡片插入点：适用场景后第一个句号之后
        end = text.find("。", pos_c)
        if end >= 0:
            boundaries.append((end + 1, card_idx))
        else:
            boundaries.append((pos_c + 4, card_idx))  # len("适用场景") = 4

    if boundaries:
        boundaries.sort(key=lambda x: x[0])
        return boundaries

    # 策略3: 标题模糊匹配
    for card_idx, card in enumerate(cards):
        title = card.get("title", "").strip()
        if not title or len(title) < 3:
            continue
        pos = text.find(title)
        if pos < 0:
            tokens = re.findall(r'[一-鿿]+|[a-zA-Z0-9]+', title)
            if len(tokens) >= 2:
                for start in range(1, min(3, len(tokens))):
                    sub = ''.join(tokens[start:])
                    if len(sub) >= 4:
                        pos = text.find(sub)
                        if pos >= 0:
                            break
        if pos < 0:
            prefix = title[:6].strip()
            if len(prefix) >= 4:
                pos = text.find(prefix)
        if pos >= 0:
            boundaries.append((pos, card_idx))

    boundaries.sort(key=lambda x: x[0])
    return boundaries


def _nth_occurrence(text: str, pattern: str, n: int) -> int:
    """返回 pattern 在 text 中第 n 次出现的位置（0-indexed），未找到返回 -1。"""
    pos = -1
    for _ in range(n):
        pos = text.find(pattern, pos + 1)
        if pos == -1:
            return -1
    return pos


async def _emit_interleaved(response_text: str, cards: list) -> AsyncGenerator:
    """基于完整文本的交错输出：文本分段 → 清理标记 → 16字符分块输出 → 插入卡片。

    卡片位置由 _find_card_boundaries 预计算，基于完整文本，定位准确。
    """
    tag_re = re.compile(r"「(?:商品\s*\d+\s*」|结语」)|\[(?:SUMMARY|PRODUCT_\d+|CLOSING)\]|【(?:SUMMARY|PRODUCT_\d+|CLOSING)】", re.IGNORECASE)
    closing_re = re.compile(r"「结语」|\[CLOSING\]|【CLOSING】", re.IGNORECASE)

    def _card(idx: int) -> dict:
        if idx >= len(cards):
            return None
        c = cards[idx]
        return {
            "event": "product_cards",
            "data": ProductCardEvent(
                product_id=c.get("product_id", c.get("id", "")),
                title=c.get("title", ""), price=c.get("price", 0),
                rating=c.get("rating", 0),
                match_score=c.get("match_score", c.get("score", 0.5)),
                highlights=c.get("highlights", []),
                image_url=c.get("image_url"), image_urls=c.get("image_urls", []),
                brand=c.get("brand"), category=c.get("category", ""),
                index=idx + 1, total=len(cards),
            ).model_dump_json(),
        }

    # 1. 分离结语：结语标记之后的内容不再插入卡片
    body_text = response_text
    closing_text = ""
    cm_body = closing_re.search(response_text)
    if cm_body:
        body_text = response_text[: cm_body.start()]
        closing_text = response_text[cm_body.start():]

    # 2. 找到每张卡片的插入点
    boundaries = _find_card_boundaries(body_text, cards)

    # 3. 按卡片边界切分文本 → 逐段输出文本 + 卡片
    emitted = set()
    prev_pos = 0
    for pos, card_idx in boundaries:
        if card_idx in emitted:
            continue
        seg_text = tag_re.sub("", body_text[prev_pos:pos])
        # 输出文本
        for i in range(0, len(seg_text), 16):
            chunk = seg_text[i : i + 16]
            if chunk:
                yield {"event": "text_delta", "data": TextDeltaEvent(content=chunk).model_dump_json()}
        # 输出卡片
        evt = _card(card_idx)
        if evt:
            emitted.add(card_idx)
            yield evt
        prev_pos = pos

    # 4. 剩余正文文本（卡片之后、结语之前）
    remaining_body = tag_re.sub("", body_text[prev_pos:])
    for i in range(0, len(remaining_body), 16):
        chunk = remaining_body[i : i + 16]
        if chunk:
            yield {"event": "text_delta", "data": TextDeltaEvent(content=chunk).model_dump_json()}

    # 5. 结语文本
    closing_text = tag_re.sub("", closing_text)
    for i in range(0, len(closing_text), 16):
        chunk = closing_text[i : i + 16]
        if chunk:
            yield {"event": "text_delta", "data": TextDeltaEvent(content=chunk).model_dump_json()}

    # 6. 兜底：未发送的卡片附在末尾
    for idx in range(len(cards)):
        if idx not in emitted:
            evt = _card(idx)
            if evt:
                yield evt


def _build_cache_key(message: str, conversation_id: str | None, history: list[dict]) -> str:
    """Build a context-aware cache key so short multi-turn replies do not collide."""
    recent = [
        {"role": h.get("role", ""), "content": (h.get("content", "") or "")[:200]}
        for h in history[-4:]
    ]
    return json.dumps(
        {
            "v": cache.CACHE_VERSION,
            "conversation_id": conversation_id or "",
            "message": message.strip(),
            "history": recent,
        },
        ensure_ascii=False,
        sort_keys=True,
    )


def _clean_stream_text(text: str) -> str:
    """Remove internal structure markers before streaming text to the client."""
    text = re.sub(r"「(?:商品\s*\d+\s*」|结语」)|\[(?:SUMMARY|PRODUCT_\d+|CLOSING)\]|【(?:SUMMARY|PRODUCT_\d+|CLOSING)】", "", text, flags=re.IGNORECASE)
    return text


async def generate_response(
    message: str,
    conversation_id: str | None = None,
    state: dict | None = None,
) -> AsyncGenerator[dict, None]:
    """
    Agent 真流式响应入口 — 被 api/chat.py 调用。

    架构（v2.0 真流式）：
    1. 缓存检查
    2. 意图分类 + 检索（非流式，~0.8s）
    3. LLM 边生成边推送 text_delta（真流式，TTFT ~1s DeepSeek / ~5s Doubao）
    4. 商品卡片 product_cards
    5. done

    SSE 事件格式:
    - text_delta: 流式文本增量（逐 token）
    - product_cards: 单张推荐商品卡片
    - done: 流结束
    - error: 错误
    """
    t_start = time.monotonic()
    try:
        # 获取多轮对话历史。缓存 key 也必须包含上下文，避免“便宜一点”等短句误命中。
        from app.services import state_manager as sm
        conversation_history = await sm.get_recent_messages(conversation_id or "", limit=6)
        cache_key = _build_cache_key(message, conversation_id, conversation_history)

        # ── 缓存检查 ──
        cached = await cache.get(message, cache_key=cache_key)
        if cached:
            response_text = cached["response"]
            cards = cached["cards"]
            # 安全阀：旧缓存可能包含>3件商品，截断至top 3
            if len(cards) > 3:
                cards = cards[:3]
            async for evt in _emit_interleaved(response_text, cards):
                yield evt
            elapsed = (time.monotonic() - t_start) * 1000
            yield {"event": "done", "data": DoneEvent(latency_ms=int(elapsed), message="cache-hit").model_dump_json()}
            return

        # ── 缓存检查 ──
        # 阶段 1: 意图分类 + 检索（非流式，~0.8s）
        # ═══════════════════════════════════════════════════════

        initial_state: AgentState = {
            "query": message,
            "session_id": conversation_id or "",
            "slots": state or {},
            "history": conversation_history,
        }

        # ── Progress 1: 立即推送首屏反馈，让用户感知 TTFT ≈ 0 ──
        yield {"event": "progress", "data": ProgressEvent(message="🔍 正在分析您的需求...").model_dump_json()}

        after_intent = await node_classify_intent(initial_state)

        # 闲聊直接回复
        if after_intent.get("intent") == "chitchat":
            yield {"event": "progress", "data": ProgressEvent(message="已理解您的问题，正在回复...").model_dump_json()}
            text = "你可以告诉我具体的需求，比如「推荐一款降噪耳机」「300元以内的运动鞋」「送女朋友的生日礼物」，我会帮你找到合适的商品～"
            yield {"event": "text_delta", "data": TextDeltaEvent(content=text).model_dump_json()}
            yield {"event": "done", "data": DoneEvent().model_dump_json()}
            return

        # ── 否定/排除语义检测：LLM 可能把 "排除 Apple" 误判为 web_search，
        #     但只要有否定关键词 + 对话历史（多轮上下文），就应走 anti_selection 检索路径
        neg_keywords = ["不要", "除了", "非", "不含", "排除", "拒绝", "去掉", "避开", "别"]
        has_negation_in_query = any(kw in message for kw in neg_keywords)
        has_negation_slots = bool(
            after_intent.get("slots", {}).get("exclude_brands") or
            after_intent.get("slots", {}).get("exclude_categories") or
            after_intent.get("slots", {}).get("exclude_text_terms")
        )
        has_history = len(conversation_history) >= 2
        if after_intent.get("intent") == "web_search" and (has_negation_in_query or has_negation_slots) and has_history:
            logger.info("Overriding web_search → anti_selection: negation detected in multi-turn context")
            after_intent["intent"] = "anti_selection"

        # ── 联网搜索：外部信息查询 → web_search node → 返回结果 ──
        if after_intent.get("intent") == "web_search":
            yield {"event": "progress", "data": ProgressEvent(message="正在联网搜索...").model_dump_json()}
            after_ws = await node_web_search(after_intent)
            ws_response = after_ws.get("response", "")
            web_results = after_ws.get("_web_results", [])

            # 发送搜索摘要文本
            yield {"event": "text_delta", "data": TextDeltaEvent(content=ws_response).model_dump_json()}

            # 发送每条搜索结果
            for i, wr in enumerate(web_results):
                wr_event = WebSearchResultEvent(
                    title=wr.get("title", ""),
                    url=wr.get("url", ""),
                    snippet=wr.get("snippet", ""),
                    index=i + 1,
                    total=len(web_results),
                )
                yield {"event": "web_search_result", "data": wr_event.model_dump_json()}

            yield {"event": "done", "data": DoneEvent().model_dump_json()}
            return

        # ── 购物车上下文检测：上轮是订单确认页，本轮回复视为确认/取消 ──
        cart_confirm_keywords = {"确认下单", "确认", "是的", "确定", "没错", "下单", "结算"}
        if after_intent.get("intent") != "cart_operation" and conversation_history:
            last_assistant = ""
            for m in reversed(conversation_history):
                if m.get("role") == "assistant":
                    last_assistant = m.get("content", "")
                    break
            if ("订单确认" in last_assistant or "确认下单" in last_assistant) and \
               any(kw in message for kw in cart_confirm_keywords):
                logger.info("Cart context override: detected checkout confirmation reply")
                after_intent["intent"] = "cart_operation"
                after_intent["slots"] = after_intent.get("slots", {})

        # ── 购物车操作：走 cart node → 直接返回，不做 RAG ──
        if after_intent.get("intent") == "cart_operation":
            yield {"event": "progress", "data": ProgressEvent(message="正在处理您的购物车...").model_dump_json()}
            after_cart = await node_cart(after_intent)
            cart_response = after_cart.get("response", "购物车操作完成。")
            yield {"event": "text_delta", "data": TextDeltaEvent(content=cart_response).model_dump_json()}
            yield {"event": "done", "data": DoneEvent().model_dump_json()}
            return

        # ── clarify 反问：缺失关键信息 / 短词仅有品类 / 低置信度 / 极短模糊词 → 追问用户 ──
        slots_check = after_intent.get("slots", {})
        missing_check = slots_check.get("missing_slots", [])
        has_cat = bool(slots_check.get("category"))
        has_scene = bool(slots_check.get("scenario"))
        has_budget = bool(slots_check.get("price_min") or slots_check.get("price_max"))
        has_attrs = bool(slots_check.get("attributes"))
        confidence = after_intent.get("confidence", 0.5)
        intent_type = after_intent.get("intent", "")
        original_query = message.strip()
        is_short_query = len(original_query) <= 4

        # 场景0：极短模糊词（≤2字且无具体商品名，"推荐"、"买东西"）→ 无论扩展与否都追问
        ultra_vague_words = {"推荐", "买东西", "推荐一下", "买什么", "买啥", "有什么", "推荐个"}
        is_ultra_vague = len(original_query) <= 3 or original_query in ultra_vague_words

        # 场景1：完全缺失关键信息（无品类无场景无预算无属性）→ 必须追问
        missing_everything = bool(missing_check) and not (has_cat or has_scene or has_budget or has_attrs)

        # 场景2：短词仅含品类（如"耳机"、"手机"）→ 追问细化需求
        only_has_category = has_cat and not (has_scene or has_budget or has_attrs)
        short_category_query = is_short_query and only_has_category

        # 场景3：低置信度 → LLM不理解用户意图
        low_confidence = confidence < 0.5 and intent_type == "commodity_recommend"

        needs_clarify = (is_ultra_vague or missing_everything or short_category_query or low_confidence) \
            and intent_type not in ("chitchat", "anti_selection", "cart_operation")
        # 有多轮历史时，短查询很可能是在之前推荐基础上的细化（如"要轻量的"），跳过追问
        has_history = len(conversation_history) >= 2
        logger.info("Clarify check: needs=%s history=%d short=%s ultra=%s cat=%s",
                    needs_clarify, len(conversation_history), is_short_query, is_ultra_vague, has_cat)
        if needs_clarify and has_history and (is_short_query or is_ultra_vague):
            logger.info("Clarify bypassed due to conversation history")
            needs_clarify = False
        if needs_clarify:
            # 持久化当前槽位，下次用户回复时可合并继承
            await sm.update_state(conversation_id or "", slots=slots_check)
            yield {"event": "progress", "data": ProgressEvent(message="正在分析您的需求细节...").model_dump_json()}
            after_clarify = await node_clarify(after_intent)
            clarify_text = after_clarify.get("response", "")
            missing_list = missing_check if isinstance(missing_check, list) else []
            from app.schemas.sse_events import ClarifyEvent
            yield {
                "event": "clarify",
                "data": ClarifyEvent(
                    question=clarify_text or "能再具体说说您的需求吗？",
                    missing_slots=missing_list,
                ).model_dump_json()
            }
            yield {"event": "done", "data": DoneEvent().model_dump_json()}
            return

        # ── Progress 2: 意图分类完成，进入检索阶段 ──
        yield {"event": "progress", "data": ProgressEvent(message="已理解需求，正在检索商品...").model_dump_json()}

        after_retrieve = await node_retrieve(after_intent)
        chunks = after_retrieve.get("retrieved_chunks", [])
        slots = after_retrieve.get("slots", {})

        if not chunks:
            if slots.get("category"):
                logger.info("No precise results for '%s', expanding within category=%s", message[:40], slots.get("category"))
                chunks = await _retrieve_same_category_supplements(message, slots, chunks)

        if not chunks:
            # 兜底策略：无过滤检索 + 热门推荐词
            # 清除排除条件，避免 "排除 Apple" 后所有结果都被过滤导致为空
            logger.info("No results for '%s', trying hot fallback...", message[:40])
            fallback_slots = {k: v for k, v in slots.items() if k not in (
                "exclude_brands", "exclude_by_category", "exclude_categories",
                "exclude_attributes", "exclude_text_terms",
            )} if slots.get("category") else {}
            fallback_query = f"{slots.get('category')} 热门推荐" if slots.get("category") else "热门推荐 热销商品"
            fallback_state = {**after_intent, "query": fallback_query, "rewritten_query": fallback_query, "slots": fallback_slots}
            after_fallback = await node_retrieve(fallback_state)
            chunks = after_fallback.get("retrieved_chunks", [])
            slots = after_fallback.get("slots", fallback_slots)
            if not chunks:
                # 最终兜底：联网搜索
                yield {"event": "progress", "data": ProgressEvent(message="本地商品库未匹配，正在联网搜索...").model_dump_json()}
                after_ws = await node_web_search(after_intent)
                ws_response = after_ws.get("response", "")
                web_results = after_ws.get("_web_results", [])
                yield {"event": "text_delta", "data": TextDeltaEvent(content=ws_response).model_dump_json()}
                for i, wr in enumerate(web_results):
                    yield {"event": "web_search_result", "data": WebSearchResultEvent(
                        title=wr.get("title", ""), url=wr.get("url", ""),
                        snippet=wr.get("snippet", ""), index=i + 1, total=len(web_results),
                    ).model_dump_json()}
                yield {"event": "done", "data": DoneEvent().model_dump_json()}
                return
            yield {"event": "progress", "data": ProgressEvent(message="未精确匹配，为您推荐热销商品...").model_dump_json()}

        # ── Progress 3: 检索完成，告知命中数量 ──
        yield {"event": "progress", "data": ProgressEvent(message=f"📦 已匹配 {len(chunks)} 件商品，正在为您筛选...").model_dump_json()}

        # ═══════════════════════════════════════════════════════
        # 阶段 2: 商品排序 + Prompt 构建（复用共享辅助函数）
        # ═══════════════════════════════════════════════════════

        from app.services.product_ranker import rank_products

        raw_products = _extract_raw_products(chunks)
        raw_products = _filter_products_by_requested_category(raw_products, slots.get("category", ""))
        user_prefs = _build_user_prefs(slots)
        ranked = rank_products(raw_products, user_prefs, after_retrieve.get("intent", ""), top_k=3)
        if len(ranked) < 3 and slots.get("category"):
            logger.info(
                "Only %d ranked products after exclusions; supplementing category=%s",
                len(ranked), slots.get("category")
            )
            chunks = await _retrieve_same_category_supplements(message, slots, chunks)
            raw_products = _extract_raw_products(chunks, limit=30)
            raw_products = _filter_products_by_requested_category(raw_products, slots.get("category", ""))
            user_prefs = _build_user_prefs(slots)
            ranked = rank_products(raw_products, user_prefs, after_retrieve.get("intent", ""), top_k=3)
        valid_ranked, is_reliable = _validate_ranked_products(ranked)
        logger.info("Ranked: %d products, valid: %d, reliable: %s", len(ranked), len(valid_ranked), is_reliable)

        if not valid_ranked:
            yield {"event": "progress", "data": ProgressEvent(message="未找到匹配商品").model_dump_json()}
            yield {"event": "progress", "data": ProgressEvent(message="本地商品库未匹配，正在联网搜索...").model_dump_json()}
            after_ws = await node_web_search(after_intent)
            ws_response = after_ws.get("response", "")
            web_results = after_ws.get("_web_results", [])
            yield {"event": "text_delta", "data": TextDeltaEvent(content=ws_response).model_dump_json()}
            for i, wr in enumerate(web_results):
                yield {"event": "web_search_result", "data": WebSearchResultEvent(
                    title=wr.get("title", ""), url=wr.get("url", ""),
                    snippet=wr.get("snippet", ""), index=i + 1, total=len(web_results),
                ).model_dump_json()}
            yield {"event": "done", "data": DoneEvent().model_dump_json()}
            return

        yield {"event": "progress", "data": ProgressEvent(message="📊 正在生成推荐...").model_dump_json()}

        cards = _assemble_cards(valid_ranked)
        prompt = _build_generation_prompt(message, slots, valid_ranked, is_reliable, after_retrieve.get("intent", ""), history=conversation_history)

        # ═══════════════════════════════════════════════════════
        # 阶段 4: LLM 生成 + 交错输出 — 摘要 → (商品文本 + 卡片) × N → 结语
        # ═══════════════════════════════════════════════════════

        from app.services.llm_client import chat_completion

        logger.info("Starting LLM call for interleaved output...")
        stream = await chat_completion(
            messages=[{"role": "user", "content": prompt}],
            temperature=0.7,
            max_tokens=400,
            stream=True,
        )

        t_first_token = None
        response_text = ""
        async for event in _stream_interleaved(stream, cards):
            if event["event"] == "text_delta":
                if t_first_token is None:
                    t_first_token = time.monotonic()
                data = json.loads(event["data"])
                response_text += data.get("content", "")
            yield event

        ttft_ms = int((t_first_token - t_start) * 1000) if t_first_token else 0
        logger.info("LLM done: %d chars, TTFT=%dms", len(response_text), ttft_ms)

        # ═══════════════════════════════════════════════════════
        # 阶段 6: 缓存 + 状态回写
        # ═══════════════════════════════════════════════════════

        await cache.set(message, response_text, cards, cache_key=cache_key)

        try:
            await sm.update_state(
                conversation_id or "",
                slots=slots,
                product_cards=cards,
                intent=after_retrieve.get("intent", ""),
            )
        except Exception as e:
            logger.warning("State update failed for conversation '%s': %s", conversation_id, e)

        total_ms = int((time.monotonic() - t_start) * 1000)
        # 仅暴露前端需要的状态字段，不泄露内部标记
        client_slots = {k: v for k, v in slots.items()
                        if not k.startswith("_") and k not in ("missing_slots", "exclude_text_terms")}
        yield {"event": "done", "data": DoneEvent(latency_ms=total_ms, total_cards=len(cards), slots=client_slots).model_dump_json()}

    except Exception as exc:
        logger.exception("Agent pipeline error")
        error = ErrorEvent(message=str(exc), code="AGENT_ERROR")
        yield {"event": "error", "data": error.model_dump_json()}
        yield {"event": "done", "data": DoneEvent().model_dump_json()}
