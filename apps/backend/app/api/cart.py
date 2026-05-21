"""购物车 API 端点"""
import uuid
from fastapi import APIRouter, Depends, Query
from sqlalchemy.ext.asyncio import AsyncSession
from pydantic import BaseModel
from app.core.database import get_db
from app.services import cart_service

router = APIRouter()


class CartAddRequest(BaseModel):
    session_id: str
    product_id: str
    title: str
    price: float


class CartRemoveRequest(BaseModel):
    session_id: str
    product_id: str


class CartQuantityRequest(BaseModel):
    session_id: str
    product_id: str
    quantity: int


@router.get("/cart")
async def get_cart(
    session_id: str = Query(...),
    db: AsyncSession = Depends(get_db),
):
    """获取购物车"""
    items = await cart_service.get_cart(db, session_id)
    total = await cart_service.get_cart_total(db, session_id)
    return {
        "items": [
            {
                "id": str(i.id), "product_id": str(i.product_id),
                "title": i.title, "price": i.price, "quantity": i.quantity
            }
            for i in items
        ],
        "total": round(total, 2),
        "count": len(items),
    }


@router.post("/cart/items")
async def add_item(body: CartAddRequest, db: AsyncSession = Depends(get_db)):
    """添加商品到购物车"""
    item = await cart_service.add_to_cart(
        db, body.session_id, body.product_id, body.title, body.price
    )
    return {"id": str(item.id), "quantity": item.quantity}


@router.delete("/cart/items")
async def remove_item(body: CartRemoveRequest, db: AsyncSession = Depends(get_db)):
    """删除购物车商品"""
    ok = await cart_service.remove_from_cart(db, body.session_id, body.product_id)
    return {"deleted": ok}


@router.put("/cart/items")
async def update_quantity(body: CartQuantityRequest, db: AsyncSession = Depends(get_db)):
    """修改商品数量"""
    ok = await cart_service.update_quantity(db, body.session_id, body.product_id, body.quantity)
    return {"updated": ok}


@router.delete("/cart")
async def clear_cart(
    session_id: str = Query(...),
    db: AsyncSession = Depends(get_db),
):
    """清空购物车"""
    await cart_service.clear_cart(db, session_id)
    return {"cleared": True}
