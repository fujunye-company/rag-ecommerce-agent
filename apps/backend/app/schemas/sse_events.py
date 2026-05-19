"""
SSE 事件类型统一维护
遵循开发规约 v2.0 §7.3
"""
from pydantic import BaseModel


# ── MVP 事件 ──
class TextDeltaEvent(BaseModel):
    type: str = "text_delta"
    content: str


class ProductCardEvent(BaseModel):
    type: str = "product_cards"
    products: list[dict]


class DoneEvent(BaseModel):
    type: str = "done"


class ErrorEvent(BaseModel):
    type: str = "error"
    message: str
    code: str


# ── 全量扩展 (预留) ──
class IntentEvent(BaseModel):
    type: str = "intent"
    intent: str
    slots: dict = {}


class RetrievalEvent(BaseModel):
    type: str = "retrieval"
    chunks: list[dict] = []


class CompareEvent(BaseModel):
    type: str = "compare"
    dimensions: list[dict] = []


# ── 联合类型 ──
SSEEvent = TextDeltaEvent | ProductCardEvent | DoneEvent | ErrorEvent
