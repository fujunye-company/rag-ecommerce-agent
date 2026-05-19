package com.shopping.agent.viewmodel

import androidx.lifecycle.ViewModel
import com.shopping.agent.data.model.ChatMessage
import com.shopping.agent.data.repository.ChatRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 对话 VM — 消息列表 StateFlow, 发送, SSE 管理
 */
class ChatViewModel : ViewModel() {
    private val repository = ChatRepository()
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    fun sendMessage(text: String) {
        // TODO: 调用 repository.sendMessage + 收集 SSE 事件更新 StateFlow
    }
}
