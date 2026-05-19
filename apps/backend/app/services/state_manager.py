"""
多轮状态管理 — 预算/偏好/已比较商品/反悔修改
"""
import logging
from collections import defaultdict

logger = logging.getLogger("state_manager")

# MVP: 内存字典 (全量: Redis/数据库持久化)
_session_store: dict[str, dict] = defaultdict(dict)


def get_state(session_id: str) -> dict:
    """获取会话状态"""
    return _session_store[session_id]


def update_state(session_id: str, **kwargs) -> dict:
    """更新会话状态 (合并写入)"""
    state = _session_store[session_id]
    state.update(kwargs)
    logger.debug("Session %s state updated: %s", session_id[:8], list(kwargs.keys()))
    return state


def clear_state(session_id: str) -> None:
    """清除会话状态"""
    _session_store.pop(session_id, None)
