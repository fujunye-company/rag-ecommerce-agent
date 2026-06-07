"""API 路由层 — 只处理请求/响应，不写复杂业务"""
from app.api import audio, chat, products, upload, evaluation, feedback, compare, knowledge

__all__ = ["audio", "chat", "products", "upload", "evaluation", "feedback", "compare", "knowledge"]
