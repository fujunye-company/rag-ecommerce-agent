"""Product schemas — 对齐 DATA-CONTRACT.md v1.0 ProductRecord"""
from pydantic import BaseModel, Field
from typing import Optional


class ProductRecord(BaseModel):
    """全栈统一商品模型 — DATA-CONTRACT.md v1.0 §1.1"""
    # 标识
    product_id: str = Field(..., min_length=1, max_length=64, description="唯一商品ID")

    # 基本信息
    title: str = Field(..., min_length=1, max_length=256)
    brand: str | None = None
    category: str = Field(..., min_length=1, max_length=64)

    # 价格
    price: float = Field(..., ge=0)

    # 评价
    rating: float = Field(default=3.0, ge=0, le=5.0)
    rating_count: int = Field(default=0, ge=0)

    # 描述
    highlights: list[str] = []
    attributes: dict[str, str] = {}
    scenarios: list[str] = []

    # 多媒体
    image_url: str | None = None
    image_urls: list[str] = []

    # 来源
    source: str = ""


class ProductCreate(ProductRecord):
    """创建商品 — 继承 ProductRecord（product_id 必填）"""
    pass


class ProductUpdate(BaseModel):
    """更新商品 — 所有字段可选"""
    title: str | None = None
    brand: str | None = None
    category: str | None = None
    price: float | None = None
    rating: float | None = None
    rating_count: int | None = None
    highlights: list[str] | None = None
    attributes: dict[str, str] | None = None
    scenarios: list[str] | None = None
    image_url: str | None = None
    image_urls: list[str] | None = None
    source: str | None = None


class ProductRead(ProductRecord):
    """API 响应 — 继承 ProductRecord 全字段"""
    pass


class ProductCard(BaseModel):
    """轻量级列表卡片"""
    product_id: str
    title: str
    price: float
    category: str
    brand: str | None = None
    rating: float = 0
    image_url: str | None = None
    highlights: list[str] = []
    match_score: float | None = None


class ProductFilter(BaseModel):
    """商品筛选参数"""
    category: str | None = None
    brand: str | None = None
    price_min: float | None = None
    price_max: float | None = None
    keyword: str | None = None
    sort_by: str | None = None  # price_asc / price_desc / rating / rating_count
    page: int = Field(default=1, ge=1)
    size: int = Field(default=20, ge=1, le=100)
