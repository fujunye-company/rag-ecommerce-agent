"""
反馈表 ORM 模型
"""
import uuid
from datetime import datetime
from sqlalchemy import String, Integer, DateTime, func, ForeignKey
from sqlalchemy.dialects.postgresql import UUID
from sqlalchemy.orm import Mapped, mapped_column
from app.core.database import Base


class Feedback(Base):
    __tablename__ = "feedbacks"

    id: Mapped[uuid.UUID] = mapped_column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    session_id: Mapped[str] = mapped_column(String(64), nullable=False, index=True)
    product_id: Mapped[str | None] = mapped_column(String(64), nullable=True)
    rating: Mapped[int] = mapped_column(Integer, nullable=False)  # 1 = 赞, -1 = 踩
    reason: Mapped[str | None] = mapped_column(String(256), nullable=True)
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), server_default=func.now())
