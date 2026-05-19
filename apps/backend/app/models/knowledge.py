"""
知识库版本表 ORM 模型 [全量预留]
"""
import uuid
from datetime import datetime
from sqlalchemy import String, Integer, DateTime, func
from sqlalchemy.dialects.postgresql import UUID
from sqlalchemy.orm import Mapped, mapped_column
from app.core.database import Base


class KnowledgeVersion(Base):
    __tablename__ = "knowledge_versions"

    id: Mapped[uuid.UUID] = mapped_column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    version: Mapped[str] = mapped_column(String(32), nullable=False, unique=True)
    description: Mapped[str | None] = mapped_column(String(512), nullable=True)
    document_count: Mapped[int] = mapped_column(Integer, default=0)
    chunk_count: Mapped[int] = mapped_column(Integer, default=0)
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), server_default=func.now())
