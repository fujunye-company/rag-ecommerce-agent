"""
商品足迹（浏览历史）记录表 ORM 模型

符合第三范式（3NF）：
- 主键 (user_id, product_id) 复合主键，确保同一用户同一商品只有一条足迹
- browse_date 仅记录年月日，符合需求规格
- user_id 关联用户表，product_id 关联商品表
"""
import uuid
from datetime import date, datetime
from sqlalchemy import String, Date, DateTime, func, PrimaryKeyConstraint
from sqlalchemy.orm import Mapped, mapped_column
from app.core.database import Base


class Footprint(Base):
    """用户商品足迹记录 — 记录 user_id + product_id + browse_date（仅年月日）"""
    __tablename__ = "footprints"

    user_id: Mapped[str] = mapped_column(String(64), nullable=False, index=True)
    product_id: Mapped[str] = mapped_column(String(256), nullable=False)
    # 浏览日期（仅年月日），符合需求规格
    browse_date: Mapped[date] = mapped_column(Date, nullable=False, index=True)
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), server_default=func.now())

    __table_args__ = (
        PrimaryKeyConstraint("user_id", "product_id", name="pk_footprints_user_product"),
    )
