package com.shopping.agent.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shopping.agent.ui.theme.BrandBlue

/**
 * 对话页输入栏 — 相机按钮 + 文本输入 + 发送按钮
 * 圆角 24dp，白色背景带阴影，发送按钮有内容时高亮蓝色
 */
@Composable
fun ChatInputBar(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    onCameraClick: () -> Unit = {},
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .shadow(4.dp, RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        color = Color.White
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 相机按钮 — 后续接拍照功能
            IconButton(
                onClick = onCameraClick,
                modifier = Modifier.size(40.dp)
            ) {
                Text("📷", fontSize = 20.sp)
            }

            // 文本输入框
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.weight(1f),
                placeholder = {
                    Text(
                        "输入您的问题...",
                        color = Color.Gray,
                        style = TextStyle(fontSize = 14.sp)
                    )
                },
                maxLines = 3,
                textStyle = TextStyle(fontSize = 14.sp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent
                ),
                enabled = enabled
            )

            // 发送按钮 — 有内容时高亮蓝色
            val hasContent = value.isNotBlank()
            IconButton(
                onClick = {
                    if (hasContent) onSend()
                },
                modifier = Modifier.size(40.dp),
                enabled = hasContent && enabled
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(
                            color = if (hasContent) BrandBlue else Color(0xFFE0E0E0),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "↑",
                        color = if (hasContent) Color.White else Color(0xFF9E9E9E),
                        fontSize = 18.sp
                    )
                }
            }
        }
    }
}
