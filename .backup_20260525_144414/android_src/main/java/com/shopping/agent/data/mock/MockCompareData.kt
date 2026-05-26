package com.shopping.agent.data.mock

import com.shopping.agent.data.model.Product

/**
 * 比价变体数据 — 同一商品在不同平台/时段的价格历史
 */
data class PriceHistoryPoint(
    val date: String,       // 日期标签，如 "5/1"
    val price: Float,       // 当日价格
)

data class CompareVariant(
    val label: String,                  // 变体标签，如 "深空黑 256GB"
    val priceHistory: List<PriceHistoryPoint>,
    val lowestPrice: Float,             // 历史最低价
    val imageUrl: String,
    val productName: String,
)

/**
 * P03 比价页 Mock 数据
 */
object MockCompareData {

    /** 主商品 — 被比较的主商品 */
    val mainProduct = Product(
        id = "prod_001",
        name = "Sony WH-1000XM5 无线降噪耳机",
        price = 2299.00,
        imageUrl = "https://picsum.photos/seed/headphone1/400/400",
        highlights = listOf("行业最强降噪", "30小时续航"),
        matchScore = 0.95,
        brand = "Sony",
        category = "耳机",
        rating = 4.8,
    )

    /** 分类胶囊 */
    val categoryChips = listOf("颜色", "套装", "容量", "平台")

    /** 各变体的价格趋势 */
    val variants: List<CompareVariant> = listOf(
        // 变体 1 — Sony WH-1000XM5 黑色
        CompareVariant(
            label = "黑色",
            productName = "Sony WH-1000XM5 黑色",
            imageUrl = "https://picsum.photos/seed/headphone1/400/400",
            lowestPrice = 2099f,
            priceHistory = listOf(
                PriceHistoryPoint("5/1", 2499f),
                PriceHistoryPoint("5/3", 2399f),
                PriceHistoryPoint("5/5", 2299f),
                PriceHistoryPoint("5/7", 2199f),
                PriceHistoryPoint("5/9", 2099f),
                PriceHistoryPoint("5/11", 2299f),
                PriceHistoryPoint("5/13", 2399f),
                PriceHistoryPoint("5/15", 2299f),
            ),
        ),
        // 变体 2 — Sony WH-1000XM5 银色
        CompareVariant(
            label = "银色",
            productName = "Sony WH-1000XM5 银色",
            imageUrl = "https://picsum.photos/seed/headphone2/400/400",
            lowestPrice = 2150f,
            priceHistory = listOf(
                PriceHistoryPoint("5/1", 2599f),
                PriceHistoryPoint("5/3", 2499f),
                PriceHistoryPoint("5/5", 2399f),
                PriceHistoryPoint("5/7", 2250f),
                PriceHistoryPoint("5/9", 2150f),
                PriceHistoryPoint("5/11", 2250f),
                PriceHistoryPoint("5/13", 2350f),
                PriceHistoryPoint("5/15", 2299f),
            ),
        ),
        // 变体 3 — 京东自营
        CompareVariant(
            label = "京东自营",
            productName = "Sony WH-1000XM5 京东自营",
            imageUrl = "https://picsum.photos/seed/headphone3/400/400",
            lowestPrice = 1999f,
            priceHistory = listOf(
                PriceHistoryPoint("5/1", 2399f),
                PriceHistoryPoint("5/3", 2299f),
                PriceHistoryPoint("5/5", 2199f),
                PriceHistoryPoint("5/7", 2099f),
                PriceHistoryPoint("5/9", 1999f),
                PriceHistoryPoint("5/11", 2199f),
                PriceHistoryPoint("5/13", 2299f),
                PriceHistoryPoint("5/15", 2399f),
            ),
        ),
        // 变体 4 — 天猫旗舰店
        CompareVariant(
            label = "天猫旗舰",
            productName = "Sony WH-1000XM5 天猫旗舰店",
            imageUrl = "https://picsum.photos/seed/headphone4/400/400",
            lowestPrice = 2199f,
            priceHistory = listOf(
                PriceHistoryPoint("5/1", 2499f),
                PriceHistoryPoint("5/3", 2399f),
                PriceHistoryPoint("5/5", 2299f),
                PriceHistoryPoint("5/7", 2299f),
                PriceHistoryPoint("5/9", 2199f),
                PriceHistoryPoint("5/11", 2299f),
                PriceHistoryPoint("5/13", 2399f),
                PriceHistoryPoint("5/15", 2499f),
            ),
        ),
    )
}
