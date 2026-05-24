package com.shopping.agent.data.mock

import com.shopping.agent.data.model.Product

/**
 * P02 分类商品 Mock 数据
 * 覆盖运动(6)、数码(6)、美妆(4)、家居(5)、鞋包(5)共 26 款商品
 */
object MockCategoryProducts {

    val sportProducts: List<Product> = listOf(
        Product(
            id = "sport_001", name = "Nike Air Zoom Pegasus 40 跑鞋",
            price = 899.00, imageUrl = "https://picsum.photos/seed/sport1/400/400",
            highlights = listOf("Zoom Air 缓震", "Flywire 飞线科技"),
            brand = "Nike", category = "运动", rating = 4.7,
        ),
        Product(
            id = "sport_002", name = "Adidas Ultraboost 23 跑步鞋",
            price = 1099.00, imageUrl = "https://picsum.photos/seed/sport2/400/400",
            highlights = listOf("Boost 中底", "Primeknit 鞋面"),
            brand = "Adidas", category = "运动", rating = 4.8,
        ),
        Product(
            id = "sport_003", name = "Lululemon Swiftly Tech 长袖T恤",
            price = 580.00, imageUrl = "https://picsum.photos/seed/sport3/400/400",
            highlights = listOf("Silverescent 科技", "无缝结构"),
            brand = "Lululemon", category = "运动", rating = 4.5,
        ),
        Product(
            id = "sport_004", name = "小米手环 8 Pro NFC版",
            price = 399.00, imageUrl = "https://picsum.photos/seed/sport4/400/400",
            highlights = listOf("1.74'' AMOLED", "150+ 运动模式"),
            brand = "小米", category = "运动", rating = 4.6,
        ),
        Product(
            id = "sport_005", name = "YONEX 天斧 100ZZ 羽毛球拍",
            price = 1580.00, imageUrl = "https://picsum.photos/seed/sport5/400/400",
            highlights = listOf("Namd 碳素材质", "回转力生成系统"),
            brand = "YONEX", category = "运动", rating = 4.9,
        ),
        Product(
            id = "sport_006", name = "Keep 瑜伽垫 加厚防滑 6mm",
            price = 129.00, imageUrl = "https://picsum.photos/seed/sport6/400/400",
            highlights = listOf("双层防滑", "环保TPE材质"),
            brand = "Keep", category = "运动", rating = 4.4,
        ),
    )

    val digitalProducts: List<Product> = listOf(
        Product(
            id = "digital_001", name = "MacBook Air 15 M3 芯片",
            price = 8999.00, imageUrl = "https://picsum.photos/seed/digi1/400/400",
            highlights = listOf("M3 芯片", "15.3'' Liquid Retina"),
            brand = "Apple", category = "数码", rating = 4.9,
        ),
        Product(
            id = "digital_002", name = "iPad Pro 12.9 M2 芯片",
            price = 9299.00, imageUrl = "https://picsum.photos/seed/digi2/400/400",
            highlights = listOf("M2 芯片", "Mini-LED XDR"),
            brand = "Apple", category = "数码", rating = 4.8,
        ),
        Product(
            id = "digital_003", name = "DJI Mini 4 Pro 无人机",
            price = 4788.00, imageUrl = "https://picsum.photos/seed/digi3/400/400",
            highlights = listOf("4K/100fps", "全向避障"),
            brand = "DJI", category = "数码", rating = 4.8,
        ),
        Product(
            id = "digital_004", name = "Kindle Paperwhite 第11代",
            price = 1068.00, imageUrl = "https://picsum.photos/seed/digi4/400/400",
            highlights = listOf("6.8'' 墨水屏", "可调节暖光"),
            brand = "Amazon", category = "数码", rating = 4.7,
        ),
        Product(
            id = "digital_005", name = "华为 FreeBuds Pro 3",
            price = 1499.00, imageUrl = "https://picsum.photos/seed/digi5/400/400",
            highlights = listOf("静谧通话 2.0", "星闪连接"),
            brand = "华为", category = "数码", rating = 4.6,
        ),
        Product(
            id = "digital_006", name = "罗技 MX Master 3S 鼠标",
            price = 699.00, imageUrl = "https://picsum.photos/seed/digi6/400/400",
            highlights = listOf("8K DPI", "MagSpeed 滚轮"),
            brand = "Logitech", category = "数码", rating = 4.7,
        ),
    )

