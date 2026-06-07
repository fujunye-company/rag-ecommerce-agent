package com.shopping.agent.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.shopping.agent.data.model.Product
import com.shopping.agent.ui.theme.*
import kotlinx.coroutines.delay

/**
 * 流式气泡 — Agent 边生成边显示的逐字输出组件
 * 带闪烁光标动画 + 搜索状态提示 + 商品卡片渐进展开
 */
@Composable
fun StreamingBubble(
    text: String,
    isActive: Boolean,
    searchStatus: String,
    productCards: List<Product>,
    onProductTap: (Product) -> Unit,
    modifier: Modifier = Modifier,
) {
    var cursorVisible by remember { mutableStateOf(true) }

    LaunchedEffect(isActive) {
        while (isActive) {
            delay(500)
            cursorVisible = !cursorVisible
        }
    }

    Column(modifier = modifier.widthIn(max = Dimens.messageBubbleMaxWidth)) {
        if (searchStatus.isNotEmpty()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = Dimens.space1, start = Dimens.space1),
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(12.dp),
                    strokeWidth = 1.5.dp,
                    color = Primary,
                )
                Spacer(Modifier.width(Dimens.space2))
                Text(searchStatus, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Surface(shape = AgentBubbleShape, color = MaterialTheme.colorScheme.surface, shadowElevation = 2.dp) {
            Column(modifier = Modifier.padding(Dimens.space3)) {
                Text(
                    text = buildAnnotatedString {
                        append(text)
                        if (isActive && cursorVisible) {
                            withStyle(SpanStyle(color = Primary, fontWeight = FontWeight.Bold)) {
                                append("\u258C")
                            }
                        }
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )

                val groupedCards = productCards.groupBy { it.category }
                var globalIndex = 0
                groupedCards.forEach { (category, products) ->
                    if (groupedCards.size > 1 && category.isNotBlank()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = Dimens.space2),
                        ) {
                            Text(
                                text = "▎$category",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = Primary,
                            )
                        }
                        HorizontalDivider(
                            color = Primary.copy(alpha = 0.2f),
                            modifier = Modifier.padding(top = 2.dp, bottom = 2.dp),
                        )
                    }
                    products.forEach { product ->
                        val index = globalIndex++
                        AnimatedVisibility(
                            visible = true,
                            enter = fadeIn(tween(300, delayMillis = index * 100))
                                    + slideInVertically { it / 2 },
                        ) {
                            ProductCardHorizontal(
                                product = product,
                                onTap = { onProductTap(product) },
                                modifier = Modifier.padding(top = Dimens.space2),
                            )
                        }
                    }
                }
            }
        }
    }
}
