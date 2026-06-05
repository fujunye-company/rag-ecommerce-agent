package com.shopping.agent.ui.components

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.shopping.agent.data.model.ChatMessage
import com.shopping.agent.data.model.MessageRole
import com.shopping.agent.data.model.MessageStatus
import com.shopping.agent.data.model.Product
import com.shopping.agent.ui.theme.*

private val UserBubbleBg @Composable get() = if (isSystemInDarkTheme()) Color(0xFF1A3A5C) else Color(0xFFE3F0FD)

@Composable
fun MessageBubble(
    message: ChatMessage,
    onProductTap: (Product) -> Unit,
    onRetry: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val isUser = message.role == MessageRole.User

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
    ) {
        Column(
            modifier = Modifier.widthIn(max = if (isUser) 280.dp else Dimens.messageBubbleMaxWidth),
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start,
        ) {
            Surface(
                shape = if (isUser) UserBubbleShape else AgentBubbleShape,
                color = if (isUser) UserBubbleBg else MaterialTheme.colorScheme.surface,
                shadowElevation = if (isUser) 0.dp else 2.dp,
            ) {
                Column(modifier = Modifier.padding(Dimens.space3)) {
                    Text(
                        text = message.content,
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (isUser) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface,
                    )
                    message.productCards.forEach { product ->
                        Spacer(Modifier.height(Dimens.space2))
                        ProductCardHorizontal(product = product, onTap = { onProductTap(product) })
                    }
                    message.webSearchResults.forEach { item ->
                        Spacer(Modifier.height(Dimens.space2))
                        WebSearchResultCard(item = item)
                    }
                    if (message.compareDimensions.isNotEmpty()) {
                        Spacer(Modifier.height(Dimens.space2))
                        CompareDimensionsCard(dimensions = message.compareDimensions)
                    }
                }
            }
            if (onRetry != null && message.status == MessageStatus.Error) {
                Spacer(Modifier.height(Dimens.space1))
                TextButton(onClick = onRetry) { Text("重试", color = ErrorColor) }
            }
        }
    }
}

@Composable
fun CompareDimensionsCard(dimensions: List<Map<String, Any?>>) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(modifier = Modifier.padding(Dimens.space2)) {
            dimensions.forEach { dim ->
                val name = dim["name"] as? String ?: return@forEach
                val values = dim["values"] as? Map<String, Any?> ?: emptyMap()
                val winner = dim["winner"] as? String

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = name,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    if (winner != null) {
                        Spacer(Modifier.width(6.dp))
                        Surface(
                            shape = MaterialTheme.shapes.extraSmall,
                            color = MaterialTheme.colorScheme.primaryContainer,
                        ) {
                            Text(
                                text = "🏆 $winner",
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.tertiary,
                            )
                        }
                    }
                }
                values.forEach { (productId, value) ->
                    Text(
                        text = "$productId: ${value ?: ""}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (dim != dimensions.last()) {
                    Spacer(Modifier.height(6.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(Modifier.height(4.dp))
                }
            }
        }
    }
}
