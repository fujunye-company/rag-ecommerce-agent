"""
商品表 ORM 模型
"""
import uuid
from sqlalchemy import String, Float, Integer, Text, ARRAY
from sqlalchemy.dialects.postgresql import UUID
from sqlalchemy.orm import Mapped, mapped_column
from app.core.database import Base


class Product(Base):
    __tablename__ = "products"

    id: Mapped[uuid.UUID] = mapped_column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    title: Mapped[str] = mapped_column(String(256), nullable=False, index=True)
    description: Mapped[str | None] = mapped_column(Text, nullable=True)
    price: Mapped[float] = mapped_column(Float, nullable=False)
    category: Mapped[str] = mapped_column(String(128), nullable=False, index=True)
    brand: Mapped[str | None] = mapped_column(String(128), nullable=True)
    rating: Mapped[float] = mapped_column(Float, default=0)
    image_urls: Mapped[list[str] | None] = mapped_column(ARRAY(String), default=list)
    stock: Mapped[int] = mapped_column(Integer, default=0)
    sales: Mapped[int] = mapped_column(Integer, default=0)
    tags: Mapped[list[str] | None] = mapped_column(ARRAY(String), default=list)
