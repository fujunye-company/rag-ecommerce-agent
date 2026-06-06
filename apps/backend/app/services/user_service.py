"""
用户服务 — CRUD 操作
"""
import logging
from typing import Optional
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession
from app.models.user import User

logger = logging.getLogger("user_service")


async def get_user(db: AsyncSession, user_id: str) -> Optional[User]:
    """按 ID 查询用户"""
    result = await db.execute(select(User).where(User.id == user_id))
    return result.scalar_one_or_none()


async def create_or_update_user(
    db: AsyncSession,
    user_id: str,
    nickname: str = "",
    gender: str = "",
    age_range: str = "",
    budget_min: float = 0.0,
    budget_max: float = 99999.0,
    preferred_categories: str = "[]",
    is_guest: int = 1,
) -> User:
    """创建或更新用户画像（upsert 模式，保证前后端一致）"""
    existing = await get_user(db, user_id)
    if existing:
        # 更新已有用户
        if nickname:
            existing.nickname = nickname
        if gender:
            existing.gender = gender
        if age_range:
            existing.age_range = age_range
        existing.budget_min = budget_min
        existing.budget_max = budget_max
        if preferred_categories:
            existing.preferred_categories = preferred_categories
        existing.is_guest = is_guest
        await db.flush()
        await db.refresh(existing)
        logger.info("User updated: %s", user_id)
        return existing
    else:
        # 创建新用户
        user = User(
            id=user_id,
            nickname=nickname,
            gender=gender,
            age_range=age_range,
            budget_min=budget_min,
            budget_max=budget_max,
            preferred_categories=preferred_categories,
            is_guest=is_guest,
        )
        db.add(user)
        await db.flush()
        await db.refresh(user)
        logger.info("User created: %s", user_id)
        return user


async def update_user_fields(db: AsyncSession, user_id: str, **fields) -> bool:
    """按字段更新用户画像"""
    user = await get_user(db, user_id)
    if user is None:
        return False
    for key, value in fields.items():
        if hasattr(user, key) and value is not None:
            setattr(user, key, value)
    await db.flush()
    return True
