"""
评测执行 — RAGAS 指标, 测试集跑批 [全量预留]
"""
import logging

logger = logging.getLogger("evaluator")


async def run_evaluation(test_cases: list[dict], metrics: list[str] | None = None) -> dict:
    """
    运行评测: 对每条 test_case 跑完整 Agent 流程, 计算 RAGAS 指标
    返回: {total, passed, failed, metrics: {...}, details: [...]}
    """
    return {"total": 0, "passed": 0, "failed": 0, "metrics": {}, "details": []}
