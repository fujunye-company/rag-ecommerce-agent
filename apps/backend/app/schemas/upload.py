"""
上传 Schema — UploadResponse / ImageAnalysis [全量预留]
"""
from pydantic import BaseModel


class UploadResponse(BaseModel):
    file_id: str
    filename: str
    content_type: str
    size_bytes: int


class ImageAnalysis(BaseModel):
    """图片理解结果 [全量]"""
    file_id: str
    product_name: str | None = None
    brand: str | None = None
    specs: dict = {}
    confidence: float = 0.0
