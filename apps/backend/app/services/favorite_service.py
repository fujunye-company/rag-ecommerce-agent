"""
商品收藏 CRUD 服务 — 支持 user_id 维度隔离

每个用户独立管理收藏记录，收藏/取消收藏为幂等操作。
"""
import logging
from datetime import datetime
from sqlalchemy import select, delete, func
from sqlalchemy.ext.asyncio import AsyncSession
from app.models.favorite import Favorite

logger = logging.getLogger("favorite_service")


async def get_favorites(
    db: AsyncSession, user_id: str, offset: int = 0, limit: int = 50
) -> list[Favorite]:
    """获取用户的收藏商品列表，按收藏时间倒序排列。

    Args:
        user_id: 用户 ID
        offset: 分页偏移量
        limit: 每页数量，默认 50
    """
    stmt = (
        select(Favorite)
        .where(Favorite.user_id == user_id)
        .order_by(Favorite.created_at.desc())
        .offset(offset)
        .limit(limit)
    )
    result = await db.execute(stmt)
    return list(result.scalars().all())


async def get_favorite_count(db: AsyncSession, user_id: str) -> int:
    """获取用户收藏商品总数。

    Args:
        user_id: 用户 ID
    """
    stmt = select(func.count()).where(Favorite.user_id == user_id)
    result = await db.execute(stmt)
    return result.scalar() or 0


async def is_favorited(db: AsyncSession, user_id: str, product_id: str) -> bool:
    """检查用户是否已收藏指定商品。

    Args:
        user_id: 用户 ID
        product_id: 商品 ID
    """
    stmt = select(Favorite).where(
        Favorite.user_id == user_id,
        Favorite.product_id == product_id,
    )
    result = await db.execute(stmt)
    return result.scalar_one_or_none() is not None


async def toggle_favorite(
    db: AsyncSession, user_id: str, product_id: str
) -> dict:
    """切换收藏状态：已收藏则取消，未收藏则添加。

    Args:
        user_id: 用户 ID
        product_id: 商品 ID

    Returns:
        {"action": "added"|"removed", "favorited": bool}
    """
    stmt = select(Favorite).where(
        Favorite.user_id == user_id,
        Favorite.product_id == product_id,
    )
    result = await db.execute(stmt)
    existing = result.scalar_one_or_none()

    if existing:
        # 已收藏 → 取消收藏
        await db.execute(
            delete(Favorite).where(
                Favorite.user_id == user_id,
                Favorite.product_id == product_id,
            )
        )
        await db.flush()
        logger.info("Favorite: removed %s for user %s", product_id[:16], user_id[:12])
        return {"action": "removed", "favorited": False}
    else:
        # 未收藏 → 添加收藏
        fav = Favorite(user_id=user_id, product_id=product_id)
        db.add(fav)
        await db.flush()
        logger.info("Favorite: added %s for user %s", product_id[:16], user_id[:12])
        return {"action": "added", "favorited": True}


async def remove_favorites(
    db: AsyncSession, user_id: str, product_ids: list[str]
) -> int:
    """批量移除收藏商品。

    Args:
        user_id: 用户 ID
        product_ids: 要移除的商品 ID 列表

    Returns:
        实际删除的条数
    """
    if not product_ids:
        return 0
    stmt = delete(Favorite).where(
        Favorite.user_id == user_id,
        Favorite.product_id.in_(product_ids),
    )
    result = await db.execute(stmt)
    await db.flush()
    logger.info(
        "Favorite: batch removed %d items for user %s", result.rowcount, user_id[:12]
    )
    return result.rowcount
