"""商品API端点"""
from fastapi import APIRouter, Depends, Query
from sqlalchemy.ext.asyncio import AsyncSession
from app.core.database import get_db

router = APIRouter()


@router.get("/products")
async def list_products(
    category: str | None = Query(None),
    min_price: float | None = Query(None),
    max_price: float | None = Query(None),
    page: int = Query(1, ge=1),
    page_size: int = Query(20, ge=1, le=100),
    db: AsyncSession = Depends(get_db),
):
    # TODO: 接入真实数据库查询
    return {"items": [], "total": 0, "page": page, "page_size": page_size}
