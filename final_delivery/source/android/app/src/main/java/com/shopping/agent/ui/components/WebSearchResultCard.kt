package com.shopping.agent.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.shopping.agent.data.model.WebSearchItem
import com.shopping.agent.ui.theme.*

@Composable
fun WebSearchResultCard(
    item: WebSearchItem,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable {
                try {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(item.url)))
                } catch (_: Exception) { }
            },
        shape = RadiusMd,
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(containerColor = InfoLight),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                // 序号角标
                Surface(
                    shape = RadiusSm,
                    color = Info,
                    modifier = Modifier.size(20.dp),
                ) {
                    Text(
                        text = "${item.index}",
                        style = MaterialTheme.typography.bodySmall,
                        color = OnPrimary,
                        modifier = Modifier.wrapContentSize(),
                    )
                }
                Spacer(Modifier.width(8.dp))
                // 标题
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = Neutral900,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(6.dp))
            // 摘要
            Text(
                text = item.snippet,
                style = MaterialTheme.typography.bodySmall,
                color = Neutral600,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(4.dp))
            // URL 标签
            Text(
                text = item.url.removePrefix("https://").removePrefix("http://").split("/").firstOrNull() ?: item.url,
                style = MaterialTheme.typography.bodySmall,
                color = Info,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
