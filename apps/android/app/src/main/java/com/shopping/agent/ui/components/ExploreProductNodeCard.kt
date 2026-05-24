package com.shopping.agent.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

/**
 * P04 探索节点卡片 — 圆角方形图片 + 商品名 + 价格
 *
 * - 图片上方 72dp 圆角方形
 * - 下方：商品名（12sp, 1行省略）+ 价格（红色, 14sp）
 * - 整体 120dp 宽，白色背景、8dp 圆角
 */
@Composable
fun ExploreProductNodeCard(
    name: String,
    price: Double,
    imageUrl: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    androidx.compose.material3.Card(
        modifier = modifier.width(120.dp),
        shape = RoundedCornerShape(8.dp),
        elevation = androidx.compose.material3.CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = Color.White),
        onClick = onClick,
    ) {
        Column {
            // 圆角方形图片区域
            AsyncImage(
                model = imageUrl,
                contentDescription = name,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp)
                    .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)),
                contentScale = ContentScale.Crop,
            )

            // 文字区域
            Column(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            ) {
                Text(
                    text = name,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = Color.DarkGray,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "¥$price",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Red,
                )
            }
        }
    }
}
