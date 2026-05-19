"""聊天SSE端点 — 纯路由层，不包含业务逻辑"""
from sse_starlette.sse import EventSourceResponse
from fastapi import APIRouter
from app.schemas.chat import ChatRequest
from app.services.agent import generate_response

router = APIRouter()


@router.post("/chat")
async def chat(request: ChatRequest):
    async def event_generator():
        # 委托给 Agent 服务层生成流式响应
        async for event in generate_response(request.message, request.conversation_id):
            yield event

    return EventSourceResponse(event_generator())
