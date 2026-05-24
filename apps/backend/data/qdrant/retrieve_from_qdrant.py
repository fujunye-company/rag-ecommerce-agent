"""Qdrant 语义检索 + DeepSeek LLM 流式回答 (bge-large-zh-v1.5, 1024维)

运行环境: Python 3.11+, ~/.hermes-venv
依赖: pip install qdrant-client sentence-transformers openai
用法:
    python retrieve_from_qdrant.py                           # 交互模式
    python retrieve_from_qdrant.py "降噪耳机推荐"             # 单次查询
    python retrieve_from_qdrant.py --test                    # 内置测试查询
"""

import os
import sys
import json
import argparse
from sentence_transformers import SentenceTransformer
from qdrant_client import QdrantClient
from openai import OpenAI

# ── 配置 ──────────────────────────────────────────────────
QDRANT_URL = os.environ.get("QDRANT_URL", "http://localhost:6333")
COLLECTION_NAME = os.environ.get("QDRANT_COLLECTION", "products")
EMBEDDING_MODEL = "BAAI/bge-large-zh-v1.5"

# LLM: 优先 DeepSeek（当前降级方案），Doubao key 待确认
LLM_API_KEY = os.environ.get("DEEPSEEK_API_KEY", "")
LLM_BASE_URL = os.environ.get("DEEPSEEK_BASE_URL", "https://api.deepseek.com/v1")
LLM_MODEL = os.environ.get("DEEPSEEK_MODEL", "deepseek-chat")

TOP_K = 5

# ── 工具函数 ──────────────────────────────────────────────

def load_model():
    """加载 Embedding 模型"""
    print(f"加载模型: {EMBEDDING_MODEL} ...", file=sys.stderr)
    model = SentenceTransformer(EMBEDDING_MODEL)
    dim = model.get_sentence_embedding_dimension()
    print(f"  向量维度: {dim}", file=sys.stderr)
    return model


def search(client, model, query, top_k=TOP_K):
    """向量检索"""
    query_vec = model.encode(
        [query],
        normalize_embeddings=True,
        show_progress_bar=False,
    )[0].tolist()

    results = client.search(
        collection_name=COLLECTION_NAME,
        query_vector=query_vec,
        limit=top_k,
        with_payload=True,
    )
    return results


def format_results(results):
    """格式化检索结果"""
    lines = []
    for i, hit in enumerate(results, 1):
        p = hit.payload or {}
        score = hit.score
        lines.append(
            f"  [{i}] {p.get('title', '?')} | "
            f"{p.get('brand', '?')} | "
            f"¥{p.get('price', '?')} | "
            f"评分{p.get('rating', '?')}★ | "
            f"相似度={score:.4f}"
        )
        highlights = p.get("highlights", [])
        if highlights:
            lines.append(f"      亮点: {' / '.join(highlights)}")
        scenarios = p.get("scenarios", [])
        if scenarios:
            lines.append(f"      场景: {', '.join(scenarios)}")
    return "\n".join(lines)


def build_context(results):
    """构建 LLM 上下文"""
    parts = []
    for hit in results:
        p = hit.payload or {}
        parts.append(
            f"商品: {p.get('title', '?')} | 品牌: {p.get('brand', '?')} | "
            f"价格: ¥{p.get('price', '?')} | 评分: {p.get('rating', '?')}★ | "
            f"亮点: {' / '.join(p.get('highlights', []))} | "
            f"场景: {', '.join(p.get('scenarios', []))}"
        )
    return "\n".join(parts)


def ask_llm(query, context):
    """调用 DeepSeek 生成推荐回答"""
    if not LLM_API_KEY:
        return "[LLM 未配置 — 仅显示检索结果]"

    client_llm = OpenAI(api_key=LLM_API_KEY, base_url=LLM_BASE_URL)
    system_prompt = (
        "你是一个专业的电商导购助手。根据检索到的商品信息，为用户推荐最合适的商品。"
        "回答需包含：推荐理由、关键卖点、适用场景。用中文回复，简洁专业。"
    )
    response = client_llm.chat.completions.create(
        model=LLM_MODEL,
        messages=[
            {"role": "system", "content": system_prompt},
            {"role": "user", "content": f"用户查询: {query}\n\n检索到的商品:\n{context}"},
        ],
        temperature=0.7,
        max_tokens=1024,
    )
    return response.choices[0].message.content or ""


# ── 主逻辑 ────────────────────────────────────────────────

def run_query(model, client, query):
    """执行一次完整的检索+推荐"""
    print(f"\n{'='*60}")
    print(f"查询: {query}")
    print(f"{'='*60}")

    results = search(client, model, query)
    if not results:
        print("  ⚠️ 未找到相关商品")
        return

    print(f"\n📊 检索结果 (Top {len(results)}):")
    print(format_results(results))

    print(f"\n🤖 LLM 推荐 ({LLM_MODEL}):")
    context = build_context(results)
    answer = ask_llm(query, context)
    print(f"  {answer}")


def main():
    parser = argparse.ArgumentParser(description="RAG 电商语义检索 + LLM 推荐")
    parser.add_argument("query", nargs="?", help="查询文本")
    parser.add_argument("--test", action="store_true", help="运行内置测试查询")
    args = parser.parse_args()

    # 初始化
    model = load_model()
    client = QdrantClient(url=QDRANT_URL, timeout=30)

    # 检查连接
    if not client.collection_exists(COLLECTION_NAME):
        print(f"❌ Collection '{COLLECTION_NAME}' 不存在，请先运行 ingest_to_qdrant.py")
        sys.exit(1)

    count = client.count(collection_name=COLLECTION_NAME, exact=True).count
    print(f"Qdrant 连接成功: {count} 件商品就绪\n", file=sys.stderr)

    if args.test:
        test_queries = [
            "推荐一款降噪效果好的头戴式耳机，预算2000左右",
            "适合玩游戏的手机，预算4000以内",
            "学生党买平板，主要用来看网课和记笔记",
        ]
        for q in test_queries:
            run_query(model, client, q)
        return

    if args.query:
        run_query(model, client, args.query)
        return

    # 交互模式
    print("🔍 RAG 电商检索 — 交互模式 (输入 quit 退出)")
    print(f"   Embedding: {EMBEDDING_MODEL} (1024维)")
    print(f"   LLM: {LLM_MODEL}")
    print()
    while True:
        try:
            query = input("查询> ").strip()
        except (EOFError, KeyboardInterrupt):
            print("\n再见！")
            break
        if query.lower() in ("quit", "exit", "q"):
            break
        if not query:
            continue
        run_query(model, client, query)


if __name__ == "__main__":
    main()
