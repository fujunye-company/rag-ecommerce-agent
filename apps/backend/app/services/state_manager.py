"""
多轮状态管理 — 数据库持久化 + 内存缓存 (无数据库时纯内存模式)
"""
import logging
import uuid
from datetime import datetime
from sqlalchemy import select, update
from sqlalchemy.ext.asyncio import AsyncSession
from app.core.database import AsyncSessionLocal
from app.models.session import Session

logger = logging.getLogger("state_manager")

# 内存缓存 (减少 DB 查询)
_cache: dict[str, dict] = {}

# 检测数据库是否可用
_HAS_DB = AsyncSessionLocal is not None
if not _HAS_DB:
    logger.info("state_manager: 数据库不可用，使用纯内存模式")


async def _get_db():
    """获取数据库会话上下文管理器 (无DB时返回空上下文)"""
    if _HAS_DB:
        return AsyncSessionLocal()
    else:
        from contextlib import asynccontextmanager
        @asynccontextmanager
        async def _null():
            yield None
        return _null()


async def get_or_create_session(session_id: str | None = None) -> tuple[str, dict]:
    """获取或创建会话，返回 (session_id, state)"""
    # 验证 session_id 是否为有效 UUID
    valid_uuid = None
    if session_id:
        try:
            valid_uuid = uuid.UUID(session_id)
        except (ValueError, AttributeError):
            valid_uuid = None

    if valid_uuid:
        session_str = str(valid_uuid)
        # 尝试从缓存获取
        if session_str in _cache:
            return session_str, _cache[session_str]

        # 从数据库获取 (仅当有DB时)
        if _HAS_DB:
            async with AsyncSessionLocal() as db:
                result = await db.execute(select(Session).where(Session.id == valid_uuid))
                session = result.scalar_one_or_none()
                if session:
                    state = session.state_json or {}
                    _cache[str(session.id)] = state
                    return str(session.id), state

    # 创建新会话（优先使用传入的有效 UUID）
    new_id = valid_uuid if valid_uuid else uuid.uuid4()
    new_id_str = str(new_id)

    if _HAS_DB:
        async with AsyncSessionLocal() as db:
            session = Session(id=new_id, state_json={}, message_count=0)
            db.add(session)
            await db.commit()

    _cache[new_id_str] = {}
    logger.info("Created session: %s (db=%s)", new_id_str[:8], _HAS_DB)
    return new_id_str, {}


async def update_state(session_id: str, **kwargs) -> dict:
    """更新会话状态 (合并写入，自动持久化)"""
    state = _cache.get(session_id, {})
    state.update(kwargs)

    # 缓存更新
    _cache[session_id] = state

    # 异步持久化 (仅当有DB时)
    if _HAS_DB:
        try:
            async with AsyncSessionLocal() as db:
                await db.execute(
                    update(Session)
                    .where(Session.id == session_id)
                    .values(
                        state_json=state,
                        updated_at=datetime.utcnow(),
                    )
                )
                await db.commit()
        except Exception as e:
            logger.warning("Failed to persist state for %s: %s", session_id[:8], e)

    logger.debug("Session %s state: %s", session_id[:8], list(kwargs.keys()))
    return state


async def increment_message_count(session_id: str):
    """增加消息计数"""
    if not _HAS_DB:
        return
    try:
        async with AsyncSessionLocal() as db:
            await db.execute(
                update(Session)
                .where(Session.id == session_id)
                .values(message_count=Session.message_count + 1)
            )
            await db.commit()
    except Exception as e:
        logger.warning("Failed to increment msg count: %s", e)


async def get_state(session_id: str) -> dict:
    """获取会话状态（优先缓存）"""
    if session_id in _cache:
        return _cache[session_id]
    _, state = await get_or_create_session(session_id)
    return state


async def clear_state(session_id: str) -> None:
    """清除会话状态"""
    _cache.pop(session_id, None)
    if not _HAS_DB:
        return
    try:
        async with AsyncSessionLocal() as db:
            await db.execute(update(Session).where(Session.id == session_id).values(is_active=False))
            await db.commit()
    except Exception as e:
        logger.warning("Failed to deactivate session: %s", e)
