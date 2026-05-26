package com.shopping.agent.ui.chat

import androidx.compose.runtime.Composable
import com.shopping.agent.data.model.ChatMessage
import com.shopping.agent.data.model.Product

/**
 * 旧版 ChatScreen — 已由 ChatGuideScreen 替代
 * 保留兼容，内部委托给 ChatGuideScreen
 */
@Composable
fun ChatScreen(
    messages: List<ChatMessage> = emptyList(),
    isLoading: Boolean = false,
    error: String? = null,
    onSendMessage: (String) -> Unit = {},
    onClearError: () -> Unit = {},
    onFeedback: (String, Int, Boolean) -> Unit = { _, _, _ -> },
) {
    ChatGuideScreen()
}
