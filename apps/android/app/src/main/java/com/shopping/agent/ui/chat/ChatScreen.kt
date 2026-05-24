package com.shopping.agent.ui.chat

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.shopping.agent.data.model.ChatMessage
import com.shopping.agent.ui.components.ChatInputBar
import com.shopping.agent.ui.components.FeedbackWidget
import com.shopping.agent.ui.components.GradientScreenBackground
import com.shopping.agent.ui.components.ProductCardHorizontal

// 气泡颜色常量
private val UserBubbleColor = Color(0xFFE3F2FD)   // 浅蓝
private val AiBubbleColor = Color(0xFFFCE4EC)      // 浅粉

/**
 * 聊天主界面 — 消息列表 + 横向商品卡 + 气泡 + 输入栏 + 反馈
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    messages: List<ChatMessage>,
    isLoading: Boolean,
    error: String?,
    onSendMessage: (String) -> Unit,
    onFeedback: (messageIndex: Int, liked: Boolean, reason: String?) -> Unit = { _, _, _ -> },
    onClearError: () -> Unit
) {
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    GradientScreenBackground {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("AI导购") },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = Color.White
                    )
                )
            },
            containerColor = Color.Transparent
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                // 错误提示
                error?.let {
                    Snackbar(
                        modifier = Modifier.padding(8.dp),
                        action = {
                            TextButton(onClick = onClearError) { Text("关闭") }
                        }
                    ) { Text(it) }
                }

                // 消息列表
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    state = listState,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(messages.size) { index ->
                        val message = messages[index]
                        val isUser = message.role == "user"

                        if (isUser) {
                            UserMessageBubble(message)
                        } else {
                            AiMessageBubble(
                                message = message,
                                messageIndex = index,
                                onFeedback = { liked, reason ->
                                    onFeedback(index, liked, reason)
                                }
                            )
                        }
                    }

                    // 正在输入指示器
                    if (isLoading) {
                        item {
                            TypingIndicator()
                        }
                    }
                }

                // 输入栏
                ChatInputBar(
                    value = inputText,
                    onValueChange = { inputText = it },
                    onSend = {
                        if (inputText.isNotBlank()) {
                            onSendMessage(inputText)
                            inputText = ""
                        }
                    },
                    enabled = !isLoading,
                    onCameraClick = {
                        // TODO: 后续接拍照功能
                    }
                )
            }
        }
    }
}

// ─── 用户消息气泡 ─────────────────────────────────────────────

@Composable
private fun UserMessageBubble(message: ChatMessage) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End
    ) {
        Surface(
            color = UserBubbleColor,
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = 16.dp,
                bottomEnd = 4.dp
            ),
            modifier = Modifier.widthIn(max = 280.dp),
            shadowElevation = 1.dp
        ) {
            Text(
                text = message.content,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF1A237E)
            )
        }
    }
}

// ─── AI 消息气泡 ──────────────────────────────────────────────

@Composable
private fun AiMessageBubble(
    message: ChatMessage,
    messageIndex: Int,
    onFeedback: (liked: Boolean, reason: String?) -> Unit
) {
    // 闪烁光标动画
    val infiniteTransition = rememberInfiniteTransition(label = "cursor")
    val cursorAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(500),
            repeatMode = RepeatMode.Restart
        ),
        label = "cursor_alpha"
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.Start
    ) {
        // 文本气泡
        Surface(
            color = AiBubbleColor,
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = 4.dp,
                bottomEnd = 16.dp
            ),
            modifier = Modifier.widthIn(max = 300.dp),
            shadowElevation = 1.dp
        ) {
            val content = buildAnnotatedString {
                append(message.content.ifEmpty { "思考中..." })
                if (message.isStreaming) {
                    withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary.copy(alpha = cursorAlpha))) {
                        append(" ▌")
                    }
                }
            }
            Text(
                text = content,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                style = MaterialTheme.typography.bodyMedium
            )
        }

        // 横向商品卡片行
        if (message.products.isNotEmpty()) {
            Spacer(modifier = Modifier.height(6.dp))
            LazyRow(
                contentPadding = PaddingValues(horizontal = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(message.products) { product ->
                    ProductCardHorizontal(
                        product = product,
                        onClick = {
                            // TODO: 导航到商品详情
                        }
                    )
                }
            }
        }

        // 反馈组件 — 仅对已完成（非流式）的 AI 消息展示
        if (!message.isStreaming && message.content.isNotBlank()) {
            FeedbackWidget(
                onLike = { onFeedback(true, null) },
                onDislike = { reason -> onFeedback(false, reason) }
            )
        }
    }
}

// ─── 正在输入指示器 ───────────────────────────────────────────

@Composable
private fun TypingIndicator() {
    // 三个圆点的闪烁动画
    val dot1Alpha = rememberInfiniteTransition(label = "dot1").let {
        it.animateFloat(0.3f, 1f, animationSpec = infiniteRepeatable(tween(600), RepeatMode.Reverse), label = "d1")
    }
    val dot2Alpha = rememberInfiniteTransition(label = "dot2").let {
        it.animateFloat(0.3f, 1f, animationSpec = infiniteRepeatable(tween(600, 200), RepeatMode.Reverse), label = "d2")
    }
    val dot3Alpha = rememberInfiniteTransition(label = "dot3").let {
        it.animateFloat(0.3f, 1f, animationSpec = infiniteRepeatable(tween(600, 400), RepeatMode.Reverse), label = "d3")
    }

    Row(
        modifier = Modifier
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(AiBubbleColor)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("AI正在输入", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        listOf(dot1Alpha, dot2Alpha, dot3Alpha).forEach { alpha ->
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(Color.Gray.copy(alpha = alpha.value))
            )
        }
    }
}
