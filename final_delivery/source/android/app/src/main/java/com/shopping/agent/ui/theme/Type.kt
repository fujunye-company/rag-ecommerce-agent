package com.shopping.agent.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val NumberFontFamily = FontFamily.Monospace

val AppTypography = Typography(
    displayLarge = TextStyle(
        fontSize = 28.sp,
        fontWeight = FontWeight.Light,
        lineHeight = 34.sp,
        letterSpacing = (-0.5).sp,
    ),
    headlineLarge = TextStyle(
        fontSize = 22.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 28.sp,
        letterSpacing = (-0.25).sp,
    ),
    headlineMedium = TextStyle(
        fontSize = 20.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 26.sp,
    ),
    titleMedium = TextStyle(
        fontSize = 18.sp,
        fontWeight = FontWeight.Medium,
        lineHeight = 24.sp,
    ),
    bodyLarge = TextStyle(
        fontSize = 17.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 24.sp,
    ),
    bodyMedium = TextStyle(
        fontSize = 14.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 20.sp,
    ),
    bodySmall = TextStyle(
        fontSize = 12.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 16.sp,
    ),
    labelSmall = TextStyle(
        fontSize = 12.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 16.sp,
    ),
)

// ===== 价格专用 TextStyle =====
val PriceLarge = TextStyle(
    fontSize = 24.sp,
    fontWeight = FontWeight.Bold,
    lineHeight = 28.sp,
    fontFamily = NumberFontFamily,
)
val PriceMedium = TextStyle(
    fontSize = 20.sp,
    fontWeight = FontWeight.Bold,
    lineHeight = 24.sp,
    fontFamily = NumberFontFamily,
)
val PriceSmall = TextStyle(
    fontSize = 16.sp,
    fontWeight = FontWeight.Bold,
    lineHeight = 20.sp,
    fontFamily = NumberFontFamily,
)
