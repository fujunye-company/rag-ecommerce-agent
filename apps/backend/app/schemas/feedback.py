"""
反馈 Schema — FeedbackCreate / FeedbackRead
"""
from pydantic import BaseModel
from uuid import UUID


class FeedbackCreate(BaseModel):
    session_id: str
    product_id: str | None = None
    rating: int  # 1=赞, -1=踩
    reason: str | None = None  # "不准确" / "不相关" / 其他


class FeedbackRead(BaseModel):
    id: UUID
    session_id: str
    product_id: str | None = None
    rating: int
    reason: str | None = None

    model_config = {"from_attributes": True}
