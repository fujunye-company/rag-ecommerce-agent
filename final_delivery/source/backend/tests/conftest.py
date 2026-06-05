"""
pytest fixtures — async HTTP client for testing
"""
import pytest_asyncio
from httpx import AsyncClient, ASGITransport
from app.main import app


@pytest_asyncio.fixture
async def client():
    """异步 HTTP 测试客户端 — 使用 ASGITransport 直接调用 ASGI app"""
    transport = ASGITransport(app=app)
    async with AsyncClient(transport=transport, base_url="http://test") as ac:
        yield ac


@pytest_asyncio.fixture
async def db_check():
    """检查数据库是否可用。不可用时标记测试为 xfail。"""
    import pytest
    from app.core.database import engine
    from sqlalchemy import text
    try:
        async with engine.connect() as conn:
            await conn.execute(text("SELECT 1"))
        return True
    except Exception:
        pytest.skip("Database unavailable — running in degraded mode")
