"""SQLAlchemy ORM模型"""
from app.core.database import Base
from sqlalchemy import String, Numeric, Boolean, ForeignKey, text, SmallInteger
from sqlalchemy.orm import Mapped, mapped_column, relationship
from sqlalchemy.dialects.postgresql import UUID, JSONB, ARRAY
from pgvector.sqlalchemy import Vector
import uuid
from datetime import datetime


class Product(Base):
    __tablename__ = "products"

    id: Mapped[uuid.UUID] = mapped_column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    title: Mapped[str] = mapped_column(String(500), nullable=False)
    description: Mapped[str | None] = mapped_column(String(5000))
    price: Mapped[float] = mapped_column(Numeric(12, 2), nullable=False)
    category: Mapped[str] = mapped_column(String(100), nullable=False, index=True)
    brand: Mapped[str | None] = mapped_column(String(200))
    specifications: Mapped[dict] = mapped_column(JSONB, default={})
    image_urls: Mapped[list] = mapped_column(ARRAY(String), default=[])
    rating: Mapped[float] = mapped_column(Numeric(3, 2), default=0)
    review_count: Mapped[int] = mapped_column(default=0)
    stock: Mapped[int] = mapped_column(default=0)
    is_active: Mapped[bool] = mapped_column(Boolean, default=True)
    created_at: Mapped[datetime] = mapped_column(server_default=text("now()"))
    updated_at: Mapped[datetime] = mapped_column(server_default=text("now()"))


class Conversation(Base):
    __tablename__ = "conversations"

    id: Mapped[uuid.UUID] = mapped_column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    user_id: Mapped[str] = mapped_column(String(200), nullable=False, index=True)
    title: Mapped[str] = mapped_column(String(200), default="新对话")
    messages: Mapped[list] = mapped_column(JSONB, default=[])
    context: Mapped[dict] = mapped_column(JSONB, default={})
    message_count: Mapped[int] = mapped_column(default=0)
    created_at: Mapped[datetime] = mapped_column(server_default=text("now()"))
    updated_at: Mapped[datetime] = mapped_column(server_default=text("now()"))


class Evaluation(Base):
    __tablename__ = "evaluations"

    id: Mapped[int] = mapped_column(primary_key=True, autoincrement=True)
    user_id: Mapped[str] = mapped_column(String(200), nullable=False)
    conversation_id: Mapped[uuid.UUID | None] = mapped_column(ForeignKey("conversations.id", ondelete="SET NULL"))
    query: Mapped[str] = mapped_column(String(2000), nullable=False)
    response: Mapped[str | None] = mapped_column(String(5000))
    retrieved_product_ids: Mapped[list] = mapped_column(ARRAY(UUID), default=[])
    rating: Mapped[int | None] = mapped_column(SmallInteger)
    is_helpful: Mapped[bool | None] = mapped_column(Boolean)
    feedback_reason: Mapped[str | None] = mapped_column(String(500))
    latency_ms: Mapped[int | None] = mapped_column()
    created_at: Mapped[datetime] = mapped_column(server_default=text("now()"))


class ProductEmbedding(Base):
    __tablename__ = "product_embeddings"

    product_id: Mapped[uuid.UUID] = mapped_column(ForeignKey("products.id", ondelete="CASCADE"), primary_key=True)
    embedding = mapped_column(Vector(1024))


class KnowledgeDocument(Base):
    __tablename__ = "knowledge_documents"

    id: Mapped[uuid.UUID] = mapped_column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    filename: Mapped[str] = mapped_column(String(500), nullable=False)
    file_type: Mapped[str] = mapped_column(String(20))
    file_path: Mapped[str | None] = mapped_column(String(1000))
    content: Mapped[str | None] = mapped_column()
    chunk_count: Mapped[int] = mapped_column(default=0)
    status: Mapped[str] = mapped_column(String(20), default="pending")
    error_message: Mapped[str | None] = mapped_column(String(1000))
    created_at: Mapped[datetime] = mapped_column(server_default=text("now()"))
