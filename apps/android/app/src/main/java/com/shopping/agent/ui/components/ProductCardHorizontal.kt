package com.shopping.agent.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import coil.compose.AsyncImage
import com.shopping.agent.data.model.Product
import com.shopping.agent.ui.theme.*
import androidx.compose.ui.unit.dp

@Composable
fun ProductCardHorizontal(
    product: Product,
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onTap,
        shape = RadiusLg,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = Neutral0),
        modifier = modifier.fillMaxWidth().height(120.dp),
    ) {
        Row(modifier = Modifier.padding(Dimens.cardPadding)) {
            AsyncImage(
                model = product.imageUrl,
                contentDescription = product.title,
                modifier = Modifier
                    .size(Dimens.productCardImageSize)
                    .then(Modifier),
                contentScale = ContentScale.Crop,
            )
            Spacer(Modifier.width(Dimens.space3))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = product.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = Neutral900,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.weight(1f))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("¥${product.price}", style = PriceSmall, color = TextPrice)
                    if (product.price != null && product.price > product.price) {
                        Spacer(Modifier.width(Dimens.space2))
                        Text(
                            "¥${product.price}",
                            style = MaterialTheme.typography.bodySmall.copy(
                                textDecoration = TextDecoration.LineThrough,
                            ),
                            color = Neutral500,
                        )
                    }
                }
                Spacer(Modifier.height(Dimens.space1))
                Row(horizontalArrangement = Arrangement.spacedBy(Dimens.space2)) {
                    ProductSourceBadge(source = product.source)
                    if (product.matchScore > 0) {
                        Surface(shape = RadiusSm, color = Neutral100) {
                            Text(
                                "匹配 ${(product.matchScore * 100).toInt()}%",
                                style = MaterialTheme.typography.bodySmall,
                                color = Neutral600,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            )
                        }
                    }
                    if (product.rankReason != null) {
                        Text(
                            "✓ ${product.rankReason}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Success,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ProductSourceBadge(source: String) {
    Surface(shape = RadiusSm, color = InfoLight) {
        Text(
            text = source,
            style = MaterialTheme.typography.bodySmall,
            color = Info,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
        )
    }
}
