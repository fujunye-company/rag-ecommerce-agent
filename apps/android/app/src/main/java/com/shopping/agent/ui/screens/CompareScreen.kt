package com.shopping.agent.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * 对比页 — 多商品横向对比表 [全量预留]
 */
@Composable
fun CompareScreen() {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("商品对比", style = MaterialTheme.typography.headlineMedium)
        Text("全量阶段开发", style = MaterialTheme.typography.bodyLarge)
    }
}
