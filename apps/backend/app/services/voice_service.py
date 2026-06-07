"""
语音识别服务 — 基于豆包音频理解 API (Chat API multimodal input_audio)。
"""
import base64
import logging
import httpx
from app.core.config import settings
from app.services.llm_client import chat_completion

logger = logging.getLogger("voice_service")

FORMAT_MAP = {
    "wav": {"mime": "audio/wav", "api_format": "wav"},
    "mp3": {"mime": "audio/mpeg", "api_format": "mp3"},
    "m4a": {"mime": "audio/mp4", "api_format": "m4a"},
    "aac": {"mime": "audio/aac", "api_format": "aac"},
    "mp4": {"mime": "audio/mp4", "api_format": "m4a"},
    "amr": {"mime": "audio/amr", "api_format": "amr"},
}
ARK_ASR_URL = "https://ark.cn-beijing.volces.com/api/v3/audio/transcriptions"


async def recognize_audio(audio_bytes: bytes, audio_format: str = "m4a") -> tuple[bool, str]:
    if not audio_bytes or len(audio_bytes) < 100:
        return False, "录音文件过小，请靠近麦克风清晰说话"
    fmt_info = FORMAT_MAP.get(audio_format.lower(), FORMAT_MAP["m4a"])
    logger.info("收到语音: %d bytes, format=%s", len(audio_bytes), audio_format)

    # 路径 1: multimodal input_audio (豆包音频理解)
    try:
        success, text = await _recognize_via_chat_api(audio_bytes, fmt_info)
        if success:
            return True, text
    except Exception as exc:
        logger.warning("Chat API audio 失败: %s，回退 whisper-1", exc)

    # 路径 2: whisper-1 回退
    try:
        return await _recognize_via_whisper(audio_bytes, fmt_info)
    except Exception as exc:
        logger.error("ASR 回退失败: %s", exc)
        return False, "语音识别服务异常，请稍后重试"


async def _recognize_via_chat_api(audio_bytes: bytes, fmt_info: dict) -> tuple[bool, str]:
    audio_b64 = base64.b64encode(audio_bytes).decode("ascii")
    messages = [
        {"role": "system", "content": "你是一个电商语音助手。请将用户的语音输入转换为文字。只返回识别出的文字内容本身，不要添加任何解释。保留商品品牌、型号、规格等关键信息。"},
        {"role": "user", "content": [
            {"type": "text", "text": "请识别这段语音，只返回文字："},
            {"type": "input_audio", "input_audio": {"data": audio_b64, "format": fmt_info["api_format"]}},
        ]},
    ]
    text = await chat_completion(messages=messages, temperature=0.1, max_tokens=512, stream=False)
    if text and text.strip():
        cleaned = text.strip().strip('"').strip("'").strip("。")
        logger.info("Chat API 音频理解成功: %s", cleaned[:80])
        return True, cleaned
    return False, "未识别到语音内容"


async def _recognize_via_whisper(audio_bytes: bytes, fmt_info: dict) -> tuple[bool, str]:
    ext = fmt_info["api_format"]
    async with httpx.AsyncClient(timeout=30.0) as client:
        resp = await client.post(
            ARK_ASR_URL,
            headers={"Authorization": f"Bearer {settings.DOUBAO_API_KEY}"},
            files={"file": (f"voice.{ext}", audio_bytes, fmt_info["mime"])},
            data={"model": "whisper-1", "language": "zh"},
        )
        if resp.status_code == 200:
            text = resp.json().get("text", "")
            if text.strip():
                return True, text.strip()
            return False, "未识别到语音内容"
        elif resp.status_code == 404:
            return False, "语音识别服务不可用"
        return False, "语音识别服务异常，请稍后重试"
