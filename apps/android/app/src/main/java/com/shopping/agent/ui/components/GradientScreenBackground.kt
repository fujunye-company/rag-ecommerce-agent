package com.shopping.agent.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import com.shopping.agent.ui.theme.GradientEnd
import com.shopping.agent.ui.theme.GradientStart

/**
 * 全屏蓝粉渐变背景 — 从上到下蓝色渐变到粉色
 * 接受 content lambda 作为前景内容
 */
@Composable
fun GradientScreenBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(GradientStart, GradientEnd)
                )
            )
    ) {
        content()
    }
}
