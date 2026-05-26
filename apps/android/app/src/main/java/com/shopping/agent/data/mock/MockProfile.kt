package com.shopping.agent.data.mock

/** 个人中心 Mock — ProfileScreen 数据源 */
object MockProfile {
    val userName = "fujunye"

    // 购物车
    val cartTitle = "购物车"
    val cartCount = 3
    data class CartMiniItem(
        val title: String,
        val price: String,
        val imageUrl: String = "",
    )
    val cartItems = listOf(
        CartMiniItem("轻盈跑步鞋", "¥299"),
        CartMiniItem("缓震运动鞋", "¥359"),
        CartMiniItem("运动中筒袜", "¥39"),
        CartMiniItem("运动健身包", "¥159"),
    )

    // 我的订单
    data class OrderStatus(
        val id: String,
        val label: String,
        val count: Int,
    )
    val orderStatuses = listOf(
        OrderStatus("unpaid", "待付款", 0),
        OrderStatus("shipping", "待发货", 1),
        OrderStatus("receiving", "待收货", 2),
        OrderStatus("comment", "待评价", 0),
        OrderStatus("refund", "退款/售后", 0),
    )

    // 常用功能
    data class Feature(
        val title: String,
        val subtitle: String,
    )
    val features = listOf(
        Feature("快递", "1件待发货"),
        Feature("收藏", "15件收藏"),
        Feature("关注店铺", "最近逛过"),
        Feature("足迹", "看过89件"),
        Feature("想要", "1件想要"),
        Feature("我有", "1件拥有"),
    )

    // 领券中心
    data class Coupon(
        val amount: String,
        val label: String,
    )
    val coupons = listOf(
        Coupon("¥260", "满减券"),
        Coupon("¥300", "鞋服专享"),
        Coupon("¥25", "新客券"),
        Coupon("¥105", "通用券包"),
    )

    // 向后兼容旧字段
    val collectionCount = 15
    val followingShops = 6
    @Deprecated("Use coupons list instead")
    val couponAmounts = listOf("+260", "+300", "¥25", "¥105")
    @Deprecated("Use coupons list instead")
    val couponLabels = listOf("减消费", "鞋服专享", "通用券", "通用券包")
}
