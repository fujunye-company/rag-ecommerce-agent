"""
Agent 编排 — LangGraph StateGraph 工作流
遵循开发规约 v2.0 §3.2: services/ 承担核心业务逻辑
"""
import json
from typing import TypedDict, AsyncGenerator
from langgraph.graph import StateGraph
from app.schemas.sse_events import TextDeltaEvent, ProductCardEvent, DoneEvent, ErrorEvent


class AgentState(TypedDict, total=False):
    """Agent 全局状态"""
    query: str
    session_id: str
    intent: str
    slots: dict
    retrieved_chunks: list
    ranked_products: list
    response: str
    product_cards: list


def build_agent_graph() -> StateGraph:
    """
    构建 LangGraph Agent 工作流:
    intent → retrieval → rank → generate → format
    """
    workflow = StateGraph(AgentState)
    # TODO: 添加节点和边
    return workflow


agent_graph = build_agent_graph()


async def generate_response(
    message: str,
    conversation_id: str | None = None,
) -> AsyncGenerator[dict, None]:
    """
    Agent 流式响应入口 — 被 api/chat.py 调用
    
    SSE 事件格式（与 schemas/sse_events.py 对齐）:
    - text_delta: 流式文本增量
    - product_cards: 推荐商品卡片
    - done: 流结束
    - error: 错误
    """
    try:
        # TODO: 接入完整 Agent 流程: intent → retrieval → rank → generate
        greeting = "你好！我是AI导购助手，请问有什么可以帮您？"
        event = TextDeltaEvent(content=greeting)
        yield {"event": "text_delta", "data": event.model_dump_json()}
        yield {"event": "done", "data": DoneEvent().model_dump_json()}
    except Exception as exc:
        error = ErrorEvent(message=str(exc), code="AGENT_ERROR")
        yield {"event": "error", "data": error.model_dump_json()}
        yield {"event": "done", "data": DoneEvent().model_dump_json()}
