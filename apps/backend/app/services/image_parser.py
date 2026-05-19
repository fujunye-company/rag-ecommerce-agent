"""
图片理解 — OCR/VLM → 商品结构化 [全量预留]
"""
import logging

logger = logging.getLogger("image_parser")


async def parse_product_image(image_bytes: bytes) -> dict:
    """
    解析商品图片，提取商品信息
    全量: 调用 OCR (Tesseract/EasyOCR) + VLM (DeepSeek Vision)
    返回: {product_name, brand, specs, confidence}
    """
    return {"product_name": None, "brand": None, "specs": {}, "confidence": 0.0}
