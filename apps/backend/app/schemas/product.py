"""Product schemas — 请求体/响应体/卡片"""
from pydantic import BaseModel, Field
from uuid import UUID
from typing import Optional


class ProductBase(BaseModel):
    title: str = Field(..., min_length=1, max_length=256)
    description: str | None = None
    price: float = Field(..., ge=0)
    category: str = Field(..., min_length=1, max_length=128)
    brand: str | None = None


class ProductCreate(ProductBase):
    rating: float = 0.0
    image_urls: list[str] = []
    stock: int = 0
    sales: int = 0
    tags: list[str] = []
    attributes: dict | None = None
    highlights: list[str] | None = None
    scenarios: list[str] | None = None


class ProductUpdate(BaseModel):
    """所有字段可选，仅更新传入字段"""
    title: str | None = None
    description: str | None = None
    price: float | None = None
    category: str | None = None
    brand: str | None = None
    rating: float | None = None
    image_urls: list[str] | None = None
    stock: int | None = None
    sales: int | None = None
    tags: list[str] | None = None
    attributes: dict | None = None
    highlights: list[str] | None = None
    scenarios: list[str] | None = None


class ProductRead(ProductBase):
    id: UUID
    rating: float = 0
    image_urls: list[str] = []
    stock: int = 0
    sales: int = 0
    tags: list[str] = []
    attributes: dict | None = None
    highlights: list[str] | None = None
    scenarios: list[str] | None = None

    model_config = {"from_attributes": True}


class ProductCard(BaseModel):
    """轻量级列表卡片 — 用于 LazyRow 横向滚动"""
    id: UUID
    title: str
    price: float
    category: str
    brand: str | None = None
    rating: float = 0
    image_urls: list[str] = []
    highlights: list[str] | None = None
    match_score: float | None = None  # 排序匹配分（agent rank阶段填充）

    model_config = {"from_attributes": True}


class ProductFilter(BaseModel):
    """商品筛选参数"""
    category: str | None = None
    brand: str | None = None
    price_min: float | None = None
    price_max: float | None = None
    keyword: str | None = None
    sort_by: str | None = None  # price_asc / price_desc / rating / sales
    page: int = Field(default=1, ge=1)
    size: int = Field(default=20, ge=1, le=100)
