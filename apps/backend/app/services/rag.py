"""
RAG 检索入口 — 编排 retrieval (MVP含基础rerank)
"""
import logging
from app.services.retriever import hybrid_search
from app.services.embedding import embed_text

logger = logging.getLogger("rag")


async def retrieve(query: str, top_k: int = 5, **filters) -> list[dict]:
    """
    RAG 检索主入口:
    1. 文本 → embedding
    2. 向量 + 元数据混合检索
    3. MVP: 按 score 排序 (全量: cross-encoder rerank)
    """
    vector = await embed_text(query)
    chunks = await hybrid_search(
        query_vector=vector,
        query_text=query,
        top_k=top_k,
        **filters,
    )
    # MVP 基础排序: 按 score 降序
    chunks.sort(key=lambda c: c["score"], reverse=True)
    logger.info("RAG retrieve: query='%s', results=%d, top_score=%.3f",
                query[:60], len(chunks), chunks[0]["score"] if chunks else 0)
    return chunks[:top_k]
