package com.shopping.agent.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * 历史会话 — 会话列表, 复盘查看 [全量预留]
 */
@Composable
fun HistoryScreen() {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("历史会话", style = MaterialTheme.typography.headlineMedium)
        Text("全量阶段开发", style = MaterialTheme.typography.bodyLarge)
    }
}
