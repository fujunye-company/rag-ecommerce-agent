"""
商品 CRUD — 数据库读写
"""
import logging
from uuid import UUID
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession
from app.models.product import Product

logger = logging.getLogger("product_service")


async def get_products(
    db: AsyncSession,
    category: str | None = None,
    brand: str | None = None,
    price_min: float | None = None,
    price_max: float | None = None,
    page: int = 1,
    size: int = 20,
) -> list[Product]:
    """分页查询商品列表"""
    stmt = select(Product)
    if category:
        stmt = stmt.where(Product.category == category)
    if brand:
        stmt = stmt.where(Product.brand == brand)
    if price_min is not None:
        stmt = stmt.where(Product.price >= price_min)
    if price_max is not None:
        stmt = stmt.where(Product.price <= price_max)
    stmt = stmt.offset((page - 1) * size).limit(size)
    result = await db.execute(stmt)
    return list(result.scalars().all())


async def get_product_by_id(db: AsyncSession, product_id: UUID) -> Product | None:
    """获取单个商品详情"""
    result = await db.execute(select(Product).where(Product.id == product_id))
    return result.scalar_one_or_none()
