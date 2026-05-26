package com.shopping.agent.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.shopping.agent.data.mock.MockExploreProducts
import com.shopping.agent.ui.components.ExploreProductNodeCard
import com.shopping.agent.ui.components.GradientScreenBackground
import com.shopping.agent.ui.components.UnifiedSearchBar

/**
 * P04 探索页（重写）
 *
 * - 蓝粉渐变背景
 * - 顶部标题："探索"
 * - UnifiedSearchBar
 * - 分类胶囊行（FilterChip）
 * - 主体：3 列错位网格 LazyVerticalStaggeredGrid
 * - 30 个探索节点 ExploreProductNodeCard
 */
@Composable
fun ExploreScreen() {
    val chips = MockExploreProducts.exploreChips
    val allNodes = MockExploreProducts.nodes

    var selectedChip by remember { mutableStateOf(chips.first()) }

    // 按分类筛选（"推荐"和"随机"显示全部）
    val filteredNodes = remember(selectedChip) {
        if (selectedChip == "推荐" || selectedChip == "随机") {
            allNodes
        } else {
            allNodes.filter { it.product.category == selectedChip }
        }
    }

    GradientScreenBackground {
        Column(modifier = Modifier.fillMaxSize()) {
            // === 顶部标题 ===
            Text(
                text = "探索",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onPrimary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 20.dp, top = 16.dp, end = 20.dp),
            )

            Spacer(modifier = Modifier.height(4.dp))

            // === 搜索栏 ===
            UnifiedSearchBar(
                onClick = { /* TODO: 搜索跳转 */ },
                modifier = Modifier.padding(top = 4.dp),
            )

            Spacer(modifier = Modifier.height(8.dp))

            // === 分类胶囊行 ===
            ExploreChipRow(
                chips = chips,
                selectedChip = selectedChip,
                onChipSelected = { selectedChip = it },
            )

            Spacer(modifier = Modifier.height(8.dp))

            // === 3 列错位网格 ===
            LazyVerticalStaggeredGrid(
                columns = StaggeredGridCells.Fixed(3),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalItemSpacing = 8.dp,
            ) {
                items(filteredNodes, key = { it.product.id }) { node ->
                    ExploreProductNodeCard(
                        name = node.product.name,
                        price = node.product.price,
                        imageUrl = node.product.imageUrl,
                    )
                }
            }
        }
    }
}

/**
 * 探索页分类胶囊行 — 横向滚动 FilterChip
 */
@Composable
private fun ExploreChipRow(
    chips: List<String>,
    selectedChip: String,
    onChipSelected: (String) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
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
