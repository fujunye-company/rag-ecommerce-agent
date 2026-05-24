"""
LLM 调用封装 — Doubao (豆包) OpenAI-compatible，DeepSeek 降级回退
遵循开发规约 v2.0 §3.3.3: 大模型调用统一封装

优先级: Doubao (比赛指定) → DeepSeek (降级)
可测试性: create_llm_client() 接受参数注入，测试时可传 mock client。
"""
import time
import logging
from openai import AsyncOpenAI
from app.core.config import settings

logger = logging.getLogger("llm_client")


def create_llm_client(
    api_key: str = "",
    base_url: str = "",
    timeout: float = 30.0,
) -> AsyncOpenAI:
    """工厂函数 — 优先 Doubao，回退 DeepSeek"""
    key = api_key or settings.DEEPSEEK_API_KEY or settings.DOUBAO_API_KEY
    url = base_url or settings.DEEPSEEK_BASE_URL or settings.DOUBAO_BASE_URL
    return AsyncOpenAI(
        api_key=key,
        base_url=url,
        timeout=timeout,
    )


# 模块级默认实例 (生产使用)
_client: AsyncOpenAI | None = None
_fast_client: AsyncOpenAI | None = None


def get_client() -> AsyncOpenAI:
    """懒加载 LLM 客户端（Doubao，用于最终生成）"""
    global _client
    if _client is None:
        _client = create_llm_client()
    return _client


def get_fast_client() -> AsyncOpenAI:
    """懒加载快速 LLM 客户端（DeepSeek，用于意图/槽位等轻量任务）"""
    global _fast_client
    if _fast_client is None:
        _fast_client = AsyncOpenAI(
            api_key=settings.DEEPSEEK_API_KEY,
            base_url=settings.DEEPSEEK_BASE_URL,
            timeout=15.0,
        )
    return _fast_client


async def fast_chat_completion(
    messages: list[dict],
    temperature: float = 0.1,
    max_tokens: int = 200,
) -> str:
    """
    轻量任务快速 LLM 调用 — DeepSeek（无推理链，~0.5s）。
    用于意图分类、槽位提取、查询改写等简单结构化输出任务。
    """
    start = time.time()
    try:
        response = await get_fast_client().chat.completions.create(
            model=settings.DEEPSEEK_MODEL,
            messages=messages,
            temperature=temperature,
            max_tokens=max_tokens,
        )
        elapsed = time.time() - start
        content = response.choices[0].message.content or ""
        logger.info("Fast LLM: model=%s, tokens=%s, elapsed=%.1fs",
                    settings.DEEPSEEK_MODEL, getattr(response, 'usage', '?'), elapsed)
        return content
    except Exception as exc:
        elapsed = time.time() - start
        logger.error("Fast LLM failed after %.1fs: %s", elapsed, exc)
        raise


async def chat_completion(
    messages: list[dict],
    model: str | None = None,
    temperature: float = 0.7,
    max_tokens: int = 2048,
    stream: bool = False,
    client: AsyncOpenAI | None = None,
) -> str | AsyncOpenAI:
    """
    调用 LLM 生成回答

    Args:
        messages: 对话消息列表
        model: 模型名 (默认 settings.LLM_MODEL)
        temperature: 温度参数
        max_tokens: 最大 token 数
        stream: True 返回异步流对象 (SSE), False 返回完整文本
        client: 可注入的 AsyncOpenAI 实例 (测试用)

    Returns:
        stream=False: 完整回答文本
        stream=True: AsyncOpenAI 流对象
    """
    _c = client or get_client()
    _model = model or settings.LLM_MODEL
    start = time.time()

    # Doubao-Seed-2.0-lite 默认开启 Chain-of-Thought (reasoning_content)，
    # 导致 TTFT 从 1.3s 暴涨到 10s。必须显式关闭 thinking。
    # 仅对 Doubao (volces.com / ep-* endpoint) 添加此参数。
    _is_doubao = ("volces.com" in str(_c.base_url)) or _model.startswith("ep-")
    extra_kwargs = {}
    if _is_doubao:
        extra_kwargs["extra_body"] = {"thinking": {"type": "disabled"}}

    try:
        response = await _c.chat.completions.create(
            model=_model,
            messages=messages,
            temperature=temperature,
            max_tokens=max_tokens,
            stream=stream,
            **extra_kwargs,
        )
        elapsed = time.time() - start
        if not stream:
            content = response.choices[0].message.content or ""
            logger.info("LLM call: model=%s, tokens=%s, elapsed=%.1fs",
                        _model, getattr(response, 'usage', '?'), elapsed)
            return content
        logger.info("LLM stream started: model=%s", _model)
        return response
    except Exception as exc:
        elapsed = time.time() - start
        logger.error("LLM call failed after %.1fs: %s", elapsed, exc)
        raise
