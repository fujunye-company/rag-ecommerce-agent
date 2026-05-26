package com.shopping.agent.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.shopping.agent.data.mock.MockProfile
import com.shopping.agent.ui.components.CartPreviewSection
import com.shopping.agent.ui.components.GradientScreenBackground
import com.shopping.agent.ui.theme.CardWhite

/**
 * P05 我的页 — 用户信息 + 购物车预览 + 功能入口 + 退出登录
 *
 * 结构：
 * - 蓝粉渐变背景
 * - 顶部：圆形蓝色头像 + "fujunye" + "查看资料 >"
 * - 购物车预览区（白色卡片，横向滚动，空状态 "🛒 购物车是空的"）
 * - 功能入口列表（白色卡片）：我的订单 | 优惠券 | 收藏 | 浏览历史 | 设置
 * - 底部：退出登录（灰色文字按钮）
 */
@Composable
fun ProfileScreen(
    onViewProfile: () -> Unit = {},
    onViewCart: () -> Unit = {},
    onViewOrders: () -> Unit = {},
    onViewCoupons: () -> Unit = {},
    onViewFavorites: () -> Unit = {},
    onViewHistory: () -> Unit = {},
    onSettings: () -> Unit = {},
    onLogout: () -> Unit = {},
) {
    GradientScreenBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // === 顶部用户信息 ===
            ProfileHeader(onViewProfile = onViewProfile)

            Spacer(modifier = Modifier.height(16.dp))

            // === 购物车预览区（白色卡片）===
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = CardWhite),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            ) {
                CartPreviewSection(
                    cartItems = MockProfile.cartItems,
                    onViewAll = onViewCart,
                    modifier = Modifier.padding(vertical = 12.dp),
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // === 功能入口列表（白色卡片）===
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = CardWhite),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            ) {
                Column {
                    ProfileMenuItem(
                        icon = Icons.Outlined.ReceiptLong,
                        label = "我的订单",
                        badge = "${MockProfile.orderCount}",
                        onClick = onViewOrders,
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    ProfileMenuItem(
                        icon = Icons.Outlined.ConfirmationNumber,
                        label = "优惠券",
                        badge = "${MockProfile.couponCount}张",
                        onClick = onViewCoupons,
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    ProfileMenuItem(
                        icon = Icons.Outlined.FavoriteBorder,
                        label = "收藏",
                        onClick = onViewFavorites,
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    ProfileMenuItem(
                        icon = Icons.Outlined.History,
                        label = "浏览历史",
                        onClick = onViewHistory,
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    ProfileMenuItem(
                        icon = Icons.Outlined.Settings,
                        label = "设置",
                        onClick = onSettings,
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // === 退出登录 ===
            TextButton(
                onClick = onLogout,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            ) {
                Text(
                    text = "退出登录",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

/**
 * 用户头像 + 用户名 + 查看资料
 */
@Composable
private fun ProfileHeader(
    onViewProfile: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onViewProfile)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // 圆形蓝色头像占位
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "f",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onPrimary,
                fontWeight = FontWeight.Bold,
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = MockProfile.userName,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimary,
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "查看资料 >",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
        )
    }
}

/**
 * 功能入口单项 — 图标 + 文字 + 箭头（可选 badge）
 */
@Composable
private fun ProfileMenuItem(
    icon: ImageVector,
    label: String,
    badge: String? = null,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = MaterialTheme.colorScheme.onSurface,
        )

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )

        if (badge != null) {
            Text(
                text = badge,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(modifier = Modifier.width(4.dp))

        Icon(
            imageVector = Icons.Outlined.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp),
        )
    }
}
