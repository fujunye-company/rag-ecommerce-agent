"""
用户表 ORM 模型

符合第三范式（3NF）：
- id 为主键（UUID 字符串，对应前端 SQLite user_profile.id）
- 字段与前段 SQLite user_profile 表完全一致
- 不包含传递依赖，每个非主属性直接依赖于主键
"""
from datetime import datetime
from sqlalchemy import String, Float, Integer, DateTime, Text, LargeBinary, func
from sqlalchemy.orm import Mapped, mapped_column
from app.core.database import Base


class User(Base):
    """用户画像表 — 同步前端 SQLite user_profile 表字段"""
    __tablename__ = "users"

    # 主键：UUID 字符串（"sw" 前缀），对应前端 user_profile.id
    id: Mapped[str] = mapped_column(String(64), primary_key=True)
    # 昵称
    nickname: Mapped[str] = mapped_column(String(128), default="")
    # 性别：male / female / ""
    gender: Mapped[str] = mapped_column(String(16), default="")
    # 年龄段："0-9" / "10-19" / ... / "100+"
    age_range: Mapped[str] = mapped_column(String(16), default="")
    # 预算下限
    budget_min: Mapped[float] = mapped_column(Float, default=0.0)
    # 预算上限
    budget_max: Mapped[float] = mapped_column(Float, default=99999.0)
    # 偏好品类 JSON 数组字符串
    preferred_categories: Mapped[str] = mapped_column(Text, default="[]")
    # 头像 BLOB（JPEG 字节，最大 480px 压缩后）
    avatar: Mapped[bytes | None] = mapped_column(LargeBinary, nullable=True)
    # 是否为游客：0=非游客（已登录），1=游客
    is_guest: Mapped[int] = mapped_column(Integer, default=1)
    # 创建时间
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), server_default=func.now())
    # 更新时间
    updated_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), server_default=func.now(), onupdate=func.now()
    )
