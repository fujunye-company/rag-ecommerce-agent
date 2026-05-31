package com.shopping.agent.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.shopping.agent.data.model.CartItem
import com.shopping.agent.ui.components.GradientTopBar
import com.shopping.agent.ui.theme.*
import com.shopping.agent.viewmodel.CartViewModel

@Composable
fun CartScreen(
    viewModel: CartViewModel = viewModel(),
    onBack: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) { viewModel.loadCart() }

    Column(modifier = Modifier.fillMaxSize().background(Neutral50)) {
        // 顶栏
        GradientTopBar {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("购物车", style = MaterialTheme.typography.titleMedium, color = Neutral900)
                if (uiState.items.isNotEmpty()) {
                    TextButton(onClick = { viewModel.clearCart() }) {
                        Text("清空", color = ErrorColor)
                    }
                }
            }
        }

        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Primary)
            }
        } else if (uiState.items.isEmpty()) {
            // 空购物车
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.ShoppingCart, "空购物车",
                        tint = Neutral300, modifier = Modifier.size(64.dp)
                    )
                    Spacer(Modifier.height(12.dp))
                    Text("购物车为空", color = Neutral400, style = MaterialTheme.typography.bodyLarge)
                    Text("去首页逛逛 AI 导购推荐吧", color = Neutral400, style = MaterialTheme.typography.bodySmall)
                }
            }
        } else {
            // 商品列表
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(uiState.items, key = { it.product.productId }) { item ->
                    CartItemCard(
                        item = item,
                        onAdd = { viewModel.updateQuantity(item.product.productId, item.quantity + 1) },
                        onRemove = { viewModel.updateQuantity(item.product.productId, item.quantity - 1) },
                        onDelete = { viewModel.removeFromCart(item.product.productId) },
                    )
                }
            }

            // 底部结算栏
            Surface(
                shadowElevation = 8.dp,
                color = Neutral0,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .fillMaxWidth()
                        .navigationBarsPadding(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("合计", color = Neutral500, style = MaterialTheme.typography.bodySmall)
                        Text(
                            "¥%.2f".format(uiState.totalPrice),
                            color = Primary,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    Button(
                        onClick = { viewModel.placeOrder() },
                        shape = RadiusFull,
                        colors = ButtonDefaults.buttonColors(containerColor = Primary),
                        modifier = Modifier.height(48.dp),
                        enabled = !uiState.isLoading,
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(color = OnPrimary, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Text("结算 (${viewModel.itemCount})", color = OnPrimary)
                        }
                    }
                }
            }
        }

        // 下单成功弹窗
        if (uiState.orderResult != null) {
            Dialog(onDismissRequest = { viewModel.dismissOrderResult() }) {
                Surface(shape = RoundedCornerShape(16.dp), color = Neutral0) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(Icons.Default.CheckCircle, null, tint = Primary, modifier = Modifier.size(48.dp))
                        Spacer(Modifier.height(12.dp))
                        Text("下单成功！", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            uiState.orderResult!!,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Neutral700,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = { viewModel.dismissOrderResult() },
                            shape = RadiusFull,
                            colors = ButtonDefaults.buttonColors(containerColor = Primary),
                        ) {
                            Text("完成", color = OnPrimary)
                        }
                    }
                }
            }
        }

    }
}

@Composable
private fun CartItemCard(
    item: CartItem,
    onAdd: () -> Unit,
    onRemove: () -> Unit,
    onDelete: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Neutral0,
        shadowElevation = 1.dp,
    ) {
        Row(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 商品图
            AsyncImage(
                model = item.product.imageUrl ?: "",
                contentDescription = item.product.title,
                modifier = Modifier.size(72.dp).clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop,
            )
            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    item.product.title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Neutral900,
                    maxLines = 2,
                )
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "¥${item.product.price}",
                        style = MaterialTheme.typography.titleMedium,
                        color = Primary,
                        fontWeight = FontWeight.Bold,
                    )
                    if (item.quantity > 1) {
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "×${item.quantity}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Neutral500,
                        )
                    }
                }
            }

            // 数量控制
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onRemove,
                    modifier = Modifier.size(32.dp),
                ) {
                    Icon(Icons.Default.Remove, "减少", tint = Neutral500, modifier = Modifier.size(18.dp))
                }
                Text(
                    "${item.quantity}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Neutral900,
                    modifier = Modifier.width(28.dp),
                    textAlign = TextAlign.Center,
                )
                IconButton(
                    onClick = onAdd,
                    modifier = Modifier.size(32.dp),
                ) {
                    Icon(Icons.Default.Add, "增加", tint = Primary, modifier = Modifier.size(18.dp))
                }
            }

            // 删除
            IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.Delete, "删除", tint = Neutral400, modifier = Modifier.size(20.dp))
            }
        }
    }
}
