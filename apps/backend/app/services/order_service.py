"""订单服务 — 下单/查单/取消"""
import uuid
import hashlib
import logging
from typing import Optional
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession
from app.models.order import Order

logger = logging.getLogger("order_service")


def _generate_order_no(session_id: str) -> str:
    """生成唯一订单号：ORD + 时间戳 + hash 后8位"""
    import time
    ts = str(int(time.time()))
    digest = hashlib.md5((session_id + ts).encode()).hexdigest()[:8].upper()
    return f"ORD{ts[-6:]}{digest}"


async def create_order(
    db: AsyncSession,
    session_id: str,
    items: list[dict],
    total: float,
    address: str = "默认地址",
    remark: str = "",
) -> Order:
    """创建订单，返回持久化的 Order 对象"""
    order_no = _generate_order_no(session_id)
    order = Order(
        session_id=uuid.UUID(session_id),
        order_no=order_no,
        total=round(total, 2),
        address=address,
        remark=remark,
        items_snapshot=items,
        status="confirmed",
    )
    db.add(order)
    await db.flush()
    await db.refresh(order)
    logger.info("Order created: %s, total=%.2f, items=%d", order_no, total, len(items))
    return order


async def get_order(db: AsyncSession, order_id: str) -> Optional[Order]:
    """按 ID 查询订单"""
    try:
        uid = uuid.UUID(order_id)
    except (ValueError, AttributeError):
        return None
    result = await db.execute(select(Order).where(Order.id == uid))
    return result.scalar_one_or_none()


async def get_orders_by_session(db: AsyncSession, session_id: str) -> list[Order]:
    """按 session 查询所有订单"""
    try:
        sid = uuid.UUID(session_id)
    except (ValueError, AttributeError):
        return []
    result = await db.execute(
        select(Order).where(Order.session_id == sid).order_by(Order.created_at.desc())
    )
    return list(result.scalars().all())


async def cancel_order(db: AsyncSession, order_id: str) -> bool:
    """取消订单"""
    order = await get_order(db, order_id)
    if order is None:
        return False
    order.status = "cancelled"
    await db.flush()
    logger.info("Order cancelled: %s", order.order_no)
    return True
