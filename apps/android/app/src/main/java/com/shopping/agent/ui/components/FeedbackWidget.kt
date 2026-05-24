package com.shopping.agent.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 反馈组件 — 点赞/点踩，带选中状态
 * 每条 AI 消息后展示
 */
@Composable
fun FeedbackWidget(
    onLike: () -> Unit,
    onDislike: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var feedbackState by remember { mutableStateOf<FeedbackState>(FeedbackState.None) }
    var showReasons by remember { mutableStateOf(false) }

    Column(modifier = modifier.padding(start = 4.dp, top = 4.dp, bottom = 8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            // 点赞按钮
            IconButton(
                onClick = {
                    feedbackState = if (feedbackState == FeedbackState.Liked) FeedbackState.None else FeedbackState.Liked
                    if (feedbackState == FeedbackState.Liked) onLike()
                },
                modifier = Modifier.size(32.dp)
            ) {
                Text(
                    text = "👍",
                    fontSize = if (feedbackState == FeedbackState.Liked) 16.sp else 14.sp
                )
            }

            // 点踩按钮
            IconButton(
                onClick = {
                    if (feedbackState == FeedbackState.Disliked) {
                        feedbackState = FeedbackState.None
                        showReasons = false
                    } else {
                        feedbackState = FeedbackState.Disliked
                        showReasons = true
                    }
                },
                modifier = Modifier.size(32.dp)
            ) {
                Text(
                    text = "👎",
                    fontSize = if (feedbackState == FeedbackState.Disliked) 16.sp else 14.sp
                )
            }

            if (feedbackState == FeedbackState.Liked) {
                Text(
                    text = "感谢反馈！",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }

        // 点踩原因选择
        if (showReasons) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(start = 8.dp, top = 4.dp)
            ) {
                val reasons = listOf("不准确", "不相关", "价格太贵", "已过时")
                reasons.forEach { reason ->
                    AssistChip(
                        onClick = {
                            onDislike(reason)
                            showReasons = false
                        },
                        label = {
                            Text(
                                text = reason,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    )
                }
            }
        }
    }
}

private enum class FeedbackState {
    None, Liked, Disliked
}
