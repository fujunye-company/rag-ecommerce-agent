"""
评测 Schema — EvalRequest / EvalReport [全量预留]
"""
from pydantic import BaseModel


class EvalCase(BaseModel):
    id: str
    query: str
    expected_intent: str
    expected_product_ids: list[str] = []


class EvalRequest(BaseModel):
    test_cases: list[EvalCase]
    metrics: list[str] = ["faithfulness", "relevance", "precision", "recall"]


class EvalMetrics(BaseModel):
    faithfulness: float = 0.0
    relevance: float = 0.0
    precision: float = 0.0
    recall: float = 0.0


class EvalReport(BaseModel):
    total_cases: int
    passed: int
    failed: int
    metrics: EvalMetrics
    details: list[dict] = []
