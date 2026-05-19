"""
通用响应包装 + 分页模型
遵循开发规约 v2.0 §7.2
"""
from typing import Generic, TypeVar
from pydantic import BaseModel

T = TypeVar("T")


class ApiResponse(BaseModel, Generic[T]):
    """统一 JSON 响应体: {code, data, message}"""
    code: int = 0
    data: T | None = None
    message: str = "ok"


class PaginatedResponse(BaseModel, Generic[T]):
    """分页响应: {items, total, page, size}"""
    items: list[T] = []
    total: int = 0
    page: int = 1
    size: int = 20
