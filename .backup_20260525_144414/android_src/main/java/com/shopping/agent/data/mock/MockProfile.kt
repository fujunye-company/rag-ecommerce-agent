package com.shopping.agent.data.mock

import com.shopping.agent.data.model.Product

/**
 * 购物车单项 — 包含商品与数量
 */
data class CartItem(
    val product: Product,
    val quantity: Int,
)

/**
 * P05 我的页 Mock 数据
 * 用户信息 + 购物车预览 + 订单/优惠券统计
 */
object MockProfile {
    val userName = "fujunye"
    val avatarUrl: String? = null

    /** 购物车 3 件商品 */
    val cartItems: List<CartItem> = listOf(
        CartItem(
            product = Product(
                id = "prod_001",
                name = "Sony WH-1000XM5 无线降噪耳机",
                price = 2299.00,
                imageUrl = "https://picsum.photos/seed/headphone1/400/400",
                highlights = listOf("行业最强降噪", "30小时续航"),
                brand = "Sony",
                category = "耳机",
                rating = 4.8,
            ),
            quantity = 1,
        ),
        CartItem(
            product = Product(
                id = "prod_003",
                name = "Anker 氮化镓 65W 充电器",
                price = 149.00,
                imageUrl = "https://picsum.photos/seed/charger1/400/400",
                highlights = listOf("GaN技术", "三口快充"),
                brand = "Anker",
                category = "配件",
                rating = 4.7,
            ),
            quantity = 2,
        ),
        CartItem(
            product = Product(
                id = "prod_007",
                name = "AirPods Pro 2 (USB-C)",
                price = 1799.00,
                imageUrl = "https://picsum.photos/seed/airpods/400/400",
                highlights = listOf("自适应降噪", "空间音频"),
                brand = "Apple",
                category = "耳机",
                rating = 4.8,
            ),
            quantity = 1,
        ),
    )

    val orderCount = 5
    val couponCount = 3
}
