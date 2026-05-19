"""
重排序 — cross-encoder / LLM rerank [全量预留]
"""
import logging

logger = logging.getLogger("reranker")


async def rerank(query: str, chunks: list[dict], top_k: int = 5) -> list[dict]:
    """
    MVP: 不做重排序，直接返回
    全量: 用 cross-encoder 或 LLM 对候选 chunk 重排
    """
    return chunks[:top_k]
