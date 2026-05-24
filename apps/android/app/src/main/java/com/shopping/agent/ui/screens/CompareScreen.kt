package com.shopping.agent.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.shopping.agent.data.mock.MockCompareData
import com.shopping.agent.ui.components.GradientScreenBackground
import com.shopping.agent.ui.components.ProductCardHorizontal
import com.shopping.agent.ui.components.ProductTrendCard

/**
 * P03 比价页
 *
 * 结构：
 * - 蓝粉渐变背景
 * - 顶部：主商品横向卡片（ProductCardHorizontal）
 * - 中部：分类胶囊（颜色/套装/容量等变体）
 * - 下方：LazyColumn 价格趋势卡片列表（ProductTrendCard）
 * - 每张趋势卡：左图 + 右 Canvas 自绘价格折线
 */
@Composable
fun CompareTabScreen() {
    val mainProduct = MockCompareData.mainProduct
    val variants = MockCompareData.variants
    val chips = MockCompareData.categoryChips

    var selectedChip by remember { mutableStateOf(chips.firstOrNull() ?: "") }

    GradientScreenBackground {
        Column(
            modifier = Modifier.fillMaxSize(),
        ) {
            // === 顶部标题 ===
            Text(
                text = "商品比价",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onPrimary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 20.dp, top = 16.dp, end = 20.dp, bottom = 8.dp),
            )

            // === 主商品横向卡片 ===
            ProductCardHorizontal(
                product = mainProduct,
                modifier = Modifier.padding(horizontal = 16.dp),
            )

            Spacer(modifier = Modifier.height(12.dp))

            // === 分类胶囊 ===
            ScrollableChipRow(
                chips = chips,
                selectedChip = selectedChip,
                onChipSelected = { selectedChip = it },
            )

            Spacer(modifier = Modifier.height(8.dp))

            // === 价格趋势卡片列表 ===
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 4.dp),
            ) {
                items(variants) { variant ->
                    ProductTrendCard(
                        variantLabel = variant.label,
                        productName = variant.productName,
                        imageUrl = variant.imageUrl,
                        priceHistory = variant.priceHistory,
                        lowestPrice = variant.lowestPrice,
                    )
                }
            }
        }
    }
}

/**
 * 水平滚动的分类胶囊
 */
@Composable
private fun ScrollableChipRow(
    chips: List<String>,
    selectedChip: String,
    onChipSelected: (String) -> Unit,
) {
    // 使用简单的 Row 水平排列胶囊
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        chips.forEach { chip ->
            val isSelected = chip == selectedChip
            FilterChip(
                selected = isSelected,
                onClick = { onChipSelected(chip) },
                label = {
                    Text(
                        text = chip,
                        style = MaterialTheme.typography.labelMedium,
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                ),
            )
        }
    }
}
