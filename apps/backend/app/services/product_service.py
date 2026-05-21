"""
商品 CRUD — 数据库读写服务
"""
import logging
from uuid import UUID
from sqlalchemy import select, func, delete
from sqlalchemy.ext.asyncio import AsyncSession
from app.models.product import Product

logger = logging.getLogger("product_service")


async def get_products(
    db: AsyncSession,
    category: str | None = None,
    brand: str | None = None,
    price_min: float | None = None,
    price_max: float | None = None,
    keyword: str | None = None,
    sort_by: str | None = None,
    page: int = 1,
    size: int = 20,
) -> tuple[list[Product], int]:
    """分页查询商品列表，返回 (商品列表, 总数)"""
    stmt = select(Product)
    count_stmt = select(func.count(Product.id))

    if category:
        stmt = stmt.where(Product.category == category)
        count_stmt = count_stmt.where(Product.category == category)
    if brand:
        stmt = stmt.where(Product.brand == brand)
        count_stmt = count_stmt.where(Product.brand == brand)
    if price_min is not None:
        stmt = stmt.where(Product.price >= price_min)
        count_stmt = count_stmt.where(Product.price >= price_min)
    if price_max is not None:
        stmt = stmt.where(Product.price <= price_max)
        count_stmt = count_stmt.where(Product.price <= price_max)
    if keyword:
        stmt = stmt.where(Product.title.ilike(f"%{keyword}%"))
        count_stmt = count_stmt.where(Product.title.ilike(f"%{keyword}%"))

    # 排序
    sort_map = {
        "price_asc": Product.price.asc(),
        "price_desc": Product.price.desc(),
        "rating": Product.rating.desc(),
        "sales": Product.sales.desc(),
    }
    if sort_by and sort_by in sort_map:
        stmt = stmt.order_by(sort_map[sort_by])
    else:
        stmt = stmt.order_by(Product.sales.desc())

    stmt = stmt.offset((page - 1) * size).limit(size)

    result = await db.execute(stmt)
    products = list(result.scalars().all())

    total_result = await db.execute(count_stmt)
    total = total_result.scalar()

    return products, total


async def get_product_by_id(db: AsyncSession, product_id: UUID) -> Product | None:
    """获取单个商品详情"""
    result = await db.execute(select(Product).where(Product.id == product_id))
    return result.scalar_one_or_none()


async def create_product(db: AsyncSession, data: dict) -> Product:
    """创建商品"""
    product = Product(**data)
    db.add(product)
    await db.flush()
    await db.refresh(product)
    logger.info(f"Created product: {product.id} — {product.title}")
    return product


async def update_product(db: AsyncSession, product_id: UUID, data: dict) -> Product | None:
    """更新商品（仅更新非 None 字段）"""
    product = await get_product_by_id(db, product_id)
    if not product:
        return None
    for key, value in data.items():
        if value is not None:
            setattr(product, key, value)
    await db.flush()
    await db.refresh(product)
    logger.info(f"Updated product: {product_id}")
    return product


async def delete_product(db: AsyncSession, product_id: UUID) -> bool:
    """删除商品，返回是否成功"""
    result = await db.execute(delete(Product).where(Product.id == product_id))
    await db.flush()
    deleted = result.rowcount > 0
    if deleted:
        logger.info(f"Deleted product: {product_id}")
    return deleted
