package com.shopping.agent.ui.theme

import androidx.compose.ui.graphics.Color

// ===== 品牌色 — 蓝粉渐变 (原型设计) =====
val BrandBlue = Color(0xFF4A90D9)
val BrandPink = Color(0xFFE8917E)
val GradientStart = Color(0xFF4A90D9)
val GradientEnd   = Color(0xFFE8917E)
val OnPrimary     = Color(0xFFFFFFFF)

// ===== 主色 (Material3) =====
val Primary       = BrandBlue
val PrimaryLight  = Color(0xFFEBF3FC)
val PrimaryDark   = Color(0xFF3A7BC8)

// ===== 辅助色 =====
val Success       = Color(0xFF2ECC71)
val SuccessLight  = Color(0xFFE8F8EF)
val Warning       = Color(0xFFF39C12)
val WarningLight  = Color(0xFFFFF8E8)
val Info          = Color(0xFF4A90D9)
val InfoLight     = Color(0xFFEBF3FC)
val ErrorColor    = Color(0xFFE74C3C)
val ErrorLight    = Color(0xFFFDEDEC)

// ===== 中性色 =====
val Neutral0      = Color(0xFFFFFFFF)
val Neutral50     = Color(0xFFF8F9FA)
val Neutral100    = Color(0xFFF0F0F0)
val Neutral200    = Color(0xFFE0E0E0)
val Neutral300    = Color(0xFFCCCCCC)
val Neutral400    = Color(0xFFAAAAAA)
val Neutral500    = Color(0xFF888888)
val Neutral600    = Color(0xFF666666)
val Neutral700    = Color(0xFF444444)
val Neutral800    = Color(0xFF222222)
val Neutral900    = Color(0xFF111111)

// ===== 语义色 =====
val TextPrice     = Color(0xFFFF5C5C)  // 珊瑚红 — 仅价格
val SourceTagColor = Info
val RatingStar    = Color(0xFFFFB800)
val SkeletonColor = Neutral200
val OverlayColor  = Color(0x66000000)

// ===== Material3 语义映射 =====
val Secondary     = BrandPink
val Tertiary      = Success
val Background    = Neutral50
val Surface       = Neutral0
val OnBackground  = Neutral900
val OnSurface     = Neutral900
val OnSurfaceVariant = Neutral500
val Outline       = Neutral200
val OutlineVariant = Neutral100

// ===== 向后兼容别名 =====
val CardWhite     = Neutral0
val PriceRed      = TextPrice

// ===== 暗色模式预留 =====
val DarkPrimary   = Color(0xFF7AB8FF)
val DarkBackground = Color(0xFF1A1A1A)
val DarkSurface   = Color(0xFF2C2C2C)
