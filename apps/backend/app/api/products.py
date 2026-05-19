"""商品API端点 — 纯路由层，委托给 services/product_service"""
from uuid import UUID
from fastapi import APIRouter, Depends, Query
from sqlalchemy.ext.asyncio import AsyncSession
from app.core.database import get_db
from app.schemas.product import ProductRead
from app.schemas.common import ApiResponse, PaginatedResponse
from app.services.product_service import get_products, get_product_by_id

router = APIRouter()


@router.get("/products")
async def list_products(
    category: str | None = Query(None),
    brand: str | None = Query(None),
    price_min: float | None = Query(None),
    price_max: float | None = Query(None),
    page: int = Query(1, ge=1),
    size: int = Query(20, ge=1, le=100),
    db: AsyncSession = Depends(get_db),
):
    """商品列表 — 支持按品类/品牌/价格筛选 + 分页"""
    products = await get_products(
        db=db, category=category, brand=brand,
        price_min=price_min, price_max=price_max,
        page=page, size=size,
    )
    items = [ProductRead.model_validate(p).model_dump(mode="json") for p in products]
    return ApiResponse(
        data=PaginatedResponse(items=items, total=len(items), page=page, size=size)
    ).model_dump()


@router.get("/products/{product_id}")
async def get_product_detail(
    product_id: UUID,
    db: AsyncSession = Depends(get_db),
):
    """商品详情"""
    product = await get_product_by_id(db, product_id)
    if not product:
        from app.core.exceptions import NotFoundError
        raise NotFoundError(message=f"商品不存在: {product_id}")
    return ApiResponse(data=ProductRead.model_validate(product).model_dump(mode="json")).model_dump()
