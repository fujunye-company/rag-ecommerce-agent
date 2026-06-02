package com.shopping.agent.ui.screens

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.shopping.agent.data.local.UserRepository
import com.shopping.agent.ui.theme.*
import com.shopping.agent.data.mock.MockProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** 头像图片最大尺寸（像素），与 ProfileEditScreen 保持一致 */
private const val MAX_AVATAR_DIM = 480

/**
 * 计算 Bitmap 降采样比例因子，返回值总是 2 的幂。
 */
private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
    val (height, width) = options.outHeight to options.outWidth
    var inSampleSize = 1
    if (height > reqHeight || width > reqWidth) {
        val halfHeight = height / 2
        val halfWidth = width / 2
        while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
            inSampleSize *= 2
        }
    }
    return inSampleSize
}

// ===== ProfileHeader 背景色 =====
private val ProfileHeaderBg = Color(0xFFEBF3FC)

/**
 * 用户头部区域 — 蓝粉渐变 + 头像 + 用户名 + 官方客服 + 设置。
 *
 * 头像优先显示从 SQLite 加载的用户上传图片，无头像时展示"海"字默认占位符。
 *
 * @param onSettingsClick 设置按钮点击回调
 * @param avatarBitmap 用户头像 ImageBitmap，可为 null 以显示默认占位符
 * @param nickname 用户显示昵称，为空时回退默认值 "fujunye"
 */
@Composable
private fun ProfileHeader(
    onSettingsClick: () -> Unit = {},
    onCustomerServiceClick: () -> Unit = {},
    avatarBitmap: ImageBitmap? = null,
    nickname: String = "fujunye",
) {
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
            IconButton(onClick = onCustomerServiceClick, modifier = Modifier.size(40.dp)) {
                Icon(Icons.Default.HeadsetMic, "官方客服", tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(24.dp))
            }
            IconButton(onClick = onSettingsClick, modifier = Modifier.size(40.dp)) {
                Icon(Icons.Default.Settings, "设置", tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(24.dp))
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
                color = MaterialTheme.colorScheme.outlineVariant,
                shadowElevation = 2.dp,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    val bmp = avatarBitmap
                    if (bmp != null) {
                        Image(
                            bitmap = bmp,
                            contentDescription = "头像",
                            modifier = Modifier.fillMaxSize().clip(CircleShape),
                            contentScale = ContentScale.Crop,
                        )
                    } else {
                        Text(
                            text = "海",
                            style = MaterialTheme.typography.headlineSmall,
                            color = Primary,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
            // 用户名
            Text(
                text = nickname,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
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
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
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
                    color = MaterialTheme.colorScheme.onSurface,
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
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = if (status.count != 0) FontWeight.Bold else FontWeight.Normal,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(status.label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                icon,
                contentDescription = title,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(28.dp),
            )
            Spacer(Modifier.height(6.dp))
            Text(title, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
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
                    color = MaterialTheme.colorScheme.onSurface,
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
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * 我的 — 完整页面。
 *
 * 启动时从 SQLite 加载用户头像并传递给 [ProfileHeader] 展示。
 * 子模块：用户头部、购物车预览、我的订单、常用功能 2×3 网格、领券中心。
 *
 * @param onSettingsClick 设置图标点击回调，导航到设置页
 * @param onCustomerServiceClick 客服图标点击回调，导航到客服页面
 */
@Composable
fun ProfileScreen(
    onSettingsClick: () -> Unit = {},
    onCustomerServiceClick: () -> Unit = {},
) {
    val context = LocalContext.current
    val repository = remember { UserRepository(context) }
    var avatarBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    var displayName by remember { mutableStateOf("fujunye") }

    // 懒加载用户头像和昵称：DB 读取均在 IO 线程异步执行
    LaunchedEffect(Unit) {
        try {
            val avatarBytes = withContext(Dispatchers.IO) { repository.getUserAvatar() }
            if (avatarBytes != null && avatarBytes.isNotEmpty()) {
                val androidBmp = withContext(Dispatchers.IO) {
                    val opts = BitmapFactory.Options().apply {
                        inJustDecodeBounds = true
                    }
                    BitmapFactory.decodeByteArray(avatarBytes, 0, avatarBytes.size, opts)
                    opts.inSampleSize = calculateInSampleSize(opts, MAX_AVATAR_DIM, MAX_AVATAR_DIM)
                    opts.inJustDecodeBounds = false
                    opts.inPreferredConfig = android.graphics.Bitmap.Config.ARGB_8888
                    BitmapFactory.decodeByteArray(avatarBytes, 0, avatarBytes.size, opts)
                }
                if (androidBmp != null) {
                    avatarBitmap = androidBmp.asImageBitmap()
                }
            }
        } catch (_: Exception) {
            avatarBitmap = null
        }
    }

    // 懒加载昵称：从 SQLite 读取，为空则回退默认值 "fujunye"
    LaunchedEffect(Unit) {
        try {
            val profile = withContext(Dispatchers.IO) { repository.getUserProfile() }
            val name = profile["nickname"]?.takeIf { it.isNotEmpty() } ?: "fujunye"
            displayName = name
        } catch (_: Exception) {
            displayName = "fujunye"
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        // ===== A01: 用户头部 =====
        ProfileHeader(onSettingsClick = onSettingsClick, onCustomerServiceClick = onCustomerServiceClick, avatarBitmap = avatarBitmap, nickname = displayName)

        // ===== 内容区 =====
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // ===== A02: 我的订单 =====
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
