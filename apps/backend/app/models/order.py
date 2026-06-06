"""订单表 ORM 模型"""
import uuid
from datetime import datetime
from sqlalchemy import String, Integer, Float, DateTime, func, Text
from sqlalchemy.dialects.postgresql import UUID, JSONB
from sqlalchemy.orm import Mapped, mapped_column
from app.core.database import Base


class Order(Base):
    __tablename__ = "orders"

    id: Mapped[uuid.UUID] = mapped_column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    order_no: Mapped[str] = mapped_column(String(32), nullable=False, unique=True, index=True)
    session_id: Mapped[uuid.UUID] = mapped_column(UUID(as_uuid=True), nullable=False, index=True)
    status: Mapped[str] = mapped_column(String(20), default="pending_payment")  # pending_payment/pending_shipping/pending_receipt/pending_review/completed/cancelled
    total: Mapped[float] = mapped_column(Float, nullable=False, default=0)
    address: Mapped[str] = mapped_column(String(512), default="默认地址")
    remark: Mapped[str] = mapped_column(Text, default="")
    items_snapshot: Mapped[dict] = mapped_column(JSONB, default=list)  # 下单时的商品快照
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), server_default=func.now())
    updated_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), server_default=func.now(), onupdate=func.now())
