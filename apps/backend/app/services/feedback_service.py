"""
反馈记录 — 存储 + 聚合分析
"""
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
    fb = Feedback(
        session_id=session_id,
        product_id=product_id,
        rating=rating,
        reason=reason,
    )
    db.add(fb)
    await db.flush()
    logger.info("Feedback recorded: session=%s, rating=%d, product=%s",
                session_id[:8], rating, product_id)
    return fb
