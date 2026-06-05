package com.shopping.agent.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * 统一搜索栏 — 首页底部搜索入口
 * 白色圆角胶囊形：左侧相机图标 + 中间提示文字 + 右侧搜索图标
 */
@Composable
fun UnifiedSearchBar(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "搜索你想要的商品",
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .height(48.dp)
            .clip(RoundedCornerShape(24.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        color = Color.White,
        shadowElevation = 4.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // 左侧：相机图标
            Icon(
                imageVector = Icons.Default.CameraAlt,
                contentDescription = "拍照搜索",
                tint = Color.Gray,
                modifier = Modifier.size(22.dp),
            )

            Spacer(modifier = Modifier.width(12.dp))

            // 中间：提示文字
            Text(
                text = placeholder,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Start,
            )

            // 右侧：搜索图标
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "搜索",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}
