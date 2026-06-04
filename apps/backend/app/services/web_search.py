"""
联网搜索服务 — DuckDuckGo 搜索 + LLM 知识兜底

优先使用 ddgs 库执行真实联网搜索（需网络环境支持）。
如搜索失败（国内网络受限等），回退到 LLM 训练知识回答。
"""
import logging
import os
from ddgs import DDGS

logger = logging.getLogger("web_search")

# 代理配置 — 从环境变量 DDGS_PROXY 读取，默认使用项目 Clash 代理
_DDGS_PROXY = os.getenv("DDGS_PROXY", "")


async def search_web(query: str, max_results: int = 5) -> list[dict]:
    """
    执行 DuckDuckGo 联网搜索，返回结构化结果。
    国内网络环境下可能失败，调用方应处理空结果。
    """
    try:
        results = []
        kwargs = {}
        if _DDGS_PROXY:
            kwargs["proxy"] = _DDGS_PROXY
        ddgs = DDGS(**kwargs)
        for r in ddgs.text(query, max_results=max_results):
            results.append({
                "title": r.get("title", ""),
                "url": r.get("href", ""),
                "snippet": r.get("body", ""),
            })
        logger.info("Web search for '%s': %d results", query[:40], len(results))
        return results
    except Exception as e:
        logger.warning("Web search failed for '%s': %s", query[:40], e)
        return []


async def search_or_fallback(query: str) -> tuple[list[dict], bool]:
    """
    执行联网搜索，失败时返回空列表 + is_fallback=True。
    调用方根据 is_fallback 决定使用 LLM 知识兜底。
    """
    results = await search_web(query)
    if results:
        return results, False
    return [], True


def format_search_results(results: list[dict]) -> str:
    """将搜索结果格式化为 LLM prompt 可用的文本"""
    if not results:
        return "（未找到相关网络结果）"
    lines = []
    for i, r in enumerate(results, 1):
        lines.append(f"{i}. {r['title']}\n   链接: {r['url']}\n   摘要: {r['snippet']}")
    return "\n".join(lines)


WEB_SEARCH_PROMPT = """你是一个电商导购助手。用户的问题无法通过本地商品库回答，你需要根据联网搜索结果来回答。

用户问题：{query}

联网搜索结果：
{search_results}

请根据以上搜索结果，用自然的口吻回答用户的问题。要求：
1. 如果搜索结果包含相关商品/品牌信息，列出关键信息
2. 如果搜索结果不足以完整回答，诚实告知
3. 保持简洁，200字以内
4. 在回答末尾可以提示用户进一步细化需求"""


WEB_SEARCH_FALLBACK_PROMPT = """你是一个电商导购助手。用户提出了一个需要联网搜索的问题，但暂时无法访问搜索引擎。
请根据你的训练知识来回答。你的知识截止日期约为2024年，请尽量提供有用信息。

用户问题：{query}

要求：
1. 用自然的口吻回答
2. 如果知识可能过时，请在开头说明"以下信息基于我的训练知识，可能不是最新数据："
3. 保持简洁，200字以内"""
