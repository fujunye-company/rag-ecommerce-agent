"""
Agent 编排 — LangGraph StateGraph 工作流
流程: classify_intent → (missing_slots? → clarify → generate | retrieve → generate) → SSE stream
"""
import json
import logging
from typing import TypedDict, AsyncGenerator
from langgraph.graph import StateGraph, END
from app.services.intent import classify_intent, extract_slots, rewrite_query, extract_negation_slots
from app.services.rag import retrieve as rag_retrieve
from app.services.reranker import rerank_async
from app.services.llm_client import chat_completion
from app.schemas.sse_events import TextDeltaEvent, ProductCardEvent, DoneEvent, ErrorEvent, ProgressEvent
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


# ── Nodes ──

async def node_classify_intent(state: AgentState) -> AgentState:
    """意图分类 + 槽位填充"""
    query = state["query"]

    if not query or len(query.strip()) < 2:
        state["intent"] = "chitchat"
        state["confidence"] = 1.0
        state["slots"] = {}
        return state

    intent_result = await classify_intent(query)
    state["intent"] = intent_result["intent"]
    state["confidence"] = intent_result["confidence"]

    # 非闲聊意图才提取槽位
    if state["intent"] != "chitchat":
        slots = await extract_slots(query, state["intent"])

        # 合并历史上下文：保留上一轮的排除条件，本轮的偏好覆盖历史
        prev_slots = state.get("slots", {})
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
        # 偏好类字段：本轮覆盖历史
        for key in slots:
            if key not in merged:
                merged[key] = slots[key]
        # 保留本轮未涉及的历史偏好
        for key in prev_slots:
            if key not in merged:
                merged[key] = prev_slots[key]

        state["slots"] = merged

        # 否定语义：任何含否定关键词的查询都提取排除条件（不只 anti_selection）
        neg_keywords = ["不要", "除了", "非", "不含", "排除", "拒绝", "去掉", "避开", "别"]
        has_negation = any(kw in query for kw in neg_keywords)
        if state["intent"] == "anti_selection" or has_negation:
            negation = await extract_negation_slots(query)
            state["slots"]["exclude_brands"] = negation.get("exclude_brands", [])
            state["slots"]["exclude_categories"] = negation.get("exclude_categories", [])
            state["slots"]["exclude_attributes"] = negation.get("exclude_attributes", {})

            # 补充关键词提取的文本级排除词（LLM 可能不返回此字段）
            from app.services.intent import _keyword_extract_negation
            kw_neg = _keyword_extract_negation(query)
            state["slots"]["exclude_text_terms"] = kw_neg.get("exclude_text_terms", [])
            # LLM 未覆盖的排除项，用关键词结果补充
            if not state["slots"]["exclude_brands"] and kw_neg.get("exclude_brands"):
                state["slots"]["exclude_brands"] = kw_neg["exclude_brands"]
            if not state["slots"]["exclude_attributes"] and kw_neg.get("exclude_attributes"):
                state["slots"]["exclude_attributes"] = kw_neg["exclude_attributes"]

            logger.info("Negation extracted: brands=%s, attrs=%s, text_terms=%s",
                        state["slots"]["exclude_brands"],
                        state["slots"]["exclude_attributes"],
                        state["slots"].get("exclude_text_terms"))

        # 改写查询（用 positive_query 替代原始 query）
        positive_q = negation.get("positive_query", "") if has_negation else ""
        rewritten = await rewrite_query(positive_q or query, slots)
        state["rewritten_query"] = rewritten
    else:
        state["slots"] = {}

    return state


