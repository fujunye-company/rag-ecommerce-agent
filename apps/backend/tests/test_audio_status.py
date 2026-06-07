"""Audio ASR readiness tests."""
import pytest


@pytest.mark.asyncio
async def test_asr_status_does_not_load_model(client):
    response = await client.get("/api/v1/audio/asr-status")

    assert response.status_code == 200
    data = response.json()
    assert {"provider", "model", "ready", "loaded"} <= set(data)
    assert data["provider"] == "faster_whisper"
    assert data["ready"] is True
