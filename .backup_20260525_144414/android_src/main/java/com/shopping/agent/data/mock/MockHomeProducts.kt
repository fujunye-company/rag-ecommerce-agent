package com.shopping.agent.data.mock

import com.shopping.agent.data.model.Product

/**
 * P01 首页推荐 Mock 数据 — 8 款推荐商品
 * 覆盖耳机/手机/配件/手表/音箱/相机品类
 */
object MockHomeProducts {

    val products: List<Product> = listOf(
        Product(
            id = "prod_001",
            name = "Sony WH-1000XM5 无线降噪耳机",
            price = 2299.00,
            imageUrl = "https://picsum.photos/seed/headphone1/400/400",
            highlights = listOf("行业最强降噪", "30小时续航"),
            matchScore = 0.95,
            brand = "Sony",
            category = "耳机",
            rating = 4.8,
        ),
        Product(
            id = "prod_002",
            name = "iPhone 15 Pro Max 256GB",
            price = 9999.00,
            imageUrl = "https://picsum.photos/seed/iphone15/400/400",
            highlights = listOf("A17 Pro 芯片", "钛金属设计"),
            matchScore = 0.88,
            brand = "Apple",
            category = "手机",
            rating = 4.9,
        ),
        Product(
            id = "prod_003",
            name = "Anker 氮化镓 65W 充电器",
            price = 149.00,
            imageUrl = "https://picsum.photos/seed/charger1/400/400",
            highlights = listOf("GaN技术", "三口快充"),
            matchScore = 0.82,
            brand = "Anker",
            category = "配件",
            rating = 4.7,
        ),
        Product(
            id = "prod_004",
            name = "Apple Watch Ultra 2",
            price = 6499.00,
            imageUrl = "https://picsum.photos/seed/watchultra/400/400",
            highlights = listOf("49mm钛金属", "极限续航36h"),
            matchScore = 0.91,
            brand = "Apple",
            category = "手表",
            rating = 4.8,
        ),
        Product(
            id = "prod_005",
            name = "Marshall Stanmore III 蓝牙音箱",
            price = 2699.00,
            imageUrl = "https://picsum.photos/seed/speaker1/400/400",
            highlights = listOf("经典复古设计", "震撼低音"),
            matchScore = 0.79,
            brand = "Marshall",
            category = "音箱",
            rating = 4.6,
        ),
        Product(
            id = "prod_006",
            name = "Sony A7M4 全画幅微单",
            price = 16499.00,
            imageUrl = "https://picsum.photos/seed/camera1/400/400",
            highlights = listOf("3300万像素", "4K 60fps"),
            matchScore = 0.86,
            brand = "Sony",
            category = "相机",
            rating = 4.9,
        ),
        Product(
            id = "prod_007",
            name = "AirPods Pro 2 (USB-C)",
            price = 1799.00,
            imageUrl = "https://picsum.photos/seed/airpods/400/400",
            highlights = listOf("自适应降噪", "空间音频"),
            matchScore = 0.93,
            brand = "Apple",
            category = "耳机",
            rating = 4.8,
        ),
        Product(
            id = "prod_008",
            name = "小米 14 Ultra 徕卡影像",
            price = 5999.00,
            imageUrl = "https://picsum.photos/seed/xiaomi14/400/400",
            highlights = listOf("徕卡Summilux", "骁龙8Gen3"),
            matchScore = 0.84,
            brand = "小米",
            category = "手机",
            rating = 4.7,
        ),
    )
}
