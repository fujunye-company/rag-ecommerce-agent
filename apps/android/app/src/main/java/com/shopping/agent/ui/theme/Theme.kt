package com.shopping.agent.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color

/**
 * 全局主题包装器
 * 从 LocalThemeState 读取深色模式开关状态，用 animateColorAsState 对颜色做渐变过渡
 * 不创建新的 composition 子树，避免导航状态丢失
 */
@Composable
fun ShoppingTheme(
    content: @Composable () -> Unit
) {
    val themeState = LocalThemeState.current
    val isDarkMode = themeState.isDarkMode.value

    val animSpec = tween<Color>(durationMillis = 400)

    val primary by animateColorAsState(
        targetValue = if (isDarkMode) DarkPrimary else Primary, animationSpec = animSpec, label = "primary"
    )
    val primaryContainer by animateColorAsState(
        targetValue = if (isDarkMode) PrimaryDark else PrimaryLight, animationSpec = animSpec, label = "primaryContainer"
    )
    val background by animateColorAsState(
        targetValue = if (isDarkMode) DarkBackground else Background, animationSpec = animSpec, label = "background"
    )
    val onBackground by animateColorAsState(
        targetValue = if (isDarkMode) Color.White else OnBackground, animationSpec = animSpec, label = "onBackground"
    )
    val surface by animateColorAsState(
        targetValue = if (isDarkMode) DarkSurface else Surface, animationSpec = animSpec, label = "surface"
    )
    val onSurface by animateColorAsState(
        targetValue = if (isDarkMode) Color.White else OnSurface, animationSpec = animSpec, label = "onSurface"
    )
    val onSurfaceVariant by animateColorAsState(
        targetValue = if (isDarkMode) Neutral400 else OnSurfaceVariant, animationSpec = animSpec, label = "onSurfaceVariant"
    )
    val outline by animateColorAsState(
        targetValue = if (isDarkMode) Neutral500 else Outline, animationSpec = animSpec, label = "outline"
    )
    val outlineVariant by animateColorAsState(
        targetValue = if (isDarkMode) Neutral700 else OutlineVariant, animationSpec = animSpec, label = "outlineVariant"
    )

    val animatedScheme = lightColorScheme(
        primary            = primary,
        onPrimary          = OnPrimary,
        primaryContainer   = primaryContainer,
        secondary          = Secondary,
        onSecondary        = OnPrimary,
        tertiary           = Tertiary,
        background         = background,
        onBackground       = onBackground,
        surface            = surface,
        onSurface          = onSurface,
        onSurfaceVariant   = onSurfaceVariant,
        error              = ErrorColor,
        onError            = OnPrimary,
        outline            = outline,
        outlineVariant     = outlineVariant,
    )

    MaterialTheme(
        colorScheme = animatedScheme,
        typography  = AppTypography,
        shapes      = AppShapes,
        content     = content,
    )
}
