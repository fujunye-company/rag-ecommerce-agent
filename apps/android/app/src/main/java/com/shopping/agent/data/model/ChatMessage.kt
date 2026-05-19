package com.shopping.agent.data.model

/**
 * 消息 DTO — role, content, products, timestamp
 */
data class ChatMessage(
    val role: String,        // "user" | "assistant"
    val content: String,
    val products: List<Product> = emptyList(),
    val timestamp: Long = System.currentTimeMillis(),
    val isStreaming: Boolean = false,
)
