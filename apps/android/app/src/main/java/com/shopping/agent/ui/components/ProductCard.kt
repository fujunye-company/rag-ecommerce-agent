package com.shopping.agent.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * 商品卡片 — MVP: 名称 + 价格 + 图片 + 推荐理由
 * 全量扩展: 标签, 评分, 匹配度
 */
@Composable
fun ProductCard(
    name: String,
    price: Double,
    imageUrl: String,
    reason: String,
    onClick: () -> Unit = {},
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(8.dp),
        onClick = onClick
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(name, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(4.dp))
            Text("¥$price", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(4.dp))
            Text(reason, style = MaterialTheme.typography.bodySmall)
        }
    }
}
