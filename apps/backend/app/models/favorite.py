"""
商品收藏记录表 ORM 模型

符合第三范式（3NF）：
- 主键 (user_id, product_id) 复合主键，确保同一用户不重复收藏同一商品
- user_id 关联用户表，product_id 关联商品表
"""
import uuid
from datetime import datetime
from sqlalchemy import String, DateTime, func, PrimaryKeyConstraint
from sqlalchemy.orm import Mapped, mapped_column
from app.core.database import Base


class Favorite(Base):
    """用户商品收藏记录 — 记录 user_id + product_id 映射"""
    __tablename__ = "favorites"

    user_id: Mapped[str] = mapped_column(String(64), nullable=False, index=True)
    product_id: Mapped[str] = mapped_column(String(256), nullable=False)
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), server_default=func.now())

    __table_args__ = (
        PrimaryKeyConstraint("user_id", "product_id", name="pk_favorites_user_product"),
    )
