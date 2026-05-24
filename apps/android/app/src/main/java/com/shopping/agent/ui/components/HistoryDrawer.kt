package com.shopping.agent.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.shopping.agent.data.mock.HistorySession
import com.shopping.agent.data.mock.MockHistory

/**
 * 历史侧边栏 — ModalDrawerSheet 内容
 *
 * 结构：
 * - 顶部：搜索框 + "新建对话" 按钮
 * - 中部：LazyColumn 按月份分组（每组标题 + 会话列表）
 * - 底部：用户信息 "fujunye" + "退出登录"
 *
 * 点击会话 → 关闭抽屉并导航到 chat
 */
@Composable
fun HistoryDrawer(
    onSessionClick: (HistorySession) -> Unit,
    onNewChat: () -> Unit,
    onLogout: () -> Unit,
) {
    var searchQuery by remember { mutableStateOf("") }

    val allGroups = MockHistory.sessions
    val filteredGroups = if (searchQuery.isBlank()) {
        allGroups
    } else {
        allGroups.mapNotNull { group ->
            val filtered = group.sessions.filter {
                it.title.contains(searchQuery, ignoreCase = true)
            }
            if (filtered.isNotEmpty()) group.copy(sessions = filtered) else null
        }
    }

    ModalDrawerSheet(
        modifier = Modifier.width(300.dp),
    ) {
        // === 顶部：搜索框 + 新建对话 ===
        Column(
            modifier = Modifier.padding(16.dp),
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("搜索历史会话") },
                leadingIcon = {
                    Icon(Icons.Outlined.Search, contentDescription = "搜索")
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(
                onClick = onNewChat,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Outlined.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("新建对话")
            }
        }

        HorizontalDivider()

        // === 中部：按月份分组的会话列表 ===
        if (filteredGroups.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "未找到相关会话",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
            ) {
                filteredGroups.forEach { group ->
                    // 月份标题
                    item(key = "header_${group.monthLabel}") {
                        Text(
                            text = group.monthLabel,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(
                                horizontal = 16.dp,
                                vertical = 10.dp,
                            ),
                        )
                    }

                    // 会话列表
                    items(group.sessions, key = { it.id }) { session ->
                        HistorySessionItem(
                            session = session,
                            onClick = { onSessionClick(session) },
                        )
                    }

                    // 分组间分割线
                    item(key = "divider_${group.monthLabel}") {
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    }
                }
            }
        }

        HorizontalDivider()

        // === 底部：用户信息 + 退出登录 ===
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onLogout)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // 用户头像 — 圆形蓝色占位
            Surface(
                modifier = Modifier.size(36.dp),
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.primary,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "f",
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "fujunye",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    text = "退出登录",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/**
 * 单条历史会话项
 * 标题 + 时间 + 标签 chip
 */
@Composable
private fun HistorySessionItem(
    session: HistorySession,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        // 标题 + 时间
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = session.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = session.time,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // 标签 chips
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            session.tags.forEach { tag ->
                SuggestionChip(
                    onClick = {},
                    label = {
                        Text(
                            text = tag,
                            style = MaterialTheme.typography.labelSmall,
                        )
                    },
                )
            }
        }
    }
}
