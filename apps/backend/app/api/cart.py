"""购物车 API 端点"""
import uuid
from fastapi import APIRouter, Depends, Query, HTTPException
from sqlalchemy.ext.asyncio import AsyncSession
from pydantic import BaseModel
from app.core.database import get_db
from app.services import cart_service

router = APIRouter()


def _validate_uuid(value: str, name: str = "session_id") -> uuid.UUID:
    """校验 UUID 格式，无效时返回 400"""
    try:
        return uuid.UUID(value)
    except (ValueError, AttributeError):
        raise HTTPException(status_code=400, detail=f"无效的 {name} 格式: {value}")


class CartAddRequest(BaseModel):
    session_id: str
    product_id: str
    title: str = ""     # 客户端标识用，服务端忽略
    price: float = 0    # 客户端标识用，服务端忽略（防伪造）


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
    _validate_uuid(session_id)
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
    """添加商品到购物车 — 服务端查商品表取真实价格"""
    _validate_uuid(body.session_id)
    _validate_uuid(body.product_id, "product_id")

    # 服务端查商品表，不信任客户端传入的 price/title
    from app.services import product_service
    import uuid as _uuid
    product = await product_service.get_product_by_id(db, _uuid.UUID(body.product_id))
    if product is None:
        raise HTTPException(status_code=404, detail="商品不存在")
    title = product.title
    price = product.price

    item = await cart_service.add_to_cart(
        db, body.session_id, body.product_id, title, price
    )
    return {"id": str(item.id), "quantity": item.quantity}


@router.delete("/cart/items")
async def remove_item(body: CartRemoveRequest, db: AsyncSession = Depends(get_db)):
    """删除购物车商品"""
    _validate_uuid(body.session_id)
    _validate_uuid(body.product_id, "product_id")
    ok = await cart_service.remove_from_cart(db, body.session_id, body.product_id)
    return {"deleted": ok}


@router.put("/cart/items")
async def update_quantity(body: CartQuantityRequest, db: AsyncSession = Depends(get_db)):
    """修改商品数量"""
    _validate_uuid(body.session_id)
    _validate_uuid(body.product_id, "product_id")
    ok = await cart_service.update_quantity(db, body.session_id, body.product_id, body.quantity)
    return {"updated": ok}


@router.delete("/cart")
async def clear_cart(
    session_id: str = Query(...),
    db: AsyncSession = Depends(get_db),
):
    """清空购物车"""
    _validate_uuid(session_id)
    await cart_service.clear_cart(db, session_id)
    return {"cleared": True}
