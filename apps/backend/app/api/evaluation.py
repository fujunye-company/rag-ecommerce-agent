"""评测接口 — 纯路由层 [全量预留]"""
from fastapi import APIRouter
from app.schemas.common import ApiResponse

router = APIRouter()


@router.post("/evaluation/run")
async def run_evaluation():
    """触发评测 (全量阶段实现)"""
    return ApiResponse(
        data={"eval_id": "eval-001", "status": "queued"},
        message="评测任务已提交"
    ).model_dump()


@router.get("/eval/report")
async def get_eval_report():
    """获取评测报告 (全量阶段实现)"""
    return ApiResponse(
        data={"status": "not_implemented"},
        message="评测报告功能全量阶段开发"
    ).model_dump()
