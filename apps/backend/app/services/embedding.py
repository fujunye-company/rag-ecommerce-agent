"""
Embedding 封装 — 本地 BGE 模型，异步非阻塞
"""
import asyncio
import logging
from sentence_transformers import SentenceTransformer
from app.core.config import settings

logger = logging.getLogger("embedding")

_embedding_model: SentenceTransformer | None = None


def get_embedding_model() -> SentenceTransformer:
    """懒加载 embedding 模型 (单例)，warmup 首次调用"""
    global _embedding_model
    if _embedding_model is None:
        logger.info("Loading embedding model: %s", settings.EMBEDDING_MODEL)
        _embedding_model = SentenceTransformer(settings.EMBEDDING_MODEL)
        # warmup: encode a short text to avoid first-request latency
        _embedding_model.encode("warmup", normalize_embeddings=True)
        logger.info("Embedding model ready, dim=%d", _embedding_model.get_sentence_embedding_dimension())
    return _embedding_model


async def embed_text(text: str) -> list[float]:
    """单文本 → 向量 (异步非阻塞)"""
    model = get_embedding_model()
    embedding = await asyncio.to_thread(
        model.encode, text, normalize_embeddings=True
    )
    return embedding.tolist()


async def embed_batch(texts: list[str]) -> list[list[float]]:
    """批量文本 → 向量列表 (异步非阻塞)"""
    if not texts:
        return []
    model = get_embedding_model()
    embeddings = await asyncio.to_thread(
        model.encode, texts, normalize_embeddings=True
    )
    return embeddings.tolist()
