"""
查询缓存 — 内存 LRU，热门查询避免重复 LLM 调用
"""
import hashlib
import time
import logging
from collections import OrderedDict

logger = logging.getLogger("cache")

# LRU 缓存：最多 100 条，5 分钟 TTL
_cache: OrderedDict[str, tuple[float, dict]] = OrderedDict()
MAX_SIZE = 100
TTL_SECONDS = 300


def _key(query: str) -> str:
    return hashlib.md5(query.strip().lower().encode()).hexdigest()


def get(query: str) -> dict | None:
    """查询缓存，返回 {response, cards} 或 None"""
    k = _key(query)
    if k in _cache:
        ts, value = _cache[k]
        if time.monotonic() - ts < TTL_SECONDS:
            _cache.move_to_end(k)  # LRU: move to end
            logger.debug("Cache HIT: %s", query[:30])
            return value
        else:
            del _cache[k]
    return None


def set(query: str, response: str, cards: list):
    """写入缓存"""
    k = _key(query)
    if len(_cache) >= MAX_SIZE:
        _cache.popitem(last=False)  # remove oldest
    _cache[k] = (time.monotonic(), {"response": response, "cards": cards})
    logger.debug("Cache SET: %s", query[:30])


def stats() -> dict:
    """缓存统计"""
    return {"size": len(_cache), "max": MAX_SIZE, "ttl_s": TTL_SECONDS}


def clear():
    _cache.clear()
