package com.shopping.agent.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.shopping.agent.data.mock.MockCategoryProducts
import com.shopping.agent.ui.components.GradientScreenBackground
import com.shopping.agent.ui.components.ProductCard
import com.shopping.agent.ui.components.UnifiedSearchBar

/**
 * P02 商品列表页
 *
 * - 蓝粉渐变背景
 * - 顶部：UnifiedSearchBar（固定）
 * - 分类 Tab 行（吸顶）：横向滚动的 FilterChip 列表
 * - 主体：2 列 LazyVerticalGrid，ProductCard
 */
@Composable
fun CategoryListScreen(
    navController: NavHostController? = null,
) {
    val tabs = MockCategoryProducts.categoryTabs
    var selectedTab by remember { mutableStateOf(tabs.first()) }

    val products = remember(selectedTab) {
        MockCategoryProducts.categoryMap[selectedTab] ?: emptyList()
    }

    GradientScreenBackground {
        Column(modifier = Modifier.fillMaxSize()) {
            // === 顶部：固定搜索栏 ===
            UnifiedSearchBar(
                onClick = { navController?.navigate("chat") },
                modifier = Modifier.padding(top = 8.dp),
            )

            // === 商品网格（tab 吸顶） ===
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // 吸顶分类 Tab 行
                stickyHeader {
                    CategoryTabRow(
                        tabs = tabs,
                        selectedTab = selectedTab,
                        onTabSelected = { selectedTab = it },
                    )
                }

                items(products, key = { it.id }) { product ->
                    ProductCard(
                        name = product.name,
                        price = product.price,
                        imageUrl = product.imageUrl,
                        reason = product.highlights.joinToString(" · "),
                        rating = product.rating,
                        matchScore = product.matchScore,
                        onClick = {
                            navController?.navigate("chat")
                        },
                    )
                }
            }
        }
    }
}

/**
 * 吸顶分类 Tab 行 — 带红色选中下划线
 */
@Composable
private fun CategoryTabRow(
    tabs: List<String>,
    selectedTab: String,
    onTabSelected: (String) -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.background.copy(alpha = 0.92f),
        shadowElevation = 2.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            tabs.forEach { tab ->
                val isSelected = tab == selectedTab
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clickable { onTabSelected(tab) }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                ) {
                    Text(
                        text = tab,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected)
                            Color(0xFFE53935)
                        else
                            MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    // 选中红色下划线，其余灰色
                    Box(
                        modifier = Modifier
                            .width(24.dp)
                            .height(3.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(
                                if (isSelected) Color(0xFFE53935)
                                else Color.Transparent,
                            ),
                    )
                }
            }
        }
    }
}
