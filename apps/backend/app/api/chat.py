"""聊天SSE端点 — 纯路由层，多轮支持"""
import json
import logging
from sse_starlette.sse import EventSourceResponse
from fastapi import APIRouter
from app.schemas.chat import ChatRequest
from app.services.agent import generate_response
from app.services.state_manager import get_or_create_session, increment_message_count, save_message
from app.schemas.sse_events import ErrorEvent, DoneEvent

logger = logging.getLogger("chat_api")
router = APIRouter()


@router.post(
    "/chat",
    summary="AI 导购对话",
    description="""提交用户消息，返回 SSE 流式响应。

**SSE 事件类型**:
- `progress`: 处理进度通知 (message字段)
- `text_delta`: AI 文本回复片段 (content字段)
- `product_cards`: 商品卡片 (product_id, title, price, image_url等)
- `done`: 流结束 (latency_ms, total_cards)

**多轮对话**: 传入 `conversation_id` 保持上下文连续性。不传则自动创建新会话。""",
    responses={200: {"description": "SSE 事件流"}},
)
async def chat(request: ChatRequest):
    # 获取或创建会话
    session_id, state = await get_or_create_session(request.conversation_id)

    # 持久化用户消息
    await save_message(session_id, "user", request.message)

    async def event_generator():
        response_text_parts = []
        try:
            async for event in generate_response(
                message=request.message,
                conversation_id=session_id,
                state=state,
            ):
                # 收集 text_delta 内容用于持久化
                try:
                    data = json.loads(event.get("data", "{}"))
                    if data.get("type") == "text_delta":
                        response_text_parts.append(data.get("content", ""))
                except (json.JSONDecodeError, TypeError):
                    pass
                yield event
            # 持久化助手回复
            full_response = "".join(response_text_parts)
            await save_message(session_id, "assistant", full_response)
            # 消息计数 +1
            await increment_message_count(session_id)
        except Exception as exc:
            logger.exception("Chat endpoint error")
            error = ErrorEvent(message=str(exc), code="CHAT_ERROR")
            yield {"event": "error", "data": error.model_dump_json()}
            yield {"event": "done", "data": DoneEvent().model_dump_json()}

    return EventSourceResponse(event_generator())
