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
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.shopping.agent.data.mock.HistorySession
import com.shopping.agent.data.mock.MockHistory
import com.shopping.agent.ui.theme.*

/**
 * 挂画式历史侧边栏 — 白色大圆角面板从左侧覆盖，右侧保留主页面露出区
 *
 * 设计规约: 拾物_历史对话挂画式侧边栏UI素材与页面设计说明书
 * - 左侧面板: 白色背景, 大圆角(右上32dp+右下32dp), 轻阴影
 * - 面板宽度: 约 72-76% 屏幕宽
 * - 右侧: 保留主页面露出 (MainContentPeekArea) 作为关闭热区
 * - 顶部: 搜索框 + 新建对话按钮
 * - 主体: 历史会话按月份分组
 * - 底部: 用户头像 + fujunye + 更多
 */
@Composable
fun HistoryDrawer(
    visible: Boolean,
    onDismiss: () -> Unit,
    onSessionClick: (HistorySession) -> Unit,
    onNewChat: () -> Unit,
) {
    if (!visible) return

    // 动画: drawer 从左侧滑入
    val drawerOffsetX by animateDpAsState(
        targetValue = if (visible) 0.dp else (-320).dp,
        animationSpec = tween(300),
        label = "drawer_slide",
    )

    Box(modifier = Modifier.fillMaxSize()) {
        // ===== 遮罩层 (右侧主页面露出区 + 关闭热区) =====
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

        // ===== 挂画式面板 =====
        Surface(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(0.74f)  // 约 74% 屏幕宽
                .offset(x = drawerOffsetX),
            shape = RoundedCornerShape(
                topStart = 0.dp,
                topEnd = 32.dp,
                bottomStart = 0.dp,
                bottomEnd = 32.dp,
            ),
            color = Neutral0,
            shadowElevation = 16.dp,
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // === 顶部: 搜索框 + 新建对话 ===
                DrawerTopSection(
                    onNewChat = {
                        onNewChat()
                        onDismiss()
                    },
                )

                HorizontalDivider(color = Neutral100)

                // === 主体: 历史会话列表 ===
                DrawerSessionList(
                    onSessionClick = { session ->
                        onSessionClick(session)
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f),
                )

                HorizontalDivider(color = Neutral100)

                // === 底部: 用户信息 ===
                DrawerUserFooter()
            }
        }
    }
}

@Composable
private fun DrawerTopSection(onNewChat: () -> Unit) {
    var searchQuery by remember { mutableStateOf("") }

    Column(modifier = Modifier.padding(16.dp)) {
        // 搜索框
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("搜索历史会话", color = Neutral400) },
            leadingIcon = {
                Icon(Icons.Default.Search, "搜索", tint = Neutral500)
            },
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

        // 新建对话按钮
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
    onSessionClick: (HistorySession) -> Unit,
    modifier: Modifier = Modifier,
) {
    val groups = MockHistory.sessions

    LazyColumn(modifier = modifier) {
        groups.forEach { group ->
            // 月份标题
            item(key = "month_${group.monthLabel}") {
                Text(
                    text = group.monthLabel,
                    style = MaterialTheme.typography.labelLarge,
                    color = Neutral400,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                )
            }

            // 会话列表
            items(group.sessions, key = { it.id }) { session ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSessionClick(session) }
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = session.title,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Neutral800,
                            fontWeight = FontWeight.Medium,
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = session.time,
                            style = MaterialTheme.typography.bodySmall,
                            color = Neutral500,
                        )
                    }
                    Icon(
                        Icons.Default.ChevronRight,
                        "恢复对话",
                        tint = Neutral300,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }

            // 分组间分割线
            item(key = "div_${group.monthLabel}") {
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    color = Neutral100,
                )
            }
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
        // 用户头像
        Surface(
            modifier = Modifier.size(40.dp),
            shape = RoundedCornerShape(50),
            color = Primary,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    "f",
                    color = OnPrimary,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                "fujunye",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = Neutral900,
            )
            Text(
                "退出登录",
                style = MaterialTheme.typography.bodySmall,
                color = Neutral400,
            )
        }
    }
}
