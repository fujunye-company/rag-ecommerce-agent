"""
对比 Schema — CompareRequest / CompareResult [全量预留]
"""
from pydantic import BaseModel


class CompareRequest(BaseModel):
    product_ids: list[str]  # 要对比的商品 ID 列表
    dimensions: list[str] | None = None  # 对比维度，如 ["价格","性能","便携性"]


class CompareDimension(BaseModel):
    name: str
    values: dict[str, str]  # product_id → value
    winner: str | None = None  # 该维度最优 product_id


class CompareResult(BaseModel):
    product_ids: list[str]
    dimensions: list[CompareDimension]
    summary: str  # LLM 生成的对比总结
