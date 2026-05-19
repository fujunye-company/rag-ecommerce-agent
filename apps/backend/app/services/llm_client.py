"""
LLM 调用封装 — DeepSeek OpenAI-compatible
遵循开发规约 v2.0 §3.3.3: 大模型调用统一封装

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
    """工厂函数 — 可注入参数用于测试"""
    return AsyncOpenAI(
        api_key=api_key or settings.DEEPSEEK_API_KEY,
        base_url=base_url or settings.DEEPSEEK_BASE_URL,
        timeout=timeout,
    )


# 模块级默认实例 (生产使用)
_client: AsyncOpenAI | None = None


def get_client() -> AsyncOpenAI:
    """懒加载 LLM 客户端"""
    global _client
    if _client is None:
        _client = create_llm_client()
    return _client


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
    try:
        response = await _c.chat.completions.create(
            model=_model,
            messages=messages,
            temperature=temperature,
            max_tokens=max_tokens,
            stream=stream,
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
