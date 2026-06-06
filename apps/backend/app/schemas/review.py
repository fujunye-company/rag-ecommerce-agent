"""
商品评价相关 Pydantic 模型 — 请求/响应校验
"""
from pydantic import BaseModel, Field


class ReviewCreate(BaseModel):
    """创建评价请求体"""
    product_id: str = Field(..., description="商品 ID")
    user_id: str = Field(..., description="用户 ID")
    nickname: str = Field(default="", description="用户昵称")
    rating: int = Field(default=5, ge=1, le=5, description="评分（1-5 分）")
    content: str = Field(default="", description="评价内容（文字）")
    is_anonymous: bool = Field(default=False, description="是否匿名评价")


class ReviewRead(BaseModel):
    """评价响应体"""
    id: str
    product_id: str
    user_id: str
    nickname: str
    rating: int
    content: str
    has_media: bool = False
    review_date: str | None = None
    is_anonymous: bool = False
    created_at: str | None = None


class ReviewListResponse(BaseModel):
    """评价列表响应"""
    reviews: list[ReviewRead] = []
    total: int = 0
    average_rating: float = 0.0
