"""
重排序 — Sentence-Transformers Cross-Encoder

对向量检索粗排结果做精排，提升 Top-K 相关性。
接口: (query, list[doc]) → list[ranked_doc]

兼容两种文档格式:
- Qdrant: {"id":"...", "score":0.8, "payload":{"text":"...", ...}}
- 通用: {"content":"...", "score":0.8, "metadata":{...}}
"""
import asyncio
import os
os.environ.setdefault("HF_HUB_OFFLINE", "1")
os.environ.setdefault("TRANSFORMERS_OFFLINE", "1")

import logging
from typing import List, Dict, Optional

logger = logging.getLogger("reranker")

# 延迟加载
_reranker_model = None


def _get_content(doc: Dict) -> str:
    """从 Qdrant 或通用格式文档中提取文本内容"""
    # Qdrant 格式: payload.text
    payload = doc.get("payload", {})
    if isinstance(payload, dict) and payload.get("text"):
        return payload["text"]
    # 通用格式
    return doc.get("content", "") or str(payload) if payload else ""


def _get_model():
    """延迟加载 BGE-Reranker v2-m3 Cross-Encoder"""
    global _reranker_model
    if _reranker_model is None:
        from sentence_transformers import CrossEncoder
        model_name = "BAAI/bge-reranker-v2-m3"
        logger.info("Loading reranker model: %s ...", model_name)
        _reranker_model = CrossEncoder(model_name)
        logger.info("Reranker model loaded: %s", model_name)
    return _reranker_model


def rerank(
    query: str,
    documents: List[Dict],
    top_k: int = 10
) -> List[Dict]:
    """
    对检索结果重排序。

    Args:
        query: 用户查询
        documents: [{"content": "...", "score": 0.8, "metadata": {...}}, ...]
        top_k: 返回 Top-K

    Returns:
        按 relevance 降序排列的文档列表，新增 rerank_score 和 final_score 字段
    """
    if not documents:
        return []

    model = _get_model()

    # 构造 (query, doc) pairs（兼容 Qdrant 和通用格式）
    pairs = [(query, _get_content(doc)) for doc in documents]

    # Cross-Encoder 打分（返回原始 logits）
    scores = model.predict(pairs)

    # 确保 scores 是列表（单文档时返回标量）
    if not hasattr(scores, '__iter__'):
        scores = [float(scores)]
    else:
        scores = [float(s) for s in scores]

    # sigmoid 归一化 CrossEncoder logits → [0, 1]
    def _sigmoid(x):
        import math
        try:
            return 1.0 / (1.0 + math.exp(-x))
        except OverflowError:
            return 1.0 if x > 0 else 0.0

    # 合并分数并排序（浅拷贝避免污染原数据）
    ranked = []
    for doc, score in zip(documents, scores):
        normalized_score = _sigmoid(score)
        entry = {**doc}
        entry["rerank_score"] = round(normalized_score, 4)
        # final_score: 向量检索分 * 0.3 + sigmoid 重排序分 * 0.7
        entry["final_score"] = round(
            float(doc.get("score", 0.0)) * 0.3 + normalized_score * 0.7,
            4
        )
        ranked.append(entry)

    ranked.sort(key=lambda x: x["final_score"], reverse=True)

    result = ranked[:top_k]
    logger.info("Rerank: '%s' → %d docs reranked, top_k=%d",
                query[:50], len(documents), len(result))
    return result


async def rerank_async(
    query: str,
    documents: List[Dict],
    top_k: int = 10
) -> List[Dict]:
    """异步包装 — 在线程池中执行 CrossEncoder 推理，不阻塞事件循环"""
    return await asyncio.to_thread(rerank, query, documents, top_k)
