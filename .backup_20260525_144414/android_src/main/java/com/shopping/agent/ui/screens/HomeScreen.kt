package com.shopping.agent.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.shopping.agent.data.mock.MockHomeProducts
import com.shopping.agent.ui.components.GradientScreenBackground
import com.shopping.agent.ui.components.ProductCard
import com.shopping.agent.ui.components.UnifiedSearchBar

/**
 * P01 首页推荐页
 *
 * 结构：
 * - 蓝粉渐变背景
 * - 顶部：用户问候 + 副标题
 * - 中间：2×2 商品推荐卡片网格
 * - 底部：UnifiedSearchBar（相机 + 输入框 + 搜索）
 * - 点击卡片 → 跳转 ChatScreen
 * - 点击搜索栏 → 跳转 ChatScreen
 */
@Composable
fun HomeScreen(
    navController: NavHostController? = null,
) {
    val products = MockHomeProducts.products

    GradientScreenBackground {
        Column(
            modifier = Modifier.fillMaxSize(),
        ) {
            // === 顶部区域：用户问候 ===
            HomeHeader()

            // === 中间：2×2 商品推荐卡片网格 ===
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
                contentPadding = PaddingValues(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
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

            // === 底部：统一搜索栏 ===
            UnifiedSearchBar(
                onClick = {
                    navController?.navigate("chat")
                },
                modifier = Modifier.padding(bottom = 8.dp),
            )
        }
    }
}

/**
 * 首页顶部问候区域
 */
@Composable
private fun HomeHeader() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, top = 16.dp, end = 20.dp, bottom = 8.dp),
    ) {
        Text(
            text = "你好，fujunye",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onPrimary,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "今天为你推荐",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f),
        )
    }
}
