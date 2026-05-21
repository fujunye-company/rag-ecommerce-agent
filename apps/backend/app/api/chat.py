"""聊天SSE端点 — 纯路由层，多轮支持"""
import logging
from sse_starlette.sse import EventSourceResponse
from fastapi import APIRouter
from app.schemas.chat import ChatRequest
from app.services.agent import generate_response
from app.services.state_manager import get_or_create_session, increment_message_count
from app.schemas.sse_events import ErrorEvent, DoneEvent

logger = logging.getLogger("chat_api")
router = APIRouter()


@router.post("/chat")
async def chat(request: ChatRequest):
    # 获取或创建会话
    session_id, state = await get_or_create_session(request.conversation_id)

    async def event_generator():
        try:
            async for event in generate_response(
                message=request.message,
                conversation_id=session_id,
                state=state,
            ):
                yield event
            # 消息计数 +1
            await increment_message_count(session_id)
        except Exception as exc:
            logger.exception("Chat endpoint error")
            error = ErrorEvent(message=str(exc), code="CHAT_ERROR")
            yield {"event": "error", "data": error.model_dump_json()}
            yield {"event": "done", "data": DoneEvent().model_dump_json()}

    return EventSourceResponse(event_generator())
