"""聊天SSE端点"""
from sse_starlette.sse import EventSourceResponse
from fastapi import APIRouter
from app.schemas.chat import ChatRequest, SSEToken

router = APIRouter()


@router.post("/chat")
async def chat(request: ChatRequest):
    async def event_generator():
        # TODO: 接入LangGraph Agent流式输出
        yield {"event": "token", "data": SSEToken(type="text", content="你好！我是AI导购助手。").model_dump_json()}
        yield {"event": "done", "data": "{}"}

    return EventSourceResponse(event_generator())
