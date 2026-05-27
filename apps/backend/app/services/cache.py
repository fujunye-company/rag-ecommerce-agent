"""
查询缓存 — 内存 LRU，热门查询避免重复 LLM 调用
线程安全（asyncio.Lock），购物车等动态查询不缓存
"""
import asyncio
import hashlib
import time
import logging
from collections import OrderedDict

logger = logging.getLogger("cache")

# LRU 缓存：最多 100 条，5 分钟 TTL
_cache: OrderedDict[str, tuple[float, dict]] = OrderedDict()
_lock = asyncio.Lock()
MAX_SIZE = 100
TTL_SECONDS = 300

# 缓存版本 — 参数变更时递增，自动失效旧缓存（top_k=3 修复等）
CACHE_VERSION = 2

# 动态查询关键词 — 命中则不缓存
SKIP_CACHE_KEYWORDS = ["购物车", "加购", "加到购物车", "加入购物车", "查看购物车", "清空购物车", "下单"]


def _key(query: str) -> str:
    return hashlib.md5(query.strip().lower().encode()).hexdigest()


def _is_dynamic(query: str) -> bool:
    """是否为动态查询（购物车等）— 此类查询不应缓存"""
    return any(kw in query for kw in SKIP_CACHE_KEYWORDS)


async def get(query: str) -> dict | None:
    """查询缓存，返回 {response, cards, _v} 或 None"""
    if _is_dynamic(query):
        return None
    k = _key(query)
    async with _lock:
        if k in _cache:
            ts, value = _cache[k]
            if time.monotonic() - ts < TTL_SECONDS:
                # 版本校验：旧版本缓存自动失效
                if value.get("_v") != CACHE_VERSION:
                    del _cache[k]
                    logger.debug("Cache STALE (v%d != v%d): %s", value.get("_v", 0), CACHE_VERSION, query[:30])
                    return None
                _cache.move_to_end(k)
                logger.debug("Cache HIT: %s", query[:30])
                return value
            else:
                del _cache[k]
    return None


async def set(query: str, response: str, cards: list):
    """写入缓存"""
    if _is_dynamic(query):
        return
    k = _key(query)
    async with _lock:
        if len(_cache) >= MAX_SIZE:
            _cache.popitem(last=False)
        _cache[k] = (time.monotonic(), {"response": response, "cards": cards, "_v": CACHE_VERSION})
    logger.debug("Cache SET: %s", query[:30])


async def stats() -> dict:
    """缓存统计"""
    async with _lock:
        return {"size": len(_cache), "max": MAX_SIZE, "ttl_s": TTL_SECONDS}


async def clear():
    async with _lock:
        _cache.clear()
