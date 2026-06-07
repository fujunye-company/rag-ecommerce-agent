package com.shopping.agent.ui.components

import android.media.MediaPlayer
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
    val hasAudio = message.audioUri != null

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
                    if (hasAudio) {
                        VoiceMessageContent(message = message)
                    } else {
                        Text(
                            text = message.content,
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (isUser) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface,
                        )
                    }
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
private fun VoiceMessageContent(message: ChatMessage) {
    var isPlaying by remember { mutableStateOf(false) }
    val mediaPlayer = remember { mutableStateOf<MediaPlayer?>(null) }
    val context = androidx.compose.ui.platform.LocalContext.current
    DisposableEffect(Unit) { onDispose { try { mediaPlayer.value?.release() } catch (_: Exception) {} } }
    Row(verticalAlignment = Alignment.CenterVertically) {
        FilledIconButton(onClick = {
            if (isPlaying) {
                try { mediaPlayer.value?.stop() } catch (_: Exception) {}
                try { mediaPlayer.value?.reset() } catch (_: Exception) {}
                isPlaying = false
            } else {
                try { mediaPlayer.value?.release() } catch (_: Exception) {}
                val mp = MediaPlayer().apply {
                    try {
                        setDataSource(message.audioUri)
                        prepare()
                        setOnCompletionListener { isPlaying = false; try { release() } catch (_: Exception) {} }
                        start()
                    } catch (e: Exception) {
                        try { release() } catch (_: Exception) {}
                        android.widget.Toast.makeText(context, "音频播放失败", android.widget.Toast.LENGTH_SHORT).show()
                        return@FilledIconButton
                    }
                }
                mediaPlayer.value = mp
                isPlaying = true
            }
        }, modifier = Modifier.size(36.dp),
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = if (isPlaying) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            )
        ) {
            Icon(if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow, contentDescription = if (isPlaying) "停止" else "播放", modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(10.dp))
        Text(text = message.content, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
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
                @Suppress("UNCHECKED_CAST")
                val name = dim["name"] as? String ?: return@forEach
                @Suppress("UNCHECKED_CAST")
                val values = dim["values"] as? Map<String, Any?> ?: emptyMap()
                val winner = dim["winner"] as? String

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = name, style = MaterialTheme.typography.labelMedium,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    if (winner != null) {
                        Spacer(Modifier.width(6.dp))
                        Surface(shape = MaterialTheme.shapes.extraSmall, color = MaterialTheme.colorScheme.primaryContainer) {
                            Text(text = "🏆 $winner", modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.tertiary)
                        }
                    }
                }
                values.forEach { (productId, value) ->
                    Text(text = "$productId: ${value ?: ""}", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
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
