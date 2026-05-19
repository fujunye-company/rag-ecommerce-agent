"""
商品对比 — 多商品多维度对比 Agent [全量预留]
"""
import logging

logger = logging.getLogger("comparator")


async def compare_products(
    product_ids: list[str],
    dimensions: list[str] | None = None,
) -> dict:
    """
    多商品横向对比
    全量: LLM 生成多维度对比表 + 总结
    返回: {dimensions: [...], summary: "..."}
    """
    return {"dimensions": [], "summary": "商品对比功能开发中"}
