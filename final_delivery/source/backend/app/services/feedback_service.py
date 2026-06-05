"""
反馈记录 — 存储 + 聚合分析
"""
import uuid
import logging
from sqlalchemy.ext.asyncio import AsyncSession
from app.models.feedback import Feedback

logger = logging.getLogger("feedback_service")


async def record_feedback(
    db: AsyncSession,
    session_id: str,
    rating: int,
    product_id: str | None = None,
    reason: str | None = None,
) -> Feedback:
    """记录一条用户反馈"""
    try:
        sid = uuid.UUID(session_id)
    except (ValueError, AttributeError):
        sid = uuid.uuid4()

    pid = None
    if product_id:
        try:
            pid = uuid.UUID(product_id)
        except (ValueError, AttributeError):
            pid = None

    fb = Feedback(
        session_id=sid,
        product_id=pid,
        rating=rating,
        reason=reason,
    )
    db.add(fb)
    await db.flush()
    logger.info("Feedback recorded: session=%s, rating=%d", str(sid)[:8], rating)
    return fb
