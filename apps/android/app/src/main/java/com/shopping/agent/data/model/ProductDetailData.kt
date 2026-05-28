package com.shopping.agent.data.model

/**
 * 商品详情页 UI 数据 — 对齐设计说明书 §8 Mock 结构
 * 严禁出现"淘宝""天猫"字样，统一用"拾物/拾物认证/拾物精选/拾物礼金"
 */
data class ProductDetailData(
    val productId: String,
    val pageTitle: String = "拾物",
    val images: List<String>,
    val campaign: CampaignInfo? = null,
    val price: PriceInfo,
    val title: String,
    val tags: List<String> = emptyList(),
    val coupons: List<CouponInfo> = emptyList(),
    val delivery: DeliveryInfo,
    val guarantee: List<String> = emptyList(),
    val specs: List<SpecItem>,
    val reviews: ReviewSummary,
    val shop: ShopInfo,
)

data class CampaignInfo(
    val title: String,
    val subsidyText: String = "",
    val specBubble: String = "",
)

data class PriceInfo(
    val current: Double,
    val couponPrice: Double? = null,
    val origin: Double? = null,
    val savedAmount: Int = 0,
    val salesText: String = "",
)

data class CouponInfo(
    val type: String,       // "coupon" / "subsidy"
    val text: String,
)

data class DeliveryInfo(
    val estimate: String,
    val location: String,
    val shipping: String,
)

data class SpecItem(
    val label: String,
    val value: String,
)

data class ReviewSummary(
    val count: Int,
    val goodRate: String,
    val keywords: List<String> = emptyList(),
)

data class ShopInfo(
    val name: String,
    val badge: String = "拾物认证",
    val score: Double,
    val fans: String = "",
    val qualityScore: String = "",
    val deliveryScore: String = "",
    val serviceScore: String = "",
)
