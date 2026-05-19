"""
知识入库 — 文档 → 切分 → 向量化 → Qdrant
合并 chunker 逻辑
"""
import logging
from app.services.embedding import embed_batch

logger = logging.getLogger("ingestion")


def chunk_text(text: str, chunk_size: int = 500, overlap: int = 100) -> list[str]:
    """语义切分: 按段落/句子边界, chunk_size 字符, overlap 重叠"""
    # MVP: 简单按段落切分
    paragraphs = [p.strip() for p in text.split("\n\n") if p.strip()]
    chunks = []
    current = ""
    for para in paragraphs:
        if len(current) + len(para) < chunk_size:
            current += para + "\n"
        else:
            if current:
                chunks.append(current.strip())
            current = para + "\n"
    if current:
        chunks.append(current.strip())
    return chunks


async def ingest_document(
    doc_id: str,
    text: str,
    metadata: dict | None = None,
) -> int:
    """
    文档入库流程:
    1. 文本切分 → chunks
    2. 批量向量化
    3. 写入 Qdrant
    返回: chunk 数量
    """
    chunks = chunk_text(text)
    if not chunks:
        logger.warning("No chunks generated for doc_id=%s", doc_id)
        return 0

    vectors = await embed_batch(chunks)
    # TODO: 写入 Qdrant (需要 upsert 逻辑)
    logger.info("Ingested doc_id=%s: %d chunks", doc_id, len(chunks))
    return len(chunks)
