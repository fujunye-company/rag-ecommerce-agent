"""知识库管理接口 [全量预留]"""
from fastapi import APIRouter
from app.schemas.common import ApiResponse

router = APIRouter()


@router.post("/knowledge/ingest")
async def ingest_knowledge():
    """知识库入库"""
    return ApiResponse(
        data={"status": "not_implemented"},
        message="知识库入库功能全量阶段开发",
        code=0
    ).model_dump()