    val beautyProducts: List<Product> = listOf(
        Product(
            id = "beauty_001", name = "兰蔻 小黑瓶精华肌底液 50ml",
            price = 1150.00, imageUrl = "https://picsum.photos/seed/beauty1/400/400",
            highlights = listOf("二裂酵母精粹", "修复肌肤屏障"),
            brand = "Lancôme", category = "美妆", rating = 4.8,
        ),
        Product(
            id = "beauty_002", name = "雅诗兰黛 DW持妆粉底液 30ml",
            price = 420.00, imageUrl = "https://picsum.photos/seed/beauty2/400/400",
            highlights = listOf("24H 持妆", "控油遮瑕"),
            brand = "Estée Lauder", category = "美妆", rating = 4.7,
        ),
        Product(
            id = "beauty_003", name = "Tom Ford 黑管唇膏 #16 Scarlet Rouge",
            price = 420.00, imageUrl = "https://picsum.photos/seed/beauty3/400/400",
            highlights = listOf("丝缎质地", "高显色度"),
            brand = "Tom Ford", category = "美妆", rating = 4.9,
        ),
        Product(
            id = "beauty_004", name = "SK-II 神仙水 230ml",
            price = 1540.00, imageUrl = "https://picsum.photos/seed/beauty4/400/400",
            highlights = listOf("PITERA™ 精华", "晶莹剔透"),
            brand = "SK-II", category = "美妆", rating = 4.8,
        ),
    )

    val homeProducts: List<Product> = listOf(
        Product(
            id = "home_001", name = "Dyson V15 Detect 无绳吸尘器",
            price = 4990.00, imageUrl = "https://picsum.photos/seed/home1/400/400",
            highlights = listOf("激光探测微尘", "60分钟续航"),
            brand = "Dyson", category = "家居", rating = 4.8,
        ),
        Product(
            id = "home_002", name = "小米 米家扫拖机器人 2 Pro",
            price = 2699.00, imageUrl = "https://picsum.photos/seed/home2/400/400",
            highlights = listOf("LDS 激光导航", "3D 避障"),
            brand = "小米", category = "家居", rating = 4.6,
        ),
        Product(
            id = "home_003", name = "MUJI 超声波香薰机 大号",
            price = 450.00, imageUrl = "https://picsum.photos/seed/home3/400/400",
            highlights = listOf("4小时定时", "暖色LED灯"),
            brand = "MUJI", category = "家居", rating = 4.5,
        ),
        Product(
            id = "home_004", name = "北鼎 即热式饮水机 S801",
            price = 998.00, imageUrl = "https://picsum.photos/seed/home4/400/400",
            highlights = listOf("3秒即热", "5档水温"),
            brand = "北鼎", category = "家居", rating = 4.6,
        ),
        Product(
            id = "home_005", name = "IKEA 毕利书柜 白色",
            price = 599.00, imageUrl = "https://picsum.photos/seed/home5/400/400",
            highlights = listOf("可调节隔板", "经典设计"),
            brand = "IKEA", category = "家居", rating = 4.4,
        ),
    )

    val shoesBagsProducts: List<Product> = listOf(
        Product(
            id = "sb_001", name = "Nike Air Force 1 '07 经典白",
            price = 799.00, imageUrl = "https://picsum.photos/seed/sb1/400/400",
            highlights = listOf("Air Sole 气垫", "经典百搭"),
            brand = "Nike", category = "鞋包", rating = 4.7,
        ),
        Product(
            id = "sb_002", name = "Converse Chuck 70 Hi 复古高帮",
            price = 599.00, imageUrl = "https://picsum.photos/seed/sb2/400/400",
            highlights = listOf("加厚帆布", "OrthoLite 鞋垫"),
            brand = "Converse", category = "鞋包", rating = 4.5,
        ),
        Product(
            id = "sb_003", name = "Coach 经典标志帆布托特包",
            price = 2950.00, imageUrl = "https://picsum.photos/seed/sb3/400/400",
            highlights = listOf("Signature C 印花", "大容量通勤"),
            brand = "Coach", category = "鞋包", rating = 4.6,
        ),
        Product(
            id = "sb_004", name = "Longchamp Le Pliage 饺子包 大号",
            price = 1100.00, imageUrl = "https://picsum.photos/seed/sb4/400/400",
            highlights = listOf("可折叠设计", "防水尼龙"),
            brand = "Longchamp", category = "鞋包", rating = 4.7,
        ),
        Product(
            id = "sb_005", name = "Dr.Martens 1460 八孔马丁靴",
            price = 1399.00, imageUrl = "https://picsum.photos/seed/sb5/400/400",
            highlights = listOf("AirWair 气垫底", "经典黄色车线"),
            brand = "Dr.Martens", category = "鞋包", rating = 4.6,
        ),
    )

    /** 所有商品汇总（推荐 Tab 用） */
    val allProducts: List<Product> = listOf(
        sportProducts[0], digitalProducts[0], beautyProducts[0],
        homeProducts[0], shoesBagsProducts[0],
        sportProducts[1], digitalProducts[1], beautyProducts[1],
        homeProducts[1], shoesBagsProducts[1],
        sportProducts[2], digitalProducts[2],
    )

    /** 分类名 → 商品列表映射 */
    val categoryMap: Map<String, List<Product>> = mapOf(
        "推荐" to allProducts,
        "运动" to sportProducts,
        "数码" to digitalProducts,
        "美妆" to beautyProducts,
        "家居" to homeProducts,
        "鞋包" to shoesBagsProducts,
        "全部" to sportProducts + digitalProducts + beautyProducts + homeProducts + shoesBagsProducts,
    )

    val categoryTabs: List<String> = listOf("推荐", "运动", "数码", "美妆", "家居", "鞋包", "全部")
}
