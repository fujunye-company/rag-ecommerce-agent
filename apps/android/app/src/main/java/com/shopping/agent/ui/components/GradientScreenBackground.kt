package com.shopping.agent.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

/**
 * 顶部条 — 纯色品牌浅蓝，覆盖状态栏。
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
    val barHeight = with(density) { statusBarHeight * 0.1406f }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(statusBarHeight + barHeight)
            .background(Color(0xFFEBF3FC)),
        contentAlignment = Alignment.Center,
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
