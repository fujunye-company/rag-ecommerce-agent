"""API 路由层 — 只处理请求/响应，不写复杂业务"""
from app.api import (
    cart,
    chat,
    compare,
    evaluation,
    favorites,
    feedback,
    footprints,
    knowledge,
    order,
    products,
    review,
    upload,
    user,
    voice,
)

__all__ = [
    "cart",
    "chat",
    "compare",
    "evaluation",
    "favorites",
    "feedback",
    "footprints",
    "knowledge",
    "order",
    "products",
    "review",
    "upload",
    "user",
    "voice",
]