async def node_clarify(state: AgentState) -> AgentState:
    """反问节点：缺失关键信息时，生成追问问题"""
    slots = state.get("slots", {})
    missing = slots.get("missing_slots", [])

    if not missing:
        # 没有缺失信息，fallback 跳过
        state["response"] = ""
        return state

    # 构造追问 prompt
    query = state["query"]
    intent = state.get("intent", "commodity_recommend")
    missing_str = "、".join(missing)

    prompt = f"""你是一个电商导购助手。用户刚才说：「{query}」，但缺少以下关键信息：{missing_str}。

请生成一个简短、友好的追问问题（1-2句话，不超过60字），引导用户补充缺失的信息。
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
        # LLM 降级：模板生成追问
        clarify_map = {
            "品类": f"请问您想购买什么品类的商品呢？",
            "价格": f"请问您的预算大概是多少呢？",
            "品牌": f"请问您有偏好的品牌吗？",
            "场景": f"请问是买来做什么用的呢？",
        }
        # 尝试匹配
        question = None
        for key, tmpl in clarify_map.items():
            if any(key in m for m in missing):
                question = tmpl
                break
        state["response"] = question or f"能再具体说说您的需求吗？比如{'、'.join(missing)}。"

    logger.info("Clarify: missing=%s → response=%s", missing, state["response"])
    return state


async def node_retrieve(state: AgentState) -> AgentState:
    """RAG 检索"""
    if state["intent"] == "chitchat":
        state["retrieved_chunks"] = []
        return state

    query = state.get("rewritten_query") or state["query"]
    slots = state.get("slots", {})

    result = await rag_retrieve(
        query=query,
        top_k=10,
        category=slots.get("category"),
        price_min=slots.get("price_min"),
        price_max=slots.get("price_max"),
        exclude_brands=slots.get("exclude_brands"),
        exclude_categories=slots.get("exclude_categories"),
        exclude_attributes=slots.get("exclude_attributes"),
    )

    state["retrieved_chunks"] = result["chunks"]
    state["latency_ms"] = result["latency_ms"]

    # 文本级兜底过滤：排除 title/highlights 中含否定词的商品
    text_terms = slots.get("exclude_text_terms", [])
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

    # ── Reranker 精排 ──
    if state["retrieved_chunks"] and state["intent"] != "chitchat":
        try:
            query_text = state.get("rewritten_query") or state["query"]
            state["retrieved_chunks"] = await rerank_async(
                query_text, state["retrieved_chunks"], top_k=10
            )
            logger.info("Reranker applied: %d chunks re-ranked", len(state["retrieved_chunks"]))
        except Exception as e:
            logger.warning("Reranker unavailable, using raw retrieval: %s", e)

    logger.info("Agent retrieved %d chunks in %.0fms", len(state["retrieved_chunks"]), state["latency_ms"])
    return state


async def node_generate(state: AgentState) -> AgentState:
    """生成回答 + 商品卡片"""
    # 购物车操作：cart 节点已生成 response，直接透传
    if state.get("intent") == "cart_operation" and state.get("response"):
        state["product_cards"] = state.get("product_cards", [])
        return state

    if state["intent"] == "chitchat":
        state["response"] = "你好！我是AI导购助手，可以帮你推荐商品、对比参数、解答疑问。有什么需要帮忙的吗？"
        state["product_cards"] = []
        return state

    chunks = state.get("retrieved_chunks", [])
    if not chunks:
        state["response"] = "抱歉，暂时没有找到符合您要求的商品。可以试试调整条件重新搜索吗？"
        state["product_cards"] = []
        return state

    # 构造商品卡片 — 通过 ProductRanker 多维打分排序
    from app.services.product_ranker import rank_products

    # 从 chunks 提取商品属性
    raw_products = []
    for chunk in chunks[:10]:
        p = chunk["payload"]
        raw_products.append({
            "title": p.get("title"),
            "price": p.get("price"),
            "rating": p.get("rating"),
            "brand": p.get("brand"),
            "category": p.get("category"),
            "attributes": p.get("attributes", {}),
            "semantic_score": round(chunk.get("final_score", chunk.get("score", 0.5)), 4),
            "highlights": p.get("highlights", []),
            "image_url": (p.get("image_urls", [None]) or [None])[0] if p.get("image_urls") else p.get("image_url"),
        })

    # 构造用户偏好
    user_prefs = {
        "price_min": slots.get("price_min"),
        "price_max": slots.get("price_max"),
        "brand_preference": slots.get("brand_preference"),
        "attributes": slots.get("attributes", {}),
        "exclude_brands": slots.get("exclude_brands") or [],
        "exclude_attributes": slots.get("exclude_attributes", {}),
    }

    # ProductRanker 多维排序
    ranked = rank_products(raw_products, user_prefs, state["intent"], top_k=5)

    # 组装最终卡片
    cards = []
    for r in ranked:
        cards.append({
            "id": r.get("product_id"),
            "title": r["title"],
            "price": r.get("price"),
            "category": r.get("category"),
            "brand": r.get("brand"),
            "rating": r.get("rating"),
            "image_urls": [r.get("image_url")] if r.get("image_url") else [],
            "highlights": r.get("highlights", []),
            "match_score": r.get("match_score", 0.5),
            "rank_reason": r.get("rank_reason", ""),
        })

    state["product_cards"] = cards

    # 构建LLM上下文
    context_parts = []
    for i, r in enumerate(ranked[:5]):
        context_parts.append(
            f"[{i+1}] {r.get('title')} | ¥{r.get('price')} | ★{r.get('rating')} | "
            f"{' '.join(r.get('highlights', [])[:2])} | {r.get('rank_reason', '')}"
        )

    # LLM 生成推荐理由（三段式 + 自信度 + 反幻觉）
    context = "\n".join(context_parts)
    user_query = state["query"]
    slots = state.get("slots", {})

    prompt = f"""你是一个电商导购助手。基于检索到的商品信息，为用户生成推荐回答。

