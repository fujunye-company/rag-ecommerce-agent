package com.shopping.agent.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * 消息气泡 — 用户/AI, 流式拼接文本
 */
@Composable
fun MessageBubble(
    role: String,
    content: String,
    isStreaming: Boolean = false,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(8.dp),
        horizontalArrangement = if (role == "user") Arrangement.End else Arrangement.Start
    ) {
        Surface(
            color = if (role == "user")
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surfaceVariant,
            shape = MaterialTheme.shapes.medium
        ) {
            Text(
                text = content + if (isStreaming) "▌" else "",
                modifier = Modifier.padding(12.dp)
            )
        }
    }
}
