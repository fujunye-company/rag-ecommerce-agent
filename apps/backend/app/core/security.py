"""
安全工具 — 敏感词过滤、上传校验、PIPL预留
遵循开发规约 v2.0 §16
"""
import re


# ── 上传校验 ──
ALLOWED_IMAGE_TYPES = {"image/jpeg", "image/png", "image/webp"}
MAX_UPLOAD_SIZE_BYTES = 10 * 1024 * 1024  # 10MB


def validate_image_upload(content_type: str | None, file_size: int) -> None:
    """校验上传图片类型和大小"""
    if content_type and content_type not in ALLOWED_IMAGE_TYPES:
        raise ValueError(f"不支持的文件类型: {content_type}，仅允许 jpg/png/webp")
    if file_size > MAX_UPLOAD_SIZE_BYTES:
        raise ValueError(f"文件大小超过限制: {MAX_UPLOAD_SIZE_BYTES // 1024 // 1024}MB")


# ── 敏感词过滤 (MVP占位) ──
# TODO: 全量阶段接入专业敏感词库或 API
def filter_sensitive(text: str) -> str:
    """过滤敏感内容，MVP阶段返回原文"""
    return text
