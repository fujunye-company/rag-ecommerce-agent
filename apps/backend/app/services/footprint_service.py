"""
商品足迹（浏览历史）CRUD 服务 — 支持 user_id 维度隔离

足迹记录逻辑：
- 库中不存在该商品记录 → 新增足迹
- 库中已存在该商品记录 → 更新浏览日期
- 浏览日期仅记录年月日（date 类型）
"""
import logging
from datetime import date, datetime
from sqlalchemy import select, delete, func, and_
from sqlalchemy.ext.asyncio import AsyncSession
from app.models.footprint import Footprint

logger = logging.getLogger("footprint_service")


async def record_footprint(
    db: AsyncSession, user_id: str, product_id: str
) -> dict:
    """记录商品浏览足迹。

    - 库中不存在该商品足迹 → 新增记录
    - 库中已存在该商品足迹 → 更新 browse_date 为当天日期

    Args:
        user_id: 用户 ID
        product_id: 商品 ID

    Returns:
        {"action": "created"|"updated"}
    """
    today = date.today()

    stmt = select(Footprint).where(
        Footprint.user_id == user_id,
        Footprint.product_id == product_id,
    )
    result = await db.execute(stmt)
    existing = result.scalar_one_or_none()

    if existing:
        # 已存在 → 更新浏览日期
        if existing.browse_date != today:
            existing.browse_date = today
            await db.flush()
            logger.info(
                "Footprint: updated date to %s for %s (user %s)",
                today, product_id[:16], user_id[:12],
            )
            return {"action": "updated", "date": str(today)}
        # 同一天不重复更新
        return {"action": "unchanged", "date": str(today)}
    else:
        # 不存在 → 新增足迹记录
        fp = Footprint(user_id=user_id, product_id=product_id, browse_date=today)
        db.add(fp)
        await db.flush()
        logger.info(
            "Footprint: created for %s (user %s, date %s)",
            product_id[:16], user_id[:12], today,
        )
        return {"action": "created", "date": str(today)}


async def get_footprints(
    db: AsyncSession,
    user_id: str,
    start_date: date | None = None,
    end_date: date | None = None,
    offset: int = 0,
    limit: int = 50,
) -> list[Footprint]:
    """获取用户的足迹记录，按浏览日期降序排列。

    Args:
        user_id: 用户 ID
        start_date: 筛选起始日期（含），可选
        end_date: 筛选结束日期（含），可选
        offset: 分页偏移量
        limit: 每页数量，默认 50
    """
    conditions = [Footprint.user_id == user_id]
    if start_date is not None:
        conditions.append(Footprint.browse_date >= start_date)
    if end_date is not None:
        conditions.append(Footprint.browse_date <= end_date)

    stmt = (
        select(Footprint)
        .where(and_(*conditions))
        .order_by(Footprint.browse_date.desc(), Footprint.created_at.desc())
        .offset(offset)
        .limit(limit)
    )
    result = await db.execute(stmt)
    return list(result.scalars().all())


async def get_footprint_count(
    db: AsyncSession,
    user_id: str,
    start_date: date | None = None,
    end_date: date | None = None,
) -> int:
    """获取用户足迹总数（可按日期范围筛选）。

    Args:
        user_id: 用户 ID
        start_date: 筛选起始日期（含），可选
        end_date: 筛选结束日期（含），可选
    """
    conditions = [Footprint.user_id == user_id]
    if start_date is not None:
        conditions.append(Footprint.browse_date >= start_date)
    if end_date is not None:
        conditions.append(Footprint.browse_date <= end_date)

    stmt = select(func.count()).where(and_(*conditions))
    result = await db.execute(stmt)
    return result.scalar() or 0


async def delete_footprints(
    db: AsyncSession, user_id: str, product_ids: list[str]
) -> int:
    """批量删除足迹记录。

    Args:
        user_id: 用户 ID
        product_ids: 要删除的商品 ID 列表

    Returns:
        实际删除的条数
    """
    if not product_ids:
        return 0
    stmt = delete(Footprint).where(
        Footprint.user_id == user_id,
        Footprint.product_id.in_(product_ids),
    )
    result = await db.execute(stmt)
    await db.flush()
    return result.rowcount
