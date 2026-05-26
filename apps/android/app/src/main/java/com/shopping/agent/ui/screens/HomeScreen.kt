package com.shopping.agent.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.shopping.agent.ui.components.*
import com.shopping.agent.ui.navigation.LocalOnMenuClick
import com.shopping.agent.ui.theme.*
import com.shopping.agent.viewmodel.ChatViewModel
import com.shopping.agent.viewmodel.GuideUiState

@Composable
fun HomeScreen(
    chatViewModel: ChatViewModel,
) {
    val uiState by chatViewModel.uiState.collectAsState()
    var initialized by remember { mutableStateOf(false) }

    // 模拟 Agent 发送问候 (仅首次)
    LaunchedEffect(Unit) {
        if (!initialized) {
            initialized = true
            chatViewModel.sendDailyGreeting()
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(Neutral50)) {
        // ===== 超薄渐变条: 仅状态栏+图标行 =====
        GradientTopBar(icons = {
            IconButton(onClick = LocalOnMenuClick.current, modifier = Modifier.size(34.dp)) {
                Icon(Icons.Default.Menu, "菜单", tint = Neutral700, modifier = Modifier.size(26.dp))
            }
            Row {
                IconButton(onClick = {}, modifier = Modifier.size(34.dp)) {
                    Icon(Icons.Default.Call, "电话", tint = Neutral700, modifier = Modifier.size(26.dp))
                }
                IconButton(onClick = {}, modifier = Modifier.size(34.dp)) {
                    Icon(Icons.Default.Notifications, "通知", tint = Neutral700, modifier = Modifier.size(26.dp))
                }
            }
        })

        // ===== 消息/内容区 (占满剩余空间) =====
        if (uiState.messages.isEmpty() && !uiState.isStreaming) {
            // 空态
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Text("输入商品需求，AI 为你精准推荐",
                    style = MaterialTheme.typography.bodyLarge, color = Neutral500)
            }
        } else {
            ChatMessageList(uiState = uiState, chatViewModel = chatViewModel,
                modifier = Modifier.weight(1f))
        }

        // ===== 底部输入栏 =====
        ChatInputBar(
            chatViewModel = chatViewModel,
            placeholder = "输入商品需求…",
            showIcons = true,
        )
    }
}

@Composable
private fun ChatMessageList(
    uiState: GuideUiState,
    chatViewModel: ChatViewModel,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    LaunchedEffect(uiState.messages.size, uiState.streamingText) {
        val c = listState.layoutInfo.totalItemsCount
        if (c > 0) listState.animateScrollToItem(c - 1)
    }
    LazyColumn(state = listState, modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = Dimens.pageHorizontal, vertical = Dimens.space2),
        verticalArrangement = Arrangement.spacedBy(Dimens.space3)) {
        items(uiState.messages, key = { it.id }) { msg ->
            MessageBubble(message = msg, onProductTap = {})
        }
        if (uiState.isStreaming) {
            item(key = "streaming") {
                StreamingBubble(text = uiState.streamingText, isActive = true,
                    searchStatus = uiState.searchStatus,
                    productCards = uiState.streamingCards, onProductTap = {})
            }
        }
        if (uiState.clarifyChips.isNotEmpty()) {
            item(key = "clarify") {
                Row(horizontalArrangement = Arrangement.spacedBy(Dimens.space2)) {
                    uiState.clarifyChips.forEach { chip ->
                        AssistChip(onClick = { chatViewModel.onClarifyChipClick(chip) },
                            label = { Text(chip) }, shape = RadiusFull,
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = PrimaryLight, labelColor = Primary))
                    }
                }
            }
        }
    }
}
