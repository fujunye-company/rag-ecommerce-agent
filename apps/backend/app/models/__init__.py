"""ORM 模型包 — 自动建表依赖导入"""
from app.models.product import Product
from app.models.session import Session
from app.models.message import Message
from app.models.feedback import Feedback
from app.models.knowledge import KnowledgeVersion

__all__ = ["Product", "Session", "Message", "Feedback", "KnowledgeVersion"]
