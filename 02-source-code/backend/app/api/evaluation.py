"""评测反馈端点"""
from fastapi import APIRouter

router = APIRouter()


@router.post("/evaluation")
async def submit_feedback(
    conversation_id: str,
    rating: int,
    is_helpful: bool = True,
    reason: str = "",
):
    # TODO: 存储反馈数据
    return {"status": "ok", "id": "eval-001"}
