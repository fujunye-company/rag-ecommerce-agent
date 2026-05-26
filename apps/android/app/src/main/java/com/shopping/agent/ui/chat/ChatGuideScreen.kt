@file:Suppress("DEPRECATION")

package com.shopping.agent.ui.chat

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.shopping.agent.data.model.Product
import com.shopping.agent.ui.components.*
import com.shopping.agent.ui.theme.*
import com.shopping.agent.viewmodel.ChatViewModel
import com.shopping.agent.viewmodel.GuideUiState
import com.shopping.agent.viewmodel.ScreenState

/**
 * @deprecated Use HomeScreen directly — ChatGuideScreen is being consolidated.
 * StreamingBubble moved to components/StreamingBubble.kt.
 * This file is kept as a thin wrapper for backward compatibility.
 */
@Deprecated(
    message = "Use HomeScreen instead. StreamingBubble extracted to components.",
    replaceWith = ReplaceWith("HomeScreen()", "com.shopping.agent.ui.screens.HomeScreen"),
)
@Composable
fun ChatGuideScreen(
    viewModel: ChatViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(modifier = Modifier.fillMaxSize().background(Neutral50)) {
        GradientTopBar {
            Text(
                "AI 智能导购",
                style = MaterialTheme.typography.titleMedium,
                color = Neutral900,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            )
        }

        Box(modifier = Modifier.weight(1f)) {
            when (uiState.screenState) {
                is ScreenState.Idle -> WelcomeContent(
                    onChipClick = viewModel::onClarifyChipClick,
                )
                is ScreenState.Error -> ErrorState(
                    message = (uiState.screenState as ScreenState.Error).message,
                    onRetry = viewModel::retryLastMessage,
                )
                else -> ChatContent(
                    uiState = uiState,
                    onProductTap = {},
                    onChipClick = viewModel::onClarifyChipClick,
                )
            }
        }

        ChatInputBar(
            text = uiState.inputText,
            onTextChange = viewModel::onInputChange,
            onSend = viewModel::sendMessage,
            enabled = !uiState.isStreaming,
        )
    }
}

@Composable
private fun WelcomeContent(onChipClick: (String) -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(Dimens.space12),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(Dimens.space10))
        Text("想买什么？我帮你找", style = MaterialTheme.typography.headlineMedium, color = Neutral900)
        Spacer(Modifier.height(Dimens.space2))
        Text("输入商品需求，AI 为你精准推荐", style = MaterialTheme.typography.bodyLarge, color = Neutral500)
        Spacer(Modifier.height(Dimens.space8))
        Row(horizontalArrangement = Arrangement.spacedBy(Dimens.space2)) {
            listOf("推荐跑鞋", "200元耳机", "保温杯").forEach { chip ->
                AssistChip(
                    onClick = { onChipClick(chip) },
                    label = { Text(chip) },
                    shape = RadiusFull,
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = PrimaryLight,
                        labelColor = Primary,
                    ),
                )
            }
        }
    }
}

@Composable
private fun ChatContent(
    uiState: GuideUiState,
    onProductTap: (Product) -> Unit,
    onChipClick: (String) -> Unit,
) {
    val listState = rememberLazyListState()

    LaunchedEffect(uiState.messages.size, uiState.streamingText) {
        if (uiState.messages.isNotEmpty() || uiState.streamingText.isNotEmpty()) {
            listState.animateScrollToItem(maxOf(0, listState.layoutInfo.totalItemsCount - 1))
        }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = Dimens.pageHorizontal, vertical = Dimens.space3),
        verticalArrangement = Arrangement.spacedBy(Dimens.space3),
    ) {
        items(uiState.messages, key = { it.id }) { msg ->
            MessageBubble(message = msg, onProductTap = onProductTap)
        }

        if (uiState.isStreaming) {
            item(key = "streaming") {
                StreamingBubble(
                    text = uiState.streamingText,
                    isActive = true,
                    searchStatus = uiState.searchStatus,
                    productCards = uiState.streamingCards,
                    onProductTap = onProductTap,
                )
            }
        }

        if (uiState.clarifyChips.isNotEmpty()) {
            item(key = "clarify") {
                Row(horizontalArrangement = Arrangement.spacedBy(Dimens.space2)) {
                    uiState.clarifyChips.forEach { chip ->
                        AssistChip(
                            onClick = { onChipClick(chip) },
                            label = { Text(chip) },
                            shape = RadiusFull,
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = PrimaryLight,
                                labelColor = Primary,
                            ),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatInputBar(
    text: String,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    enabled: Boolean,
) {
    Surface(shadowElevation = 3.dp, color = Neutral0) {
        Row(
            modifier = Modifier
                .padding(horizontal = Dimens.space3, vertical = Dimens.space2)
                .fillMaxWidth()
                .navigationBarsPadding(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = text,
                onValueChange = onTextChange,
                enabled = enabled,
                placeholder = { Text("点击发送或长按说话…", color = Neutral400) },
                textStyle = MaterialTheme.typography.bodyLarge,
                shape = RadiusFull,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Neutral100,
                    unfocusedContainerColor = Neutral100,
                    focusedBorderColor = Primary,
                    unfocusedBorderColor = Neutral100,
                ),
                modifier = Modifier.weight(1f),
                maxLines = 4,
            )
            Spacer(Modifier.width(Dimens.space2))
            FilledIconButton(
                onClick = onSend,
                enabled = text.isNotBlank() && enabled,
                shape = CircleShape,
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = Primary,
                    disabledContainerColor = Neutral200,
                ),
                modifier = Modifier.size(48.dp),
            ) {
                Icon(Icons.Default.Send, "发送", tint = OnPrimary)
            }
        }
    }
}

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("出了点问题", style = MaterialTheme.typography.titleMedium, color = Neutral700)
        Spacer(Modifier.height(8.dp))
        Text(message, style = MaterialTheme.typography.bodyMedium, color = Neutral500)
        Spacer(Modifier.height(16.dp))
        OutlinedButton(onClick = onRetry) { Text("重试") }
    }
}
