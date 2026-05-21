"""
购物车 CRUD 服务
"""
import uuid
import logging
from sqlalchemy import select, delete, func
from sqlalchemy.ext.asyncio import AsyncSession
from app.models.cart import CartItem

logger = logging.getLogger("cart_service")


async def get_cart(db: AsyncSession, session_id: str) -> list[CartItem]:
    """获取购物车商品列表"""
    sid = uuid.UUID(session_id)
    result = await db.execute(
        select(CartItem).where(CartItem.session_id == sid).order_by(CartItem.created_at)
    )
    return list(result.scalars().all())


async def add_to_cart(
    db: AsyncSession, session_id: str,
    product_id: str, title: str, price: float
) -> CartItem:
    """添加商品到购物车（已存在则数量+1）"""
    sid = uuid.UUID(session_id)
    pid = uuid.UUID(product_id)

    # 检查是否已存在
    result = await db.execute(
        select(CartItem).where(
            CartItem.session_id == sid,
            CartItem.product_id == pid
        )
    )
    existing = result.scalar_one_or_none()

    if existing:
        existing.quantity += 1
        await db.flush()
        logger.info("Cart: %s quantity=%d", product_id[:8], existing.quantity)
        return existing

    item = CartItem(session_id=sid, product_id=pid, title=title, price=price, quantity=1)
    db.add(item)
    await db.flush()
    logger.info("Cart: added %s", title[:30])
    return item


async def remove_from_cart(db: AsyncSession, session_id: str, product_id: str) -> bool:
    """从购物车删除商品"""
    sid = uuid.UUID(session_id)
    pid = uuid.UUID(product_id)
    result = await db.execute(
        delete(CartItem).where(CartItem.session_id == sid, CartItem.product_id == pid)
    )
    await db.flush()
    return result.rowcount > 0


async def update_quantity(db: AsyncSession, session_id: str, product_id: str, quantity: int) -> bool:
    """修改购物车商品数量"""
    sid = uuid.UUID(session_id)
    pid = uuid.UUID(product_id)
    result = await db.execute(
        select(CartItem).where(CartItem.session_id == sid, CartItem.product_id == pid)
    )
    item = result.scalar_one_or_none()
    if not item:
        return False
    item.quantity = max(0, quantity)
    await db.flush()
    return True


async def clear_cart(db: AsyncSession, session_id: str):
    """清空购物车"""
    sid = uuid.UUID(session_id)
    await db.execute(delete(CartItem).where(CartItem.session_id == sid))
    await db.flush()


async def get_cart_total(db: AsyncSession, session_id: str) -> float:
    """购物车总金额"""
    sid = uuid.UUID(session_id)
    result = await db.execute(
        select(func.sum(CartItem.price * CartItem.quantity))
        .where(CartItem.session_id == sid)
    )
    return result.scalar() or 0.0