⚠️ 严禁事项（违反将直接扣分）:
1. 不得编造任何商品名称、型号、价格 — 只能引用上方[1][2]...标记的商品
2. 不得编造优惠券、满减、折扣、赠品信息
3. 不得编造不存在的功能参数（如杜比全景声、Hi-Res认证等）
4. 不得编造用户评价、销量排名、市场占有率
5. 如果检索结果中没有匹配商品，直接说"暂未找到合适商品"，不要凭空推荐

校验规则:
- 你提到的每个价格数字，必须在检索数据中出现过
- 你提到的每个品牌名，必须在检索数据中出现过
- 违反以上任一条，请删除该推荐

用户需求：{user_query}
用户预算：{slots.get('price_min', '不限')}-{slots.get('price_max', '不限')}

检索到的商品：
{context}

为每款推荐商品简要说明（每款≤40字）：
① 匹配点：为什么适合 ② 核心参数：引用具体数字

要求：
- 每款商品标明综合匹配度（如"匹配度：92%"）
- 预算内的商品标注"在预算内"
- 有不足请诚实标注
- 总字数≤200字
- 引用具体数字，不要用模糊词"""

    try:
        # 真 SSE 流式：stream=True 逐 token 返回
        stream = await chat_completion(
            messages=[{"role": "user", "content": prompt}],
            temperature=0.7,
            max_tokens=400,
            stream=True,
        )
        response_text = ""
        async for chunk in stream:
            if chunk.choices and chunk.choices[0].delta.content:
                token = chunk.choices[0].delta.content
                response_text += token
        state["response"] = response_text
    except Exception as e:
        logger.error("LLM generate failed: %s", e)
        state["response"] = f"为您找到 {len(cards)} 款相关商品。如需进一步筛选，请告诉我您的偏好。"
    return state


# ── Cart Helpers ──

def _extract_cart_action(query: str) -> str:
    """从用户查询中提取购物车操作类型"""
    q = query.lower()
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
                except Exception:
                    pass
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
                else:
                    state["response"] = (
                        "抱歉，没有找到要添加的商品。请先搜索商品（如「推荐蓝牙耳机」），"
                        "然后说「把第一个加入购物车」来添加。"
                    )
                state["product_cards"] = []

            elif cart_action == "remove":
                state["response"] = await _remove_from_cart(query, session_id, db)
                state["product_cards"] = []

            elif cart_action == "clear":
                await cart_service.clear_cart(db, session_id)
                state["response"] = "🗑️ 购物车已清空。"
                state["product_cards"] = []

            elif cart_action == "checkout":
                items = await cart_service.get_cart(db, session_id)
                total = await cart_service.get_cart_total(db, session_id)
                if items:
                    item_lines = [f"{i+1}. {it.title} ×{it.quantity} — ¥{it.price * it.quantity:.0f}"
                                  for i, it in enumerate(items)]
                    state["response"] = (
                        "📋 订单确认：\n" + "\n".join(item_lines)
                        + f"\n\n💰 合计：¥{total:.0f}\n\n"
                        + "输入「确认下单」完成购买（演示环境，不会实际扣款）。"
                    )
                else:
                    state["response"] = "购物车是空的，无法下单。请先添加商品。"
                state["product_cards"] = []

    except Exception as e:
        logger.error("Cart operation failed: %s", e)
        state["response"] = f"购物车操作失败：{str(e)}"
        state["product_cards"] = []

    return state


# ── Router ──

def route_after_intent(state: AgentState) -> str:
    """意图路由：闲聊 → generate, 缺失信息 → clarify, 购物车 → cart, 其他 → retrieve"""
    if state.get("intent") == "chitchat":
        return "generate"

    if state.get("intent") == "cart_operation":
        return "cart"

    # 检查是否有缺失的关键信息——仅当品类和场景都缺失时才追问
    slots = state.get("slots", {})
    missing = slots.get("missing_slots", [])
    # 只有同时缺失品类和场景（完全不知道用户要什么）才触发 clarify
    has_category = bool(slots.get("category"))
    has_scenario = bool(slots.get("scenario"))
    has_budget = bool(slots.get("price_min") or slots.get("price_max"))
    has_attrs = bool(slots.get("attributes"))
    # 至少有一个关键信息就不追问
    if missing and not (has_category or has_scenario or has_budget or has_attrs) and state.get("intent") != "anti_selection":
        # anti_selection 的 missing_slots 通常是 LLM 误判，不追问
        return "clarify"

    return "retrieve"


# ── Graph Builder ──

def build_agent_graph() -> StateGraph:
    workflow = StateGraph(AgentState)

    workflow.add_node("classify_intent", node_classify_intent)
    workflow.add_node("clarify", node_clarify)
    workflow.add_node("retrieve", node_retrieve)
    workflow.add_node("cart", node_cart)
    workflow.add_node("generate", node_generate)

    workflow.set_entry_point("classify_intent")
    workflow.add_conditional_edges("classify_intent", route_after_intent, {
        "retrieve": "retrieve",
        "clarify": "clarify",
        "cart": "cart",
        "generate": "generate",
    })
    workflow.add_edge("clarify", "generate")
    workflow.add_edge("retrieve", "generate")
    workflow.add_edge("cart", "generate")
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


# ── Streaming Entry Point ──

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
    t_start = __import__("time").monotonic()
    try:
        # ── 缓存检查 ──
        cached = await cache.get(message)
        if cached:
            response_text = cached["response"]
            cards = cached["cards"]
            for i in range(0, len(response_text), 20):
                chunk = response_text[i:i + 20]
                event = TextDeltaEvent(content=chunk)
                yield {"event": "text_delta", "data": event.model_dump_json()}
            if cards:
                total = len(cards)
                for i, card in enumerate(cards):
                    event = ProductCardEvent(
                        product_id=card.get("product_id", card.get("id", "")),
                        title=card.get("title", ""),
                        price=card.get("price", 0),
                        rating=card.get("rating", 0),
                        match_score=card.get("match_score", card.get("score", 0.5)),
                        highlights=card.get("highlights", []),
                        image_url=card.get("image_url"),
                        index=i + 1,
                        total=total,
                    )
                    yield {"event": "product_cards", "data": event.model_dump_json()}
            elapsed = (__import__("time").monotonic() - t_start) * 1000
            yield {"event": "done", "data": DoneEvent(latency_ms=int(elapsed), message="cache-hit").model_dump_json()}
            return

        # ── 缓存检查 ──
        # 阶段 1: 意图分类 + 检索（非流式，~0.8s）
        # ═══════════════════════════════════════════════════════

        initial_state: AgentState = {
            "query": message,
            "session_id": conversation_id or "",
            "slots": state or {},
        }

        # ── Progress 1: 立即推送首屏反馈，让用户感知 TTFT ≈ 0 ──
        yield {"event": "progress", "data": ProgressEvent(message="🔍 正在分析您的需求...").model_dump_json()}

        after_intent = await node_classify_intent(initial_state)

        # 闲聊直接回复
        if after_intent.get("intent") == "chitchat":
            yield {"event": "progress", "data": ProgressEvent(message="已理解您的问题，正在回复...").model_dump_json()}
            text = "你好！我是AI导购助手，可以帮你推荐商品、对比参数、解答疑问。有什么需要帮忙的吗？"
            yield {"event": "text_delta", "data": TextDeltaEvent(content=text).model_dump_json()}
            yield {"event": "done", "data": DoneEvent().model_dump_json()}
            return

        # ── 购物车操作：走 cart node → 直接返回，不做 RAG ──
        if after_intent.get("intent") == "cart_operation":
            yield {"event": "progress", "data": ProgressEvent(message="正在处理您的购物车...").model_dump_json()}
            after_cart = await node_cart(after_intent)
            cart_response = after_cart.get("response", "购物车操作完成。")
            yield {"event": "text_delta", "data": TextDeltaEvent(content=cart_response).model_dump_json()}
            yield {"event": "done", "data": DoneEvent().model_dump_json()}
            return

        # ── clarify 反问：缺失关键信息且没有任何关键参数 → 追问用户 ──
        slots_check = after_intent.get("slots", {})
        missing_check = slots_check.get("missing_slots", [])
        has_cat = bool(slots_check.get("category"))
        has_scene = bool(slots_check.get("scenario"))
        has_budget = bool(slots_check.get("price_min") or slots_check.get("price_max"))
        has_attrs = bool(slots_check.get("attributes"))
        if missing_check and not (has_cat or has_scene or has_budget or has_attrs) and after_intent.get("intent") != "anti_selection":
            yield {"event": "progress", "data": ProgressEvent(message="正在分析您的需求细节...").model_dump_json()}
            after_clarify = await node_clarify(after_intent)
            clarify_text = after_clarify.get("response", "")
            if clarify_text:
                yield {"event": "text_delta", "data": TextDeltaEvent(content=clarify_text).model_dump_json()}
            else:
                yield {"event": "text_delta", "data": TextDeltaEvent(content="能再具体说说您的需求吗？").model_dump_json()}
            yield {"event": "done", "data": DoneEvent().model_dump_json()}
            return

        # ── Progress 2: 意图分类完成，进入检索阶段 ──
        yield {"event": "progress", "data": ProgressEvent(message="已理解需求，正在检索商品...").model_dump_json()}

        after_retrieve = await node_retrieve(after_intent)
        chunks = after_retrieve.get("retrieved_chunks", [])
        slots = after_retrieve.get("slots", {})

        if not chunks:
            yield {"event": "progress", "data": ProgressEvent(message="未找到匹配商品").model_dump_json()}
            text = "抱歉，暂时没有找到符合您要求的商品。可以试试调整条件重新搜索吗？"
            yield {"event": "text_delta", "data": TextDeltaEvent(content=text).model_dump_json()}
            yield {"event": "done", "data": DoneEvent().model_dump_json()}
            return

        # ── Progress 3: 检索完成，告知命中数量 ──
        yield {"event": "progress", "data": ProgressEvent(message=f"📦 已匹配 {len(chunks)} 件商品，正在为您筛选...").model_dump_json()}

        # ═══════════════════════════════════════════════════════
        # 阶段 2: 商品排序（从检索结果构造卡片）
        # ═══════════════════════════════════════════════════════

        from app.services.product_ranker import rank_products

        raw_products = []
        for chunk in chunks[:10]:
            p = chunk["payload"]
            raw_products.append({
                "title": p.get("title"),
                "price": p.get("price"),
                "rating": p.get("rating"),
                "brand": p.get("brand"),
                "category": p.get("category"),
                "attributes": p.get("attributes", {}),
                "semantic_score": round(chunk.get("final_score", chunk.get("score", 0.5)), 4),
                "highlights": p.get("highlights", []),
                "image_url": (p.get("image_urls", [None]) or [None])[0] if p.get("image_urls") else p.get("image_url"),
            })

        user_prefs = {
            "price_min": slots.get("price_min"),
            "price_max": slots.get("price_max"),
            "brand_preference": slots.get("brand_preference"),
            "attributes": slots.get("attributes", {}),
            "exclude_brands": slots.get("exclude_brands") or [],
            "exclude_attributes": slots.get("exclude_attributes", {}),
        }

        ranked = rank_products(raw_products, user_prefs, after_retrieve.get("intent", ""), top_k=5)

        # ── Progress 4: 排序完成，即将进入 LLM 生成 ──
        yield {"event": "progress", "data": ProgressEvent(message="📊 正在生成推荐...").model_dump_json()}

        cards = []
        for r in ranked:
            cards.append({
                "id": r.get("product_id"),
                "title": r["title"],
                "price": r.get("price"),
                "category": r.get("category"),
                "brand": r.get("brand"),
                "rating": r.get("rating"),
                "image_urls": [r.get("image_url")] if r.get("image_url") else [],
                "highlights": r.get("highlights", []),
                "match_score": r.get("match_score", 0.5),
                "rank_reason": r.get("rank_reason", ""),
            })

        # ═══════════════════════════════════════════════════════
        # 阶段 3: 构建 Prompt
        # ═══════════════════════════════════════════════════════

        context_parts = []
        for i, r in enumerate(ranked[:5]):
            context_parts.append(
                f"[{i+1}] {r.get('title')} | ¥{r.get('price')} | ★{r.get('rating')} | "
                f"{' '.join(r.get('highlights', [])[:2])} | {r.get('rank_reason', '')}"
            )
        context = "\n".join(context_parts)

        prompt = f"""你是一个电商导购助手。基于检索到的商品信息，为用户生成推荐回答。
