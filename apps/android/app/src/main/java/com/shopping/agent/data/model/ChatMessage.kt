package com.shopping.agent.data.model

data class ChatMessage(
    val id: String,
    val role: MessageRole,
    val content: String,
    val productCards: List<Product> = emptyList(),
    val timestamp: Long = System.currentTimeMillis(),
    val status: MessageStatus = MessageStatus.Sent,
    val errorMessage: String? = null,
)

enum class MessageRole { User, Assistant }

enum class MessageStatus { Sending, Sent, Streaming, Error }

data class ConversationMeta(
    val id: String,
    val title: String,
    val messageCount: Int,
    val lastMessage: String,
    val createdAt: Long,
    val updatedAt: Long,
)
