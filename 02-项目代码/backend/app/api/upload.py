"""文件上传端点"""
from fastapi import APIRouter, UploadFile, File, BackgroundTasks

router = APIRouter()


@router.post("/upload")
async def upload_document(
    file: UploadFile = File(...),
    background_tasks: BackgroundTasks = None,
):
    # TODO: 文件存储+后台向量化入库
    return {"message": "上传成功", "filename": file.filename, "status": "processing"}
