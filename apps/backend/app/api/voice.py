"""语音 API 端点 — 录音上传 + 豆包音频理解 + RAG 检索 SSE"""
import json
import logging
from fastapi import APIRouter, UploadFile, File, Form, HTTPException
from sse_starlette.sse import EventSourceResponse
from app.services import voice_service
from app.services.agent import generate_response
from app.services.state_manager import get_or_create_session, increment_message_count, save_message
from app.schemas.sse_events import ErrorEvent, DoneEvent

logger = logging.getLogger("voice_api")
router = APIRouter()
MAX_AUDIO_SIZE = 5 * 1024 * 1024

SUPPORTED_EXTENSIONS = {".wav": "wav", ".mp3": "mp3", ".m4a": "m4a", ".aac": "aac", ".mp4": "m4a", ".amr": "amr", ".pcm": "pcm", ".3gp": "aac"}


def detect_audio_format(filename: str) -> str:
    if not filename:
        return "m4a"
    lower = filename.lower()
    for ext, fmt in SUPPORTED_EXTENSIONS.items():
        if lower.endswith(ext):
            return fmt
    return "m4a"


@router.post("/voice/recognize")
async def recognize_voice(audio: UploadFile = File(..., description="录音文件")):
    if not audio.filename:
        raise HTTPException(status_code=400, detail="音频文件名为空")
    audio_bytes = await audio.read()
    if not audio_bytes or len(audio_bytes) < 100:
        raise HTTPException(status_code=400, detail="音频文件过小")
    if len(audio_bytes) > MAX_AUDIO_SIZE:
        raise HTTPException(status_code=400, detail="音频文件过大")
    audio_format = detect_audio_format(audio.filename)
    success, text = await voice_service.recognize_audio(audio_bytes, audio_format)
    if success:
        return {"success": True, "text": text}
    return {"success": False, "text": "", "error": text}


@router.post("/voice/chat")
async def voice_chat(
    audio: UploadFile = File(..., description="录音文件"),
    conversation_id: str = Form(""),
    cart_session_id: str = Form(""),
    user_id: str = Form(""),
):
    if not audio.filename:
        raise HTTPException(status_code=400, detail="音频文件名为空")
    audio_bytes = await audio.read()
    if not audio_bytes or len(audio_bytes) < 100:
        raise HTTPException(status_code=400, detail="音频文件过小")
    if len(audio_bytes) > MAX_AUDIO_SIZE:
        raise HTTPException(status_code=400, detail="音频文件过大")

    audio_format = detect_audio_format(audio.filename)
    success, text = await voice_service.recognize_audio(audio_bytes, audio_format)

    if not success or not text.strip():
        async def error_gen():
            yield {"event": "error", "data": ErrorEvent(message=text or "未识别到语音内容", code="VOICE_RECOGNIZE_FAILED").model_dump_json()}
            yield {"event": "done", "data": DoneEvent().model_dump_json()}
        return EventSourceResponse(error_gen())

    transcribed_text = text.strip()
    logger.info("语音转写成功，送入 RAG: %s", transcribed_text[:80])

    session_id, state = await get_or_create_session(conversation_id)
    runtime_state = dict(state or {})
    if cart_session_id:
        runtime_state["cart_session_id"] = cart_session_id
    if user_id:
        runtime_state["user_id"] = user_id

    display_message = f"🎤 {transcribed_text}"
    await save_message(session_id, "user", display_message, audio_data=audio_bytes)

    async def event_generator():
        response_text_parts = []
        try:
            async for event in generate_response(message=transcribed_text, conversation_id=session_id, state=runtime_state):
                try:
                    data = json.loads(event.get("data", "{}"))
                    if data.get("type") == "text_delta":
                        response_text_parts.append(data.get("content", ""))
                except (json.JSONDecodeError, TypeError):
                    pass
                yield event
            full_response = "".join(response_text_parts)
            await save_message(session_id, "assistant", full_response)
            await increment_message_count(session_id)
        except Exception as exc:
            logger.exception("Voice chat error")
            yield {"event": "error", "data": ErrorEvent(message="语音对话处理失败", code="VOICE_CHAT_ERROR").model_dump_json()}
            yield {"event": "done", "data": DoneEvent().model_dump_json()}

    return EventSourceResponse(event_generator())
