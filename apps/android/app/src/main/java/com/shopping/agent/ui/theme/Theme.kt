package com.shopping.agent.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary            = Primary,
    onPrimary          = OnPrimary,
    primaryContainer   = PrimaryLight,
    secondary          = Secondary,
    onSecondary        = OnPrimary,
    tertiary           = Tertiary,
    background         = Background,
    onBackground       = OnBackground,
    surface            = Surface,
    onSurface          = OnSurface,
    onSurfaceVariant   = OnSurfaceVariant,
    error              = ErrorColor,
    onError            = OnPrimary,
    outline            = Outline,
    outlineVariant     = OutlineVariant,
)

private val DarkColorScheme = darkColorScheme(
    primary            = DarkPrimary,
    onPrimary          = OnPrimary,
    primaryContainer   = PrimaryDark,
    secondary          = Secondary,
    onSecondary        = OnPrimary,
    background         = DarkBackground,
    onBackground       = Color.White,
    surface            = DarkSurface,
    onSurface          = Color.White,
    onSurfaceVariant   = Neutral400,
    error              = ErrorColor,
    onError            = OnPrimary,
)

@Composable
fun ShoppingTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography  = AppTypography,
        shapes      = AppShapes,
        content     = content,
    )
}
