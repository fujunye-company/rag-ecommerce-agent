package com.shopping.agent.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val RadiusNone  = RoundedCornerShape(0.dp)
val RadiusSm    = RoundedCornerShape(4.dp)
val RadiusMd    = RoundedCornerShape(8.dp)
val RadiusLg    = RoundedCornerShape(12.dp)
val RadiusXl    = RoundedCornerShape(16.dp)
val RadiusFull  = RoundedCornerShape(50)

// 聊天气泡专用
val UserBubbleShape  = RoundedCornerShape(16.dp, 16.dp, 4.dp, 16.dp)
val AgentBubbleShape = RoundedCornerShape(4.dp, 16.dp, 16.dp, 16.dp)

val AppShapes = Shapes(
    small      = RadiusSm,
    medium     = RadiusMd,
    large      = RadiusLg,
    extraLarge = RadiusXl,
)
