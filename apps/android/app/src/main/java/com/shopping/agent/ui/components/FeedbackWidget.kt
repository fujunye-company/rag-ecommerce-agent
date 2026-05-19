package com.shopping.agent.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * 反馈组件 — 点赞/点踩/原因选择
 */
@Composable
fun FeedbackWidget(
    onLike: () -> Unit,
    onDislike: (String?) -> Unit,
) {
    Row(modifier = Modifier.padding(8.dp)) {
        IconButton(onClick = onLike) {
            Text("👍")
        }
        IconButton(onClick = { onDislike("不准确") }) {
            Text("👎")
        }
    }
}
