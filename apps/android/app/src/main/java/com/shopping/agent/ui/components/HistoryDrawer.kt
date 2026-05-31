package com.shopping.agent.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.shopping.agent.data.model.ConversationMeta
import com.shopping.agent.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

/**
 * 挂画式历史侧边栏 — 从数据库加载真实会话列表。
 *
 * 设计规约: 白色大圆角面板从左侧覆盖，右侧保留主页面露出区
 * - 面板宽度: 约 74% 屏幕宽
 * - 会话按月份分组
 * - 支持长按删除
 */
@Composable
fun HistoryDrawer(
    visible: Boolean,
    onDismiss: () -> Unit,
    conversations: List<ConversationMeta>,
    currentConversationId: String,
    onSessionClick: (String) -> Unit,
    onNewChat: () -> Unit,
    onDeleteConversation: (String) -> Unit,
) {
    if (!visible) return

    var searchQuery by remember { mutableStateOf("") }

    val drawerOffsetX by animateDpAsState(
        targetValue = if (visible) 0.dp else (-320).dp,
        animationSpec = tween(300),
        label = "drawer_slide",
    )

    Box(modifier = Modifier.fillMaxSize()) {
        // 遮罩层
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0x33000000))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss,
                ),
        )

        // 挂画式面板
        Surface(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(0.74f)
                .offset(x = drawerOffsetX),
            shape = RoundedCornerShape(
                topStart = 0.dp, topEnd = 32.dp,
                bottomStart = 0.dp, bottomEnd = 32.dp,
            ),
            color = Neutral0,
            shadowElevation = 16.dp,
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                DrawerTopSection(
                    searchQuery = searchQuery,
                    onSearchQueryChange = { searchQuery = it },
                    onNewChat = {
                        onNewChat()
                        onDismiss()
                    },
                )

                HorizontalDivider(color = Neutral100)

                DrawerSessionList(
                    conversations = conversations,
                    searchQuery = searchQuery,
                    currentConversationId = currentConversationId,
                    onSessionClick = { id ->
                        onSessionClick(id)
                        onDismiss()
                    },
                    onDeleteConversation = onDeleteConversation,
                    modifier = Modifier.weight(1f),
                )

                HorizontalDivider(color = Neutral100)
                DrawerUserFooter()
            }
        }
    }
}

@Composable
private fun DrawerTopSection(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onNewChat: () -> Unit,
) {
    Column(modifier = Modifier.padding(16.dp)) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            placeholder = { Text("搜索历史会话", color = Neutral400) },
            leadingIcon = { Icon(Icons.Default.Search, "搜索", tint = Neutral500) },
            singleLine = true,
            shape = RadiusMd,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Primary,
                unfocusedBorderColor = Neutral200,
                focusedContainerColor = Neutral50,
                unfocusedContainerColor = Neutral50,
            ),
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(10.dp))

        OutlinedButton(
            onClick = onNewChat,
            modifier = Modifier.fillMaxWidth(),
            shape = RadiusMd,
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = Neutral0,
                contentColor = Primary,
            ),
            border = BorderStroke(1.dp, Neutral200),
        ) {
            Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("新建对话", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun DrawerSessionList(
    conversations: List<ConversationMeta>,
    searchQuery: String,
    currentConversationId: String,
    onSessionClick: (String) -> Unit,
    onDeleteConversation: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val filtered = if (searchQuery.isBlank()) conversations
    else conversations.filter { it.title.contains(searchQuery, ignoreCase = true) }

    if (filtered.isEmpty()) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                if (searchQuery.isNotBlank()) "未找到匹配的会话" else "暂无历史对话",
                color = Neutral400,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        return
    }

    // 按月份分组
    val dateFormat = SimpleDateFormat("yyyy年M月", Locale.CHINESE)
    val groups = filtered.groupBy { meta ->
        dateFormat.format(Date(meta.updatedAt))
    }

    LazyColumn(modifier = modifier) {
        groups.forEach { (monthLabel, sessions) ->
            item(key = "month_$monthLabel") {
                Text(
                    text = monthLabel,
                    style = MaterialTheme.typography.labelLarge,
                    color = Neutral400,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                )
            }

            items(sessions, key = { it.id }) { session ->
                val isActive = session.id == currentConversationId
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(
                            if (isActive) Modifier.background(PrimaryLight)
                            else Modifier
                        )
                        .clickable { onSessionClick(session.id) }
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = session.title.ifBlank { "新对话" },
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isActive) Primary else Neutral800,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(Modifier.height(2.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = formatSessionTime(session.updatedAt),
                                style = MaterialTheme.typography.bodySmall,
                                color = Neutral500,
                            )
                            if (session.messageCount > 0) {
                                Text(
                                    text = "${session.messageCount} 条消息",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Neutral400,
                                )
                            }
                        }
                    }
                    IconButton(onClick = { onDeleteConversation(session.id) }) {
                        Icon(
                            Icons.Default.Delete,
                            "删除对话",
                            tint = Neutral300,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }

            item(key = "div_$monthLabel") {
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    color = Neutral100,
                )
            }
        }
    }
}

private fun formatSessionTime(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    return when {
        diff < 60_000 -> "刚刚"
        diff < 3600_000 -> "${diff / 60_000} 分钟前"
        diff < 86400_000 -> "${diff / 3600_000} 小时前"
        else -> {
            val fmt = SimpleDateFormat("MM/dd HH:mm", Locale.getDefault())
            fmt.format(Date(timestamp))
        }
    }
}

@Composable
private fun DrawerUserFooter() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {}
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            modifier = Modifier.size(40.dp),
            shape = RoundedCornerShape(50),
            color = Primary,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    "f", color = OnPrimary, fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "fujunye", style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium, color = Neutral900,
            )
            Text("退出登录", style = MaterialTheme.typography.bodySmall, color = Neutral400)
        }
    }
}
