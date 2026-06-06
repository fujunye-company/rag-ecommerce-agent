"""
商品评价记录表 ORM 模型

符合第三范式（3NF）：
- id 为主键（UUID）
- product_id 关联商品表，user_id 关联用户表
- 每个非主属性直接依赖于主键
- 无传递依赖
"""
import uuid
from datetime import datetime, date
from sqlalchemy import String, Integer, Text, Date, DateTime, LargeBinary, Boolean, func
from sqlalchemy.dialects.postgresql import UUID
from sqlalchemy.orm import Mapped, mapped_column
from app.core.database import Base


class Review(Base):
    """用户商品评价记录表"""
    __tablename__ = "product_reviews"

    # 主键：评价 ID（UUID）
    id: Mapped[uuid.UUID] = mapped_column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    # 商品 ID（关联 products 表）
    product_id: Mapped[str] = mapped_column(String(256), nullable=False, index=True)
    # 用户 ID（关联 users 表）
    user_id: Mapped[str] = mapped_column(String(64), nullable=False, index=True)
    # 用户昵称（评价时的快照，支持匿名评价时显示"匿名用户"）
    nickname: Mapped[str] = mapped_column(String(128), default="")
    # 评分（1-5 分）
    rating: Mapped[int] = mapped_column(Integer, nullable=False, default=5)
    # 评价内容（文字）
    content: Mapped[str] = mapped_column(Text, default="")
    # 图片/视频二进制数据（压缩后存储，最多 9 个文件合并为一个 BLOB）
    media: Mapped[bytes | None] = mapped_column(LargeBinary, nullable=True)
    # 评价日期（仅记录年月日）
    review_date: Mapped[date] = mapped_column(Date, server_default=func.current_date())
    # 是否匿名评价
    is_anonymous: Mapped[bool] = mapped_column(Boolean, default=False)
    # 创建时间
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), server_default=func.now())
