"""Local speech-to-text service based on faster-whisper."""
import asyncio
import logging
import tempfile
from pathlib import Path

from app.core.config import settings

logger = logging.getLogger("audio_transcriber")

_model = None


def _get_model():
    global _model
    if _model is None:
        from faster_whisper import WhisperModel

        logger.info(
            "Loading ASR model %s on %s/%s",
            settings.ASR_MODEL,
            settings.ASR_DEVICE,
            settings.ASR_COMPUTE_TYPE,
        )
        _model = WhisperModel(
            settings.ASR_MODEL,
            device=settings.ASR_DEVICE,
            compute_type=settings.ASR_COMPUTE_TYPE,
            download_root=str(Path(__file__).resolve().parents[2] / "data" / "models"),
        )
    return _model


def get_asr_readiness() -> dict:
    local_path = Path(settings.ASR_MODEL)
    return {
        "provider": "faster_whisper",
        "model": settings.ASR_MODEL,
        "local_model_exists": local_path.is_dir(),
        "loaded": _model is not None,
        "ready": True,
    }


def _transcribe_file(path: str) -> dict:
    model = _get_model()
    segments, info = model.transcribe(
        path,
        language="zh",
        beam_size=5,
        vad_filter=True,
        vad_parameters={"min_silence_duration_ms": 500},
    )
    text = "".join(segment.text.strip() for segment in segments).strip()
    return {
        "text": text,
        "language": info.language,
        "language_probability": round(float(info.language_probability), 4),
        "duration": round(float(info.duration or 0), 2),
    }


async def transcribe_audio(audio_bytes: bytes, suffix: str = ".m4a") -> dict:
    if not audio_bytes:
        raise RuntimeError("Audio file is empty")

    suffix = suffix if suffix.startswith(".") else f".{suffix}"
    with tempfile.NamedTemporaryFile(delete=False, suffix=suffix) as tmp:
        tmp.write(audio_bytes)
        tmp_path = tmp.name

    try:
        return await asyncio.to_thread(_transcribe_file, tmp_path)
    finally:
        try:
            Path(tmp_path).unlink(missing_ok=True)
        except Exception:
            logger.warning("Failed to delete temp audio file: %s", tmp_path)
