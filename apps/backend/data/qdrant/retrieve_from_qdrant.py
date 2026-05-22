"""Qdrant Server 语义检索 + LLM 流式回答 (4096维)

运行环境: Python 3.11+
依赖: pip install qdrant-client transformers torch numpy langchain-ollama langchain-core
"""
import os
import sys
import numpy as np
import torch

from transformers import AutoTokenizer, AutoModel

from langchain_ollama import ChatOllama
from langchain_core.output_parsers import StrOutputParser
from langchain_core.prompts import ChatPromptTemplate

from qdrant_client import QdrantClient

QDRANT_URL = "http://localhost:6333"
COLLECTION_NAME = "products"
BASE_DIR = os.path.dirname(os.path.abspath(__file__))
MODEL_PATH = os.path.join(BASE_DIR, "RAGchat", "bge-small-zh")

BASE_DIM = 512
TARGET_DIM = 4096
BATCH_SIZE = 8

LLM_MODEL = "glm-5.1:cloud"
LLM_BASE_URL = "http://localhost:11434/v1"


class BGEEmbedder:

    def __init__(self):
        self._tokenizer = AutoTokenizer.from_pretrained(MODEL_PATH)
        self._model = AutoModel.from_pretrained(MODEL_PATH)
        self._model.eval()
        self._proj = self._load_projection()

    def _load_projection(self):
        rng = np.random.RandomState(42)
        matrix = rng.randn(BASE_DIM, TARGET_DIM).astype(np.float32)
        norms = np.linalg.norm(matrix, axis=0, keepdims=True)
        matrix = matrix / norms
        return matrix

    def _mean_pooling(self, last_hidden, attention_mask):
        mask = attention_mask.unsqueeze(-1).float()
        return (last_hidden * mask).sum(dim=1) / mask.sum(dim=1).clamp(min=1e-9)

    def _encode(self, texts):
        all_embeddings = []
        for i in range(0, len(texts), BATCH_SIZE):
            batch = texts[i : i + BATCH_SIZE]
            encoded = self._tokenizer(
                batch, padding=True, truncation=True, max_length=512, return_tensors="pt"
            )
            with torch.no_grad():
                outputs = self._model(**encoded)
                embeddings = self._mean_pooling(outputs.last_hidden_state, encoded["attention_mask"])
                norms = embeddings.norm(dim=1, keepdim=True)
                embeddings = embeddings / norms
            all_embeddings.append(embeddings.numpy())
        return np.vstack(all_embeddings)

    def embed_query(self, text):
        base = self._encode([text])
        projected = base @ self._proj
        norms = np.linalg.norm(projected, axis=1, keepdims=True)
        projected = projected / norms
        return projected[0].tolist()


def search_products(client, embedder, query, k=5):
    vector = embedder.embed_query(query)
    results = client.query_points(
        collection_name=COLLECTION_NAME,
        query=vector,
        limit=k,
        with_payload=True,
    )
    return [pt.payload for pt in results.points]


def format_product(m):
    return {
        "商品名称": m.get("title", "N/A"),
        "商品分类": m.get("category", "N/A"),
        "商品品牌": m.get("brand", "N/A"),
        "商品价格": f"￥{m.get('price', 0):.2f}",
        "商品评分": f"{m.get('rating', 0)}/5.0",
        "商品评论数量": m.get("rating_count", 0),
        "商品属性": m.get("attributes", {}),
        "商品亮点": m.get("highlights", []),
        "商品场景": m.get("scenarios", []),
    }


def format_docs(docs):
    lines = []
    for m in docs:
        attrs = " ".join(f"{k}:{v}" for k, v in m.get("attributes", {}).items())
        highlights = " ".join(m.get("highlights", []))
        lines.append(
            f"商品: {m.get('title','')} | 品牌: {m.get('brand','')} | "
            f"价格: ￥{m.get('price',0)} | 评分: {m.get('rating',0)}/5.0 | "
            f"分类: {m.get('category','')} | 属性: {attrs} | 亮点: {highlights}"
        )
    return "\n".join(lines)


PROMPT = ChatPromptTemplate.from_messages([
    (
        "system",
        "你是一个专业的电商导购助手。请根据以下商品信息回答用户的问题，"
        "给出具体推荐并说明理由。如果信息不足，请如实说明。",
    ),
    ("human", "商品信息:\n{context}\n\n用户问题: {question}"),
])


def main():
    sys.stdout.reconfigure(encoding="utf-8", errors="replace")

    embedder = BGEEmbedder()
    client = QdrantClient(url=QDRANT_URL)

    if not client.collection_exists(COLLECTION_NAME):
        raise RuntimeError(
            f"Qdrant collection '{COLLECTION_NAME}' not found. "
            "Please run ingest_to_qdrant.py first."
        )

    llm = ChatOllama(model=LLM_MODEL, base_url=LLM_BASE_URL)
    chain = PROMPT | llm | StrOutputParser()

    queries = [
        "推荐一款适合通勤使用的降噪耳机",
        "性价比高的游戏手机有什么推荐",
        "适合学生学习用的平板电脑",
    ]

    for idx, query in enumerate(queries, 1):
        print(f"\n{'='*70}")
        print(f"[Query {idx}] {query}")
        print(f"{'='*70}")

        retrieved = search_products(client, embedder, query, k=5)
        context = format_docs(retrieved)

        try:
            print(f"\n[LLM Answer (streaming)]")
            for chunk in chain.stream({"context": context, "question": query}):
                print(chunk, end="", flush=True)
            print()
        except Exception as e:
            print(f"\n  [LLM not available: {e}]")

        print(f"\n[Retrieved Products (similarity search)]")
        for i, m in enumerate(retrieved[:3], 1):
            fmt = format_product(m)
            print(f"\n--- #{i} ---")
            for key, val in fmt.items():
                val_str = str(val)
                if len(val_str) > 100:
                    val_str = val_str[:100] + "..."
                print(f"  {key}: {val_str}")


if __name__ == "__main__":
    main()
