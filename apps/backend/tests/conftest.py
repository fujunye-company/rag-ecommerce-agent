"""
pytest fixtures — async client, test DB, mock LLM
"""
import pytest
from httpx import ASGITransport, AsyncClient
from app.main import app


@pytest.fixture
async def async_client():
    """异步 HTTP 测试客户端"""
    transport = ASGITransport(app=app)
    async with AsyncClient(transport=transport, base_url="http://test") as client:
        yield client
