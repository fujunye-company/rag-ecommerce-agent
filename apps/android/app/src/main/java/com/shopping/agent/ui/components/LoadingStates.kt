package com.shopping.agent.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.shopping.agent.ui.theme.*

// ===== 骨架屏 =====

@Composable
fun ProductCardSkeleton(modifier: Modifier = Modifier) {
    val shimmerColors = listOf(
        SkeletonColor.copy(alpha = 0.6f),
        SkeletonColor.copy(alpha = 0.2f),
        SkeletonColor.copy(alpha = 0.6f),
    )
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmer",
    )
    val brush = Brush.linearGradient(
        colors = shimmerColors,
        start = Offset(translateAnim - 200f, 0f),
        end = Offset(translateAnim, 0f),
    )

    Card(shape = RadiusLg, modifier = modifier.fillMaxWidth()) {
        Column {
            Box(Modifier.fillMaxWidth().aspectRatio(1f).background(brush))
            Column(modifier = Modifier.padding(Dimens.cardPadding)) {
                Box(Modifier.fillMaxWidth(0.8f).height(18.dp).clip(RadiusSm).background(brush))
                Spacer(Modifier.height(Dimens.space1))
                Box(Modifier.fillMaxWidth(0.4f).height(24.dp).clip(RadiusSm).background(brush))
                Spacer(Modifier.height(Dimens.space1))
                Box(Modifier.fillMaxWidth(0.25f).height(16.dp).clip(RadiusSm).background(brush))
            }
        }
    }
}

@Composable
fun ChatSkeleton(modifier: Modifier = Modifier) {
    Column(modifier = modifier.padding(Dimens.pageHorizontal), verticalArrangement = Arrangement.spacedBy(Dimens.space3)) {
        // 用户气泡骨架
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            Box(Modifier.width(200.dp).height(60.dp).clip(RadiusLg).background(SkeletonColor))
        }
        // AI气泡骨架
        Box(Modifier.width(260.dp).height(80.dp).clip(RadiusLg).background(SkeletonColor))
    }
}

// ===== 空状态 =====

@Composable
fun EmptyState(
    title: String,
    subtitle: String,
    onAction: (() -> Unit)? = null,
    actionLabel: String? = null,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(Dimens.space12),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(Dimens.space10))
        Icon(
            Icons.Default.SearchOff, null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(80.dp),
        )
        Spacer(Modifier.height(Dimens.space4))
        Text(title, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.height(Dimens.space2))
        Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (onAction != null && actionLabel != null) {
            Spacer(Modifier.height(Dimens.space6))
            OutlinedButton(onClick = onAction, shape = RadiusFull) {
                Text(actionLabel)
            }
        }
    }
}

// ===== 错误状态 =====

@Composable
fun ErrorState(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(Dimens.space12),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(Dimens.space10))
        Icon(
            Icons.Default.WifiOff, null,
            tint = ErrorColor,
            modifier = Modifier.size(80.dp),
        )
        Spacer(Modifier.height(Dimens.space4))
        Text("网络连接已断开", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.height(Dimens.space2))
        Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(Dimens.space6))
        Button(onClick = onRetry, shape = RadiusFull, colors = ButtonDefaults.buttonColors(containerColor = Primary)) {
            Text("重试", color = OnPrimary)
        }
    }
}
