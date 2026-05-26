package com.shopping.agent.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.shopping.agent.data.model.Product
import com.shopping.agent.ui.theme.*

@Composable
fun ProductCard(
    product: Product,
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(onClick = onTap, shape = RadiusLg,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = Neutral0),
        modifier = modifier.fillMaxWidth()) {
        Column {
            Box {
                AsyncImage(model = product.imageUrl, contentDescription = product.title,
                    modifier = Modifier.fillMaxWidth().aspectRatio(1f),
                    contentScale = ContentScale.Crop)
                Surface(Modifier.align(Alignment.BottomEnd).padding(8.dp),
                    shape = CircleShape, color = Neutral0.copy(alpha = 0.9f)) {
                    Icon(Icons.Default.ChevronRight, "详情",
                        tint = Neutral500, modifier = Modifier.size(24.dp))
                }
            }
            Column(modifier = Modifier.padding(Dimens.cardPadding)) {
                if (product.attributes.isNotEmpty()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        product.attributes.values.take(3).forEach { tag ->
                            Surface(shape = RadiusSm, color = Neutral100) {
                                Text(tag, Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.bodySmall, color = Neutral600)
                            }
                        }
                    }
                    Spacer(Modifier.height(Dimens.space1))
                }
                Text(product.title, style = MaterialTheme.typography.titleMedium,
                    color = Neutral900, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(Dimens.space1))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text("到手价", style = MaterialTheme.typography.bodySmall, color = Neutral500)
                    Spacer(Modifier.width(4.dp))
                    Text("¥${product.price}", style = PriceMedium.copy(fontWeight = FontWeight.Bold),
                        color = TextPrice)
                    if (product.price != null && product.price > product.price) {
                        Spacer(Modifier.width(8.dp))
                        Text("¥${product.price}",
                            style = MaterialTheme.typography.bodySmall.copy(
                                textDecoration = TextDecoration.LineThrough),
                            color = Neutral400)
                    }
                }
                Spacer(Modifier.height(Dimens.space1))
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Surface(shape = RadiusSm, color = Color(0xFFEBF3FC)) {
                        Text(product.source, Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.bodySmall, color = Info)
                    }
                    if (product.ratingCount != null) {
                        Text(formatSalesCount(product.ratingCount),
                            style = MaterialTheme.typography.bodySmall, color = Neutral500)
                    }
                }
            }
        }
    }
}

fun formatSalesCount(count: Int): String = when {
    count >= 10000 -> "${count / 10000}.${(count % 10000) / 1000}万人付款"
    else -> "${count}人付款"
}
