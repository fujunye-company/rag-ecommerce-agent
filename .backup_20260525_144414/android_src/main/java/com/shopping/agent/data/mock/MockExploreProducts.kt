package com.shopping.agent.data.mock

import com.shopping.agent.data.model.Product

/**
 * P04 探索页 Mock 数据 — 30 个节点商品，随机品类和随机高度
 */
object MockExploreProducts {

    /** 探索节点商品 — 额外携带高度标签（用于错位网格） */
    data class ExploreNode(
        val product: Product,
        val heightDp: Int, // 图片区域高度 dp（模拟错位）
    )

    private val categories = listOf("运动鞋", "袜子", "包袋", "数码", "服饰", "配饰")
    private val brands = listOf("Nike", "Adidas", "Uniqlo", "MUJI", "Stance", "New Balance", "Asics", "Puma", "Herschel", "Topologie")

    val nodes: List<ExploreNode> = List(30) { index ->
        val cat = categories[index % categories.size]
        val brand = brands[index % brands.size]
        // 随机高度 140~260dp
        val height = 140 + ((index * 37 + 11) % 120)
        ExploreNode(
            product = Product(
                id = "explore_${(index + 1).toString().padStart(3, '0')}",
                name = when (index % 5) {
                    0 -> "$brand ${cat}经典款 透气舒适"
                    1 -> "$brand ${cat}联名限定款"
                    2 -> "$brand ${cat}复古运动款"
                    3 -> "$brand ${cat}轻量减震款"
                    else -> "$brand ${cat}城市通勤款"
                },
                price = 199.00 + (index * 73) % 3000,
                imageUrl = "https://picsum.photos/seed/explore${index + 1}/300/${height * 2}",
                brand = brand,
                category = cat,
                rating = 4.0 + (index % 10) * 0.1,
            ),
            heightDp = height,
        )
    }

    /** 探索页分类胶囊 */
    val exploreChips: List<String> = listOf("推荐", "运动鞋", "袜子", "包袋", "数码", "随机")
}
