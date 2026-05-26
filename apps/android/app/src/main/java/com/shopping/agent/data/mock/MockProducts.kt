package com.shopping.agent.data.mock

import com.shopping.agent.data.model.Product

/**
 * Mock 商品数据 — 对齐 DATA-CONTRACT.md v1.0
 * 字段与后端 ProductRecord 一致
 */
val mockProducts = listOf(
    // ── 运动鞋 (3) ──
    Product(
        productId = "p001", title = "Nike Air Max 270 缓震透气跑鞋",
        price = 899.0,
        imageUrl = "https://placehold.co/400x400/hsl(175,50%25,75%25)/333?text=shoe1", imageUrls = listOf("https://placehold.co/400x400/hsl(175,50%25,75%25)/333?text=shoe1"),
        category = "运动鞋", brand = "Nike", source = "天猫旗舰店",
        ratingCount = 21000, rating = 4.7f,
        rankReason = "270度大气垫，网面透气，经典百搭",
        attributes = mapOf("材质" to "网布+合成革", "功能" to "缓震/透气", "适用" to "跑步/日常"),
    ),
    Product(
        productId = "p002", title = "Adidas Ultraboost 23 爆米花跑鞋",
        price = 999.0,
        imageUrl = "https://placehold.co/400x400/hsl(231,50%25,75%25)/333?text=shoe2", imageUrls = listOf("https://placehold.co/400x400/hsl(231,50%25,75%25)/333?text=shoe2"),
        category = "运动鞋", brand = "Adidas", source = "京东自营",
        ratingCount = 18000, rating = 4.8f,
        rankReason = "全掌Boost爆米花中底，极致缓震回弹",
        attributes = mapOf("鞋面" to "Primeknit编织", "鞋底" to "Boost+马牌橡胶", "功能" to "缓震/回弹"),
    ),
    Product(
        productId = "p003", title = "李宁 飞电3.0 Elite 碳板竞速跑鞋",
        price = 1299.0,
        imageUrl = "https://placehold.co/400x400/hsl(20,50%25,75%25)/333?text=shoe3", imageUrls = listOf("https://placehold.co/400x400/hsl(20,50%25,75%25)/333?text=shoe3"),
        category = "运动鞋", brand = "李宁", source = "李宁官方",
        ratingCount = 6200, rating = 4.7f,
        rankReason = "全掌碳板+䨻科技，马拉松竞速首选",
        attributes = mapOf("鞋面" to "䨻丝鞋面", "科技" to "䨻科技+碳板", "适用" to "竞速/比赛"),
    ),

    // ── 运动装备 (2) ──
    Product(
        productId = "p004", title = "Keep 瑜伽垫 加厚防滑 10mm NBR",
        price = 99.0,
        imageUrl = "https://placehold.co/400x400/hsl(264,50%25,75%25)/333?text=sport1", imageUrls = listOf("https://placehold.co/400x400/hsl(264,50%25,75%25)/333?text=sport1"),
        category = "运动装备", brand = "Keep", source = "京东自营",
        ratingCount = 35000, rating = 4.4f,
        rankReason = "10mm加厚关节保护，双面防滑纹理",
        attributes = mapOf("材质" to "NBR", "厚度" to "10mm", "尺寸" to "183×80cm"),
    ),
    Product(
        productId = "p005", title = "YONEX 天斧100ZZ 全碳素羽毛球拍",
        price = 1680.0,
        imageUrl = "https://placehold.co/400x400/hsl(202,50%25,75%25)/333?text=sport2", imageUrls = listOf("https://placehold.co/400x400/hsl(202,50%25,75%25)/333?text=sport2"),
        category = "运动装备", brand = "YONEX", source = "天猫旗舰店",
        ratingCount = 4700, rating = 4.8f,
        rankReason = "Namd碳素+头重设计，世界冠军同款",
        attributes = mapOf("材质" to "全碳素", "重量" to "4U约83g", "适用" to "中高级"),
    ),

    // ── 服装 (3) ──
    Product(
        productId = "p006", title = "优衣库 哆啦A梦联名纯棉T恤 男女同款",
        price = 79.0,
        imageUrl = "https://placehold.co/400x400/hsl(5,50%25,75%25)/333?text=cloth1", imageUrls = listOf("https://placehold.co/400x400/hsl(5,50%25,75%25)/333?text=cloth1"),
        category = "服装", brand = "优衣库", source = "天猫旗舰店",
        ratingCount = 32000, rating = 4.6f,
        rankReason = "100%纯棉柔软透气，经典联名印花",
        attributes = mapOf("材质" to "100%棉", "版型" to "宽松", "尺码" to "S-XXL"),
    ),
    Product(
        productId = "p007", title = "ZARA 碎花连衣裙 法式复古V领 度假风",
        price = 349.0,
        imageUrl = "https://placehold.co/400x400/hsl(25,50%25,75%25)/333?text=cloth2", imageUrls = listOf("https://placehold.co/400x400/hsl(25,50%25,75%25)/333?text=cloth2"),
        category = "服装", brand = "ZARA", source = "ZARA官方",
        ratingCount = 8900, rating = 4.4f,
        rankReason = "轻盈雪纺飘逸柔美，V领显瘦",
        attributes = mapOf("材质" to "雪纺/聚酯", "裙长" to "中长款", "尺码" to "S-L"),
    ),
    Product(
        productId = "p008", title = "蕉下 冰薄系列防晒衣 UPF50+ 男女通用",
        price = 219.0,
        imageUrl = "https://placehold.co/400x400/hsl(28,50%25,75%25)/333?text=cloth3", imageUrls = listOf("https://placehold.co/400x400/hsl(28,50%25,75%25)/333?text=cloth3"),
        category = "服装", brand = "蕉下", source = "天猫旗舰店",
        ratingCount = 42000, rating = 4.6f,
        rankReason = "UPF50+冰丝凉感，夏季户外必备",
        attributes = mapOf("材质" to "冰丝面料", "防晒" to "UPF50+", "尺码" to "M-4XL"),
    ),

    // ── 箱包 (2) ──
    Product(
        productId = "p009", title = "新秀丽 20寸铝框登机箱 TSA锁 万向轮",
        price = 999.0,
        imageUrl = "https://placehold.co/400x400/hsl(317,50%25,75%25)/333?text=bag1", imageUrls = listOf("https://placehold.co/400x400/hsl(317,50%25,75%25)/333?text=bag1"),
        category = "箱包", brand = "新秀丽", source = "京东自营",
        ratingCount = 12000, rating = 4.7f,
        rankReason = "铝框结构坚固，TSA海关锁，静音万向轮",
        attributes = mapOf("尺寸" to "20寸", "材质" to "PC+铝框", "容量" to "35L"),
    ),
    Product(
        productId = "p010", title = "Coach 经典C纹帆布托特包 大号通勤",
        price = 2280.0,
        imageUrl = "https://placehold.co/400x400/hsl(255,50%25,75%25)/333?text=bag2", imageUrls = listOf("https://placehold.co/400x400/hsl(255,50%25,75%25)/333?text=bag2"),
        category = "箱包", brand = "Coach", source = "天猫国际",
        ratingCount = 5400, rating = 4.7f,
        rankReason = "经典Signature C图案，可拆卸肩带两用",
        attributes = mapOf("材质" to "帆布/牛皮", "容量" to "35L", "开合" to "拉链"),
    ),

    // ── 数码 (3) ──
    Product(
        productId = "p011", title = "索尼 WH-1000XM5 无线降噪头戴耳机",
        price = 2499.0,
        imageUrl = "https://placehold.co/400x400/hsl(105,50%25,75%25)/333?text=digi1", imageUrls = listOf("https://placehold.co/400x400/hsl(105,50%25,75%25)/333?text=digi1"),
        category = "数码", brand = "索尼", source = "天猫旗舰店",
        ratingCount = 15000, rating = 4.7f,
        rankReason = "降噪深度35dB，30小时续航，250g轻量",
        attributes = mapOf("降噪" to "主动降噪35dB", "续航" to "30小时", "重量" to "250g"),
    ),
    Product(
        productId = "p012", title = "小米 Watch S4 智能手表 心率血氧监测",
        price = 999.0,
        imageUrl = "https://placehold.co/400x400/hsl(177,50%25,75%25)/333?text=digi2", imageUrls = listOf("https://placehold.co/400x400/hsl(177,50%25,75%25)/333?text=digi2"),
        category = "数码", brand = "小米", source = "小米商城",
        ratingCount = 34000, rating = 4.5f,
        rankReason = "15天续航+100+运动模式+eSIM独立通话",
        attributes = mapOf("屏幕" to "1.43寸AMOLED", "续航" to "15天", "防水" to "5ATM"),
    ),
    Product(
        productId = "p013", title = "iPhone 15 Pro Max 256GB 原色钛金属",
        price = 8999.0,
        imageUrl = "https://placehold.co/400x400/hsl(118,50%25,75%25)/333?text=digi3", imageUrls = listOf("https://placehold.co/400x400/hsl(118,50%25,75%25)/333?text=digi3"),
        category = "数码", brand = "Apple", source = "Apple Store",
        ratingCount = 89000, rating = 4.8f,
        rankReason = "A17 Pro芯片+钛金属设计+5倍长焦",
        attributes = mapOf("芯片" to "A17 Pro", "屏幕" to "6.7寸OLED", "存储" to "256GB"),
    ),

    // ── 美妆 (2) ──
    Product(
        productId = "p014", title = "兰蔻 小黑瓶精华肌底液 30ml 抗初老",
        price = 1080.0,
        imageUrl = "https://placehold.co/400x400/hsl(205,50%25,75%25)/333?text=beauty1", imageUrls = listOf("https://placehold.co/400x400/hsl(205,50%25,75%25)/333?text=beauty1"),
        category = "美妆", brand = "兰蔻", source = "天猫国际",
        ratingCount = 23000, rating = 4.7f,
        rankReason = "10%二裂酵母精粹，7天水润度提升",
        attributes = mapOf("规格" to "30ml", "功效" to "修护/保湿/抗初老", "质地" to "蛋清质地"),
    ),
    Product(
        productId = "p015", title = "资生堂 安耐晒小金瓶 SPF50+ 60ml",
        price = 228.0,
        imageUrl = "https://placehold.co/400x400/hsl(280,50%25,75%25)/333?text=beauty2", imageUrls = listOf("https://placehold.co/400x400/hsl(280,50%25,75%25)/333?text=beauty2"),
        category = "美妆", brand = "资生堂", source = "天猫国际",
        ratingCount = 78000, rating = 4.8f,
        rankReason = "Aqua Booster遇水则强，清爽不油腻",
        attributes = mapOf("防晒" to "SPF50+PA++++", "容量" to "60ml", "功能" to "防水防汗"),
    ),

    // ── 食品 (2) ──
    Product(
        productId = "p016", title = "三只松鼠 坚果大礼包 1.8kg 送礼年货",
        price = 138.0,
        imageUrl = "https://placehold.co/400x400/hsl(53,50%25,75%25)/333?text=food1", imageUrls = listOf("https://placehold.co/400x400/hsl(53,50%25,75%25)/333?text=food1"),
        category = "食品", brand = "三只松鼠", source = "天猫旗舰店",
        ratingCount = 89000, rating = 4.7f,
        rankReason = "近10种坚果混合装，独立包装锁鲜",
        attributes = mapOf("重量" to "1.8kg", "内容" to "夏威夷果/巴旦木/腰果", "包装" to "礼盒装"),
    ),
    Product(
        productId = "p017", title = "蒙牛 特仑苏纯牛奶 250ml×12盒",
        price = 79.0,
        imageUrl = "https://placehold.co/400x400/hsl(81,50%25,75%25)/333?text=food2", imageUrls = listOf("https://placehold.co/400x400/hsl(81,50%25,75%25)/333?text=food2"),
        category = "食品", brand = "蒙牛", source = "京东自营",
        ratingCount = 109000, rating = 4.7f,
        rankReason = "3.6g优质乳蛋白，限定牧场奶源",
        attributes = mapOf("规格" to "250ml×12盒", "蛋白" to "3.6g/100ml", "类型" to "UHT灭菌"),
    ),

    // ── 家居 (2) ──
    Product(
        productId = "p018", title = "戴森 V15 Detect 激光探测无绳吸尘器",
        price = 4990.0,
        imageUrl = "https://placehold.co/400x400/hsl(122,50%25,75%25)/333?text=home1", imageUrls = listOf("https://placehold.co/400x400/hsl(122,50%25,75%25)/333?text=home1"),
        category = "家居", brand = "戴森", source = "戴森官方",
        ratingCount = 12000, rating = 4.8f,
        rankReason = "激光探测微尘，LCD实时显示，240AW吸力",
        attributes = mapOf("吸力" to "240AW", "续航" to "60分钟", "过滤" to "整机HEPA"),
    ),
    Product(
        productId = "p019", title = "富安娜 60支长绒棉四件套 贡缎工艺",
        price = 499.0,
        imageUrl = "https://placehold.co/400x400/hsl(286,50%25,75%25)/333?text=home2", imageUrls = listOf("https://placehold.co/400x400/hsl(286,50%25,75%25)/333?text=home2"),
        category = "家居", brand = "富安娜", source = "天猫旗舰店",
        ratingCount = 21000, rating = 4.6f,
        rankReason = "新疆长绒棉+60支贡缎工艺，如丝绸顺滑",
        attributes = mapOf("材质" to "长绒棉", "支数" to "60支", "工艺" to "贡缎"),
    ),

    // ── 图书 (2) ──
    Product(
        productId = "p020", title = "三体 套装共3册 刘慈欣 雨果奖科幻巨著",
        price = 93.0,
        imageUrl = "https://placehold.co/400x400/hsl(236,50%25,75%25)/333?text=book1", imageUrls = listOf("https://placehold.co/400x400/hsl(236,50%25,75%25)/333?text=book1"),
        category = "图书", brand = "刘慈欣", source = "京东自营",
        ratingCount = 231000, rating = 4.9f,
        rankReason = "亚洲首获雨果奖，黑暗森林法则震撼",
        attributes = mapOf("作者" to "刘慈欣", "页数" to "1285", "装帧" to "平装"),
    ),
    Product(
        productId = "p021", title = "人类简史 从动物到上帝 尤瓦尔·赫拉利",
        price = 68.0,
        imageUrl = "https://placehold.co/400x400/hsl(149,50%25,75%25)/333?text=book2", imageUrls = listOf("https://placehold.co/400x400/hsl(149,50%25,75%25)/333?text=book2"),
        category = "图书", brand = "赫拉利", source = "京东自营",
        ratingCount = 123000, rating = 4.7f,
        rankReason = "全球现象级畅销书，跨学科宏大叙事",
        attributes = mapOf("作者" to "尤瓦尔·赫拉利", "页数" to "440", "装帧" to "平装"),
    ),

    // ── 宠物 (2) ──
    Product(
        productId = "p022", title = "皇家 F32 理想体态成猫粮 4kg 均衡营养",
        price = 239.0,
        imageUrl = "https://placehold.co/400x400/hsl(40,50%25,75%25)/333?text=pet1", imageUrls = listOf("https://placehold.co/400x400/hsl(40,50%25,75%25)/333?text=pet1"),
        category = "宠物", brand = "皇家", source = "京东自营",
        ratingCount = 56000, rating = 4.7f,
        rankReason = "精准营养配方，益生元改善肠道健康",
        attributes = mapOf("规格" to "4kg", "适用" to "1-10岁成猫", "功能" to "均衡营养"),
    ),
    Product(
        productId = "p023", title = "小佩 全自动猫砂盆 智能铲屎APP远程",
        price = 1299.0,
        imageUrl = "https://placehold.co/400x400/hsl(178,50%25,75%25)/333?text=pet2", imageUrls = listOf("https://placehold.co/400x400/hsl(178,50%25,75%25)/333?text=pet2"),
        category = "宠物", brand = "小佩", source = "天猫旗舰店",
        ratingCount = 12000, rating = 4.5f,
        rankReason = "自动感应铲屎，9L集便仓一周不换",
        attributes = mapOf("容量" to "9L集便仓", "功能" to "自动铲屎/除臭/APP", "适用" to "1.5-10kg猫"),
    ),

    // ── 办公 (2) ──
    Product(
        productId = "p024", title = "罗技 K380 无线蓝牙键盘 多设备切换",
        price = 229.0,
        imageUrl = "https://placehold.co/400x400/hsl(126,50%25,75%25)/333?text=office1", imageUrls = listOf("https://placehold.co/400x400/hsl(126,50%25,75%25)/333?text=office1"),
        category = "办公", brand = "罗技", source = "京东自营",
        ratingCount = 67000, rating = 4.7f,
        rankReason = "一键3台设备切换，2年超长电池",
        attributes = mapOf("连接" to "蓝牙3.0", "续航" to "2年", "兼容" to "Win/Mac/iOS"),
    ),
    Product(
        productId = "p025", title = "科大讯飞 AI录音笔 B1 32G 实时转写",
        price = 399.0,
        imageUrl = "https://placehold.co/400x400/hsl(221,50%25,75%25)/333?text=office2", imageUrls = listOf("https://placehold.co/400x400/hsl(221,50%25,75%25)/333?text=office2"),
        category = "办公", brand = "科大讯飞", source = "京东自营",
        ratingCount = 23000, rating = 4.6f,
        rankReason = "98%转写准确率，1小时录音5分钟出稿",
        attributes = mapOf("存储" to "32GB", "功能" to "转写/翻译/同传", "续航" to "10小时"),
    ),

    // ── 玩具 (2) ──
    Product(
        productId = "p026", title = "LEGO 兰博基尼 Sián 1:8 科技旗舰 3696粒",
        price = 2699.0,
        imageUrl = "https://placehold.co/400x400/hsl(243,50%25,75%25)/333?text=toy1", imageUrls = listOf("https://placehold.co/400x400/hsl(243,50%25,75%25)/333?text=toy1"),
        category = "玩具", brand = "LEGO", source = "天猫旗舰店",
        ratingCount = 12000, rating = 4.9f,
        rankReason = "1:8精细还原，可动V12引擎+8速变速箱",
        attributes = mapOf("颗粒" to "3696粒", "比例" to "1:8", "年龄" to "18+"),
    ),
    Product(
        productId = "p027", title = "任天堂 Switch OLED 马力欧红蓝 国行",
        price = 2099.0,
        imageUrl = "https://placehold.co/400x400/hsl(241,50%25,75%25)/333?text=toy2", imageUrls = listOf("https://placehold.co/400x400/hsl(241,50%25,75%25)/333?text=toy2"),
        category = "玩具", brand = "任天堂", source = "天猫旗舰店",
        ratingCount = 89000, rating = 4.8f,
        rankReason = "7寸OLED屏+三种模式，老少皆宜全家同乐",
        attributes = mapOf("屏幕" to "7寸OLED", "存储" to "64GB", "模式" to "TV/掌机/桌面"),
    ),

    // ── 汽车 (1) ──
    Product(
        productId = "p028", title = "70迈 D08 4K行车记录仪 前后双录 ADAS",
        price = 499.0,
        imageUrl = "https://placehold.co/400x400/hsl(86,50%25,75%25)/333?text=auto1", imageUrls = listOf("https://placehold.co/400x400/hsl(86,50%25,75%25)/333?text=auto1"),
        category = "汽车", brand = "70迈", source = "京东自营",
        ratingCount = 34000, rating = 4.4f,
        rankReason = "真4K超清+前后双录+24H停车监控",
        attributes = mapOf("分辨率" to "4K", "视角" to "140°广角", "存储" to "最大256GB"),
    ),

    // ── 母婴 (1) ──
    Product(
        productId = "p029", title = "贝亲 宽口径玻璃奶瓶 240ml 新生儿防胀气",
        price = 139.0,
        imageUrl = "https://placehold.co/400x400/hsl(234,50%25,75%25)/333?text=baby1", imageUrls = listOf("https://placehold.co/400x400/hsl(234,50%25,75%25)/333?text=baby1"),
        category = "母婴", brand = "贝亲", source = "天猫旗舰店",
        ratingCount = 345000, rating = 4.8f,
        rankReason = "自然实感奶嘴+防胀气，480万妈妈信任",
        attributes = mapOf("容量" to "240ml", "材质" to "硼硅酸玻璃", "奶嘴" to "SS号"),
    ),

    // ── 健康 (1) ──
    Product(
        productId = "p030", title = "欧姆龙 血压计 上臂式 U701J 蓝牙大屏",
        price = 299.0,
        imageUrl = "https://placehold.co/400x400/hsl(331,50%25,75%25)/333?text=health1", imageUrls = listOf("https://placehold.co/400x400/hsl(331,50%25,75%25)/333?text=health1"),
        category = "健康", brand = "欧姆龙", source = "京东自营",
        ratingCount = 89000, rating = 4.6f,
        rankReason = "欧姆龙核心芯片准确，蓝牙APP双人管理",
        attributes = mapOf("类型" to "上臂式", "记忆" to "60组×2人", "功能" to "蓝牙+大屏"),
    ),
)

/** 获取分类列表 */
fun getCategories(): List<String> = mockProducts
    .map { it.category }
    .distinct()
    .let { listOf("推荐") + it }
