package com.shopping.agent.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HeadsetMic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.shopping.agent.ui.theme.*
import com.shopping.agent.data.mock.MockProfile

// ===== ProfileHeader 背景色 =====
private val ProfileHeaderBg = Color(0xFFEBF3FC)

/**
 * 用户头部区域 — 蓝粉渐变 + 头像 + 用户名 fujunye + 官方客服 + 设置
 * 设计规约参考: 拾物_我的页UI素材与页面设计说明书 §5.1
 */
@Composable
private fun ProfileHeader(onSettingsClick: () -> Unit = {}) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .background(ProfileHeaderBg),
    ) {
        // 右侧: 客服 + 设置
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 12.dp, end = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            IconButton(onClick = {}, modifier = Modifier.size(40.dp)) {
                Icon(Icons.Default.HeadsetMic, "官方客服", tint = Neutral700, modifier = Modifier.size(24.dp))
            }
            IconButton(onClick = onSettingsClick, modifier = Modifier.size(40.dp)) {
                Icon(Icons.Default.Settings, "设置", tint = Neutral700, modifier = Modifier.size(24.dp))
            }
        }

        // 左侧: 头像 + 用户名（水平排列）
        Row(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // 圆形头像
            Surface(
                modifier = Modifier.size(56.dp),
                shape = CircleShape,
                color = Neutral100,
                shadowElevation = 2.dp,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "海",
                        style = MaterialTheme.typography.headlineSmall,
                        color = Primary,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
            // 用户名
            Text(
                text = "fujunye",
                style = MaterialTheme.typography.titleLarge,
                color = Neutral900,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

/**
 * 购物车预览模块 — 横向商品卡片列表
 * 设计规约: §5.2 CartPreviewSection
 */
@Composable
private fun CartPreviewSection(
    onCartClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onCartClick,
        shape = RadiusLg,
        colors = CardDefaults.cardColors(containerColor = Neutral0),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // 标题行
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "购物车",
                    style = MaterialTheme.typography.titleMedium,
                    color = Neutral900,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    "${MockProfile.cartCount}件商品",
                    style = MaterialTheme.typography.bodySmall,
                    color = Neutral500,
                )
            }
            Spacer(Modifier.height(12.dp))
            // 横向商品卡片列表
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                MockProfile.cartItems.forEach { item ->
                    CartProductMiniCard(
                        title = item.title,
                        price = item.price,
                        imagePlaceholder = Neutral100,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun CartProductMiniCard(
    title: String,
    price: String,
    imagePlaceholder: Color,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        shape = RadiusMd,
        colors = CardDefaults.cardColors(containerColor = imagePlaceholder),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // 商品图占位
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Neutral200),
                contentAlignment = Alignment.Center,
            ) {
                Text("图", style = MaterialTheme.typography.bodySmall, color = Neutral500)
            }
            Spacer(Modifier.height(6.dp))
            Text(
                title,
                style = MaterialTheme.typography.bodySmall,
                color = Neutral700,
                maxLines = 1,
            )
            Text(
                price,
                style = MaterialTheme.typography.labelMedium,
                color = TextPrice,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

/**
 * 订单状态模块 — 5个状态入口
 * 设计规约: §5.3 OrderStatusSection
 */
@Composable
private fun OrderStatusSection(modifier: Modifier = Modifier) {
    Card(
        shape = RadiusLg,
        colors = CardDefaults.cardColors(containerColor = Neutral0),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    "我的订单",
                    style = MaterialTheme.typography.titleMedium,
                    color = Neutral900,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    "全部 >",
                    style = MaterialTheme.typography.bodySmall,
                    color = Primary,
                    modifier = Modifier.clickable {},
                )
            }
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                MockProfile.orderStatuses.forEach { status ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "${status.count}",
                            style = MaterialTheme.typography.titleMedium,
                            color = Neutral900,
                            fontWeight = if (status.count != 0) FontWeight.Bold else FontWeight.Normal,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(status.label, style = MaterialTheme.typography.bodySmall, color = Neutral500)
                    }
                }
            }
        }
    }
}

/**
 * 常用功能 2×3 网格 — 使用 Material Icons 替代 Emoji
 * 设计规约: §5.4 ProfileFeatureGrid
 */
@Composable
private fun ProfileFeatureGrid(modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        val features = MockProfile.features
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            FeatureItem(features[0].title, features[0].subtitle, Icons.Default.Settings, Modifier.weight(1f))
            FeatureItem(features[1].title, features[1].subtitle, Icons.Default.Settings, Modifier.weight(1f))
            FeatureItem(features[2].title, features[2].subtitle, Icons.Default.HeadsetMic, Modifier.weight(1f))
        }
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            FeatureItem(features[3].title, features[3].subtitle, Icons.Default.Settings, Modifier.weight(1f))
            FeatureItem(features[4].title, features[4].subtitle, Icons.Default.Settings, Modifier.weight(1f))
            FeatureItem(features[5].title, features[5].subtitle, Icons.Default.HeadsetMic, Modifier.weight(1f))
        }
    }
}

@Composable
private fun FeatureItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        shape = RadiusLg,
        colors = CardDefaults.cardColors(containerColor = Neutral0),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                icon,
                contentDescription = title,
                tint = Neutral700,
                modifier = Modifier.size(28.dp),
            )
            Spacer(Modifier.height(6.dp))
            Text(title, style = MaterialTheme.typography.bodySmall, color = Neutral700)
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = Neutral400)
        }
    }
}

/**
 * 领券中心 — 浅粉券面 + 红色金额
 * 设计规约: §5.5 CouponCenterSection
 */
@Composable
private fun CouponCenterSection(modifier: Modifier = Modifier) {
    Card(
        shape = RadiusLg,
        colors = CardDefaults.cardColors(containerColor = Neutral0),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "领券中心",
                    style = MaterialTheme.typography.titleMedium,
                    color = Neutral900,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    "更多优惠 >",
                    style = MaterialTheme.typography.bodySmall,
                    color = Primary,
                )
            }
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                MockProfile.coupons.forEach { coupon ->
                    CouponCard(coupon.amount, coupon.label, BrandPink.copy(alpha = 0.15f), Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun CouponCard(
    amount: String,
    label: String,
    bgColor: Color,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        shape = RadiusMd,
        colors = CardDefaults.cardColors(containerColor = bgColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                amount,
                style = MaterialTheme.typography.titleMedium,
                color = TextPrice,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = Neutral600,
            )
        }
    }
}

/**
 * 我的 — 完整页面
 * 设计规约: 拾物_我的页UI素材与页面设计说明书 完整版
 */
@Composable
fun ProfileScreen(
    onSettingsClick: () -> Unit = {},
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Neutral50),
    ) {
        // ===== A01: 用户头部 =====
        ProfileHeader(onSettingsClick = onSettingsClick)

        // ===== 内容区 =====
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // ===== A02: 购物车预览 =====
            CartPreviewSection(onCartClick = {})

            // ===== A03: 我的订单 =====
            OrderStatusSection()

            // ===== A04: 常用功能 =====
            ProfileFeatureGrid()

            // ===== A05: 领券中心 =====
            CouponCenterSection()

            // 底部留白
            Spacer(Modifier.height(24.dp))
        }
    }
}
