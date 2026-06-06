"""
用户相关 Pydantic 模型 — 请求/响应校验
"""
from pydantic import BaseModel, Field


class UserCreate(BaseModel):
    """创建用户请求体 — 字段与前端 SQLite user_profile 一致"""
    id: str = Field(..., description="用户 UUID（sw 前缀）")
    nickname: str = ""
    gender: str = ""
    age_range: str = ""
    budget_min: float = 0.0
    budget_max: float = 99999.0
    preferred_categories: str = "[]"
    is_guest: int = 1


class UserUpdate(BaseModel):
    """更新用户请求体 — 所有字段可选"""
    nickname: str | None = None
    gender: str | None = None
    age_range: str | None = None
    budget_min: float | None = None
    budget_max: float | None = None
    preferred_categories: str | None = None
    avatar: bytes | None = None
    is_guest: int | None = None


class UserRead(BaseModel):
    """用户响应体"""
    id: str
    nickname: str
    gender: str
    age_range: str
    budget_min: float
    budget_max: float
    preferred_categories: str
    is_guest: int
    created_at: str | None = None
    updated_at: str | None = None


class UserSyncRequest(BaseModel):
    """前端同步用户画像到后端的请求体"""
    id: str = Field(..., description="用户 UUID（sw 前缀）")
    nickname: str = ""
    gender: str = ""
    age_range: str = ""
    budget_min: float = 0.0
    budget_max: float = 99999.0
    preferred_categories: str = "[]"
    is_guest: int = 1
