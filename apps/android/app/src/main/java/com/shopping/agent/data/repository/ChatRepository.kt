package com.shopping.agent.data.repository

import com.shopping.agent.data.model.ChatMessage
import com.shopping.agent.data.model.SseEvent
import com.shopping.agent.data.remote.SseClient
import kotlinx.coroutines.flow.Flow

/**
 * 对话仓库 — 发消息, 管理 SSE, 缓存消息
 */
class ChatRepository {
    private val sseClient = SseClient()
    private val _messages = mutableListOf<ChatMessage>()
    val messages: List<ChatMessage> get() = _messages.toList()

    fun sendMessage(text: String): Flow<SseEvent> {
        _messages.add(ChatMessage(role = "user", content = text))
        return sseClient.connect(text)
    }

    fun addAssistantMessage(content: String, products: List<Any> = emptyList()) {
        _messages.add(ChatMessage(role = "assistant", content = content, isStreaming = false))
    }
}
