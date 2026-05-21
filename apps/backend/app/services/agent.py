"""
Agent 编排 — LangGraph StateGraph 工作流
流程: classify_intent → retrieve → generate → SSE stream
"""
import json
import logging
from typing import TypedDict, AsyncGenerator
from langgraph.graph import StateGraph, END
from app.services.intent import classify_intent, extract_slots, rewrite_query, extract_negation_slots
from app.services.rag import retrieve as rag_retrieve
from app.services.llm_client import chat_completion
from app.schemas.sse_events import TextDeltaEvent, ProductCardEvent, DoneEvent, ErrorEvent
from app.services import cache

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
        state["slots"] = slots
        # 否定语义：提取排除条件
        if state["intent"] == "anti_selection":
            negation = await extract_negation_slots(query)
            state["slots"]["exclude_brands"] = negation.get("exclude_brands", [])
            state["slots"]["exclude_categories"] = negation.get("exclude_categories", [])
            state["slots"]["exclude_attributes"] = negation.get("exclude_attributes", {})
        # 改写查询
        rewritten = await rewrite_query(query, slots)
        state["rewritten_query"] = rewritten
    else:
        state["slots"] = {}

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
    logger.info("Agent retrieved %d chunks in %.0fms", len(result["chunks"]), result["latency_ms"])
    return state


async def node_generate(state: AgentState) -> AgentState:
    """生成回答 + 商品卡片"""
    if state["intent"] == "chitchat":
        state["response"] = "你好！我是AI导购助手，可以帮你推荐商品、对比参数、解答疑问。有什么需要帮忙的吗？"
        state["product_cards"] = []
        return state

    chunks = state.get("retrieved_chunks", [])
    if not chunks:
        state["response"] = "抱歉，暂时没有找到符合您要求的商品。可以试试调整条件重新搜索吗？"
        state["product_cards"] = []
        return state

    # 构造商品卡片
    cards = []
    context_parts = []
    for i, chunk in enumerate(chunks[:5]):
        p = chunk["payload"]
        card = {
            "id": p.get("product_id"),
            "title": p.get("title"),
            "price": p.get("price"),
            "category": p.get("category"),
            "brand": p.get("brand"),
            "rating": p.get("rating"),
            "image_urls": [],
            "highlights": p.get("highlights", []),
            "match_score": round(chunk["score"], 3),
        }
        cards.append(card)
        context_parts.append(
            f"[{i+1}] {p.get('title')} | ¥{p.get('price')} | ★{p.get('rating')} | "
            f"{' '.join(p.get('highlights', [])[:2])}"
        )

    state["product_cards"] = cards

    # LLM 生成推荐理由（三段式 + 自信度 + 反幻觉）
    context = "\n".join(context_parts)
    user_query = state["query"]
    slots = state.get("slots", {})

    prompt = f"""你是一个电商导购助手。基于检索到的商品信息，为用户生成推荐回答。
严格只使用下方检索到的商品信息，不得编造任何不存在的数据、优惠券、功能或价格。

用户需求：{user_query}
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


# ── Router ──

def route_after_intent(state: AgentState) -> str:
    """意图路由：闲聊 → END，其他 → retrieve"""
    if state.get("intent") == "chitchat":
        return "generate"
    return "retrieve"


# ── Graph Builder ──

def build_agent_graph() -> StateGraph:
    workflow = StateGraph(AgentState)

    workflow.add_node("classify_intent", node_classify_intent)
    workflow.add_node("retrieve", node_retrieve)
    workflow.add_node("generate", node_generate)

    workflow.set_entry_point("classify_intent")
    workflow.add_conditional_edges("classify_intent", route_after_intent, {
        "retrieve": "retrieve",
        "generate": "generate",
    })
    workflow.add_edge("retrieve", "generate")
    workflow.add_edge("generate", END)

    return workflow


agent_graph = build_agent_graph().compile()


# ── Streaming Entry Point ──

async def generate_response(
    message: str,
    conversation_id: str | None = None,
    state: dict | None = None,
) -> AsyncGenerator[dict, None]:
    """
    Agent 流式响应入口 — 被 api/chat.py 调用。
    SSE 事件格式:
    - text_delta: 流式文本增量
    - product_cards: 推荐商品卡片
    - done: 流结束
    - error: 错误
    """
    try:
        # ── 缓存检查 ──
        cached = cache.get(message)
        if cached:
            response_text = cached["response"]
            cards = cached["cards"]
            for i in range(0, len(response_text), 20):
                chunk = response_text[i:i + 20]
                event = TextDeltaEvent(content=chunk)
                yield {"event": "text_delta", "data": event.model_dump_json()}
            if cards:
                event = ProductCardEvent(products=cards)
                yield {"event": "product_cards", "data": event.model_dump_json()}
            yield {"event": "done", "data": DoneEvent().model_dump_json()}
            return

        # ── 快速购物车处理 ──
        if any(kw in message for kw in ["购物车", "加购", "加到购物车", "加入购物车", "查看购物车", "清空购物车"]):
            from app.services import cart_service
            from app.core.database import AsyncSessionLocal
            sid = conversation_id or ""
            items = []
            total = 0.0
            try:
                async with AsyncSessionLocal() as db:
                    items = await cart_service.get_cart(db, sid)
                    total = await cart_service.get_cart_total(db, sid)
            except Exception:
                pass
            if items:
                item_list = "\n".join(f"• {i.title} x{i.quantity} ¥{i.price}" for i in items)
                text = f"购物车（{len(items)}件，合计¥{total:.0f}）：\n{item_list}\n\n输入\"清空购物车\"可清空，输入\"下单\"可结算。"
            else:
                text = "购物车是空的。说「把XX加到购物车」来添加商品。"
            event = TextDeltaEvent(content=text)
            yield {"event": "text_delta", "data": event.model_dump_json()}
            yield {"event": "done", "data": DoneEvent().model_dump_json()}
            return

        initial_state: AgentState = {
            "query": message,
            "session_id": conversation_id or "",
            "slots": state or {},
        }

        # 运行 graph
        final_state = await agent_graph.ainvoke(initial_state)

        # 流式输出文本 (逐字符模拟，实际可对接 LLM stream)
        response_text = final_state.get("response", "")
        if response_text:
            # 分块发送文本
            chunk_size = 20
            for i in range(0, len(response_text), chunk_size):
                chunk = response_text[i:i + chunk_size]
                event = TextDeltaEvent(content=chunk)
                yield {"event": "text_delta", "data": event.model_dump_json()}

        # 发送商品卡片
        cards = final_state.get("product_cards", [])
        if cards:
            event = ProductCardEvent(products=cards)
            yield {"event": "product_cards", "data": event.model_dump_json()}

        # ── 写入缓存 ──
        cache.set(message, response_text, cards)

        # 完成
        yield {"event": "done", "data": DoneEvent().model_dump_json()}

    except Exception as exc:
        logger.exception("Agent pipeline error")
        error = ErrorEvent(message=str(exc), code="AGENT_ERROR")
        yield {"event": "error", "data": error.model_dump_json()}
        yield {"event": "done", "data": DoneEvent().model_dump_json()}
