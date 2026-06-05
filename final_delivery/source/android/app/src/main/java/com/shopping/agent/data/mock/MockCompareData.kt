package com.shopping.agent.data.mock

import com.shopping.agent.data.model.Product

/** 比价 Mock — 复用 MockProducts 多品类 */
object MockCompareData {
    val products: List<Product> = mockProducts.filter {
        it.category in listOf("运动鞋", "数码", "箱包", "家居", "美妆", "食品", "宠物")
    }

    val categories = listOf("推荐", "运动鞋", "数码", "箱包", "家居", "美妆", "食品", "宠物", "全部")
    val defaultCategory = "推荐"

    data class PlatformTrend(
        val platform: String,
        val lowestPrice: Double,
        val trend: List<Float>,
    )
    val platformTrends = listOf(
        PlatformTrend("抖音商城", 299.0, listOf(399f, 375f, 360f, 348f, 335f, 318f, 299f)),
        PlatformTrend("天猫",     309.0, listOf(420f, 389f, 370f, 356f, 340f, 312f, 309f)),
        PlatformTrend("京东",     329.0, listOf(420f, 400f, 380f, 360f, 345f, 335f, 329f)),
        PlatformTrend("得物",     319.0, listOf(400f, 380f, 360f, 340f, 330f, 325f, 319f)),
    )
}
