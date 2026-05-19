"""上传接口 — 纯路由层"""
from fastapi import APIRouter, UploadFile, File
from app.schemas.common import ApiResponse

router = APIRouter()


@router.post("/upload/image")
async def upload_image(file: UploadFile = File(...)):
    """图片上传"""
    return ApiResponse(
        data={"filename": file.filename, "content_type": file.content_type},
        message="上传成功"
    ).model_dump()


@router.post("/documents/upload")
async def upload_document(file: UploadFile = File(...)):
    """知识文档上传 [全量]"""
    return ApiResponse(
        data={"filename": file.filename, "status": "processing"},
        message="文档已接收，正在处理"
    ).model_dump()
