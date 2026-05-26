package com.shopping.agent.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

private val TopBarGradient = Brush.horizontalGradient(
    colorStops = arrayOf(
        0.0f to Color(0xFFC5D9F0),
        0.5f to Color(0xFFEDE7F0),
        1.0f to Color(0xFFF5D5D8),
    ),
)

/**
 * 顶部渐变条 — 从屏幕 0px 开始（覆盖状态栏），渐变可见高度 = 状态栏×0.75
 *
 * 需配合 Activity.enableEdgeToEdge() 使状态栏透明。
 */
@Composable
fun GradientTopBar(
    modifier: Modifier = Modifier,
    icons: @Composable RowScope.() -> Unit = {},
) {
    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val density = LocalDensity.current
    val gradientHeight = with(density) { statusBarHeight * 0.1406f }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(statusBarHeight + gradientHeight)   // 总高 = 状态栏区域 + 可见渐变
            .background(TopBarGradient),
        contentAlignment = Alignment.Center,             // 图标在总高内居中
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            icons()
        }
    }
}

@Composable
fun PageBody(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(modifier = modifier.fillMaxWidth().background(com.shopping.agent.ui.theme.Neutral50)) { content() }
}
