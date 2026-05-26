package com.shopping.agent.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shopping.agent.data.mock.MockChats
import com.shopping.agent.data.model.ChatMessage
import com.shopping.agent.data.model.SseEvent
import com.shopping.agent.data.repository.ChatRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 对话 VM — 消息列表 StateFlow, 发送消息, SSE 流式更新, 反馈记录
 */
class ChatViewModel : ViewModel() {
    private val repository = ChatRepository()

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    /** 反馈记录 — key: messageIndex, value: FeedbackEntry */
    data class FeedbackEntry(val liked: Boolean, val reason: String?)
    private val _feedbackMap = mutableMapOf<Int, FeedbackEntry>()

    private var conversationId: String? = null

    /** 是否使用 Mock 模式（当后端不可用时） */
    var useMock: Boolean = false

    init {
        // 默认加载 Mock 聊天历史作为初始展示
        loadMockHistory()
    }

    /**
     * 加载 Mock 历史对话
     */
    fun loadMockHistory() {
        _messages.value = MockChats.defaultMessages
    }

    /**
     * 发送消息
     */
    fun sendMessage(text: String) {
        if (text.isBlank()) return

        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            // 添加用户消息
            val messages = _messages.value.toMutableList()
            messages.add(ChatMessage(role = "user", content = text))
            _messages.value = messages

            // 创建 AI 消息占位 (流式更新)
            val aiMsg = ChatMessage(
                role = "assistant",
                content = "",
                isStreaming = true
            )
            messages.add(aiMsg)
            _messages.value = messages.toList()

            val aiIndex = messages.size - 1
            var aiContent = ""
            var aiProducts = mutableListOf<com.shopping.agent.data.model.Product>()

            try {
                repository.sendMessage(text, conversationId).collect { event ->
                    when (event) {
                        is SseEvent.TextDelta -> {
                            aiContent += event.content
                            messages[aiIndex] = aiMsg.copy(content = aiContent, isStreaming = true)
                            _messages.value = messages.toList()
                        }
                        is SseEvent.ProductCard -> {
                            aiProducts.add(event.product)
                        }
                        is SseEvent.Done -> {
                            messages[aiIndex] = aiMsg.copy(
                                content = aiContent,
                                products = aiProducts.toList(),
                                isStreaming = false
                            )
                            _messages.value = messages.toList()
                            // TODO: 后端应通过 done 事件返回 conversation_id 以支持多轮
                        }
                        is SseEvent.Error -> {
                            _error.value = event.message
                            messages[aiIndex] = aiMsg.copy(
                                content = aiContent.ifEmpty { "抱歉，出了点问题：${event.message}" },
                                isStreaming = false
                            )
                            _messages.value = messages.toList()
                        }
                        is SseEvent.Unknown -> {
                            // 忽略未知事件类型
                        }
                    }

                    // 保存 conversationId (首次从后端响应中获取)
                    // 实际应从 response header 或首条 event 获取
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "网络错误"
                messages[aiIndex] = aiMsg.copy(
                    content = aiContent.ifEmpty { "网络连接失败，请重试" },
                    isStreaming = false
                )
                _messages.value = messages.toList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * 记录用户反馈
     */
    fun recordFeedback(messageIndex: Int, liked: Boolean, reason: String?) {
        _feedbackMap[messageIndex] = FeedbackEntry(liked, reason)
        // TODO: 发送到后端 FeedbackRepository
        viewModelScope.launch {
            // repository.submitFeedback(...)
        }
    }

    /**
     * 获取指定消息的反馈记录
     */
    fun getFeedback(messageIndex: Int): FeedbackEntry? = _feedbackMap[messageIndex]

    fun clearError() {
        _error.value = null
    }

    fun clearMessages() {
        _messages.value = emptyList()
    }
}