严格只使用下方检索到的商品信息，不得编造任何不存在的数据、优惠券、功能或价格。

用户需求：{message}
用户预算：{slots.get('price_min', '不限')}-{slots.get('price_max', '不限')}

检索到的商品：
{context}

为每款推荐商品按以下三段式生成推荐理由（每段≤30字）：
① 匹配依据：为什么这款适合用户需求
② 品质亮点：引用具体数字支撑的核心优势
③ 适用场景：最佳使用场景

要求：
- 每款商品附带综合匹配度（如"综合匹配度：92%"）
- 如果用户有预算，说明商品是否在预算内
- 如果商品有明显不足，诚实标注不推荐理由
- 结尾可追问用户偏好以进一步筛选
- 总字数控制在250字以内
- 禁止使用"非常好""很不错"等模糊词，必须引用具体数字"""

        # ═══════════════════════════════════════════════════════
        # 阶段 4: 真流式 LLM — 边生成边推送 text_delta
        # ═══════════════════════════════════════════════════════

        from app.services.llm_client import chat_completion

        logger.info("Starting true streaming LLM call...")
        stream = await chat_completion(
            messages=[{"role": "user", "content": prompt}],
            temperature=0.7,
            max_tokens=400,
            stream=True,
        )

        t_first_token = None
        response_text = ""
        async for chunk in stream:
            delta = chunk.choices[0].delta
            # 跳过 reasoning_content（豆包 Seed 模型会先输出推理链）
            if delta.content:
                if t_first_token is None:
                    t_first_token = __import__("time").monotonic()
                response_text += delta.content
                yield {"event": "text_delta", "data": TextDeltaEvent(content=delta.content).model_dump_json()}

        ttft_ms = int((t_first_token - t_start) * 1000) if t_first_token else 0
        logger.info("LLM streaming done: %d chars, TTFT=%dms", len(response_text), ttft_ms)

        # ═══════════════════════════════════════════════════════
        # 阶段 5: 发送商品卡片
        # ═══════════════════════════════════════════════════════

        if cards:
            total = len(cards)
            for i, card in enumerate(cards):
                event = ProductCardEvent(
                    product_id=card.get("product_id", card.get("id", "")),
                    title=card.get("title", ""),
                    price=card.get("price", 0),
                    rating=card.get("rating", 0),
                    match_score=card.get("match_score", card.get("score", 0.5)),
                    highlights=card.get("highlights", []),
                    image_url=card.get("image_url"),
                    index=i + 1,
                    total=total,
                )
                yield {"event": "product_cards", "data": event.model_dump_json()}

        # ═══════════════════════════════════════════════════════
        # 阶段 6: 缓存 + 状态回写
        # ═══════════════════════════════════════════════════════

        await cache.set(message, response_text, cards)

        try:
            await sm.update_state(
                conversation_id or "",
                **slots,
                product_cards=cards,
                intent=after_retrieve.get("intent", ""),
            )
        except Exception:
            pass

        total_ms = int((__import__("time").monotonic() - t_start) * 1000)
        yield {"event": "done", "data": DoneEvent(latency_ms=total_ms, total_cards=len(cards)).model_dump_json()}

    except Exception as exc:
        logger.exception("Agent pipeline error")
        error = ErrorEvent(message=str(exc), code="AGENT_ERROR")
        yield {"event": "error", "data": error.model_dump_json()}
        yield {"event": "done", "data": DoneEvent().model_dump_json()}
