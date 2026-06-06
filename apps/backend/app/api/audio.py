"""Audio APIs for local speech input."""
import logging
from pathlib import Path

from fastapi import APIRouter, File, HTTPException, UploadFile

from app.schemas.common import ApiResponse
from app.services.audio_transcriber import get_asr_readiness, transcribe_audio

logger = logging.getLogger("audio")
router = APIRouter()


@router.get("/audio/asr-status")
async def asr_status():
    return get_asr_readiness()


@router.post("/audio/transcribe")
async def transcribe(file: UploadFile = File(...)):
    contents = await file.read()
    if len(contents) > 25 * 1024 * 1024:
        raise HTTPException(status_code=413, detail="Audio size exceeds 25MB")

    suffix = Path(file.filename or "voice.m4a").suffix or ".m4a"
    try:
        result = await transcribe_audio(contents, suffix=suffix)
    except Exception as exc:
        logger.error("ASR transcription failed: %s", exc)
        raise HTTPException(status_code=503, detail=f"Local ASR failed: {str(exc)[:120]}") from exc

    return ApiResponse(data=result, message="Transcription successful").model_dump()
