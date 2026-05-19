"""
Embedding 封装 — 本地 BGE 模型 / API 切换
"""
import logging
from sentence_transformers import SentenceTransformer
from app.core.config import settings

logger = logging.getLogger("embedding")

_embedding_model = None


def get_embedding_model() -> SentenceTransformer:
    """懒加载 embedding 模型 (单例)"""
    global _embedding_model
    if _embedding_model is None:
        logger.info("Loading embedding model: %s", settings.EMBEDDING_MODEL)
        _embedding_model = SentenceTransformer(settings.EMBEDDING_MODEL)
    return _embedding_model


async def embed_text(text: str) -> list[float]:
    """单文本 → 向量"""
    model = get_embedding_model()
    return model.encode(text).tolist()


async def embed_batch(texts: list[str]) -> list[list[float]]:
    """批量文本 → 向量列表"""
    model = get_embedding_model()
    return model.encode(texts).tolist()
