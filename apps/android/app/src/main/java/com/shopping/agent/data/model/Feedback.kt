package com.shopping.agent.data.model

/**
 * 反馈 DTO — rating, reason, product_id
 */
data class Feedback(
    val sessionId: String,
    val rating: Int,         // 1 = 赞, -1 = 踩
    val productId: String? = null,
    val reason: String? = null,
)
