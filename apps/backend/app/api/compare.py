"""
商品对比接口 — POST /api/products/compare [全量预留]
遵循开发规约 v2.0 §3.2
"""
from fastapi import APIRouter
from app.schemas.compare import CompareRequest, CompareResult

router = APIRouter()


@router.post("/products/compare", response_model=CompareResult)
async def compare_products(body: CompareRequest):
    """多商品横向对比 (全量阶段实现)"""
    return CompareResult(
        product_ids=body.product_ids,
        dimensions=[],
        summary="商品对比功能开发中",
    )
