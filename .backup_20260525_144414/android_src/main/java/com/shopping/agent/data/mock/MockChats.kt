package com.shopping.agent.data.mock

import com.shopping.agent.data.model.ChatMessage
import com.shopping.agent.data.model.Product

/**
 * Mock 聊天历史 — 用于首次打开对话页时的预设展示
 * 5~8 条预设对话覆盖常见电商场景：推荐、对比、购物车、比价等
 */
object MockChats {

    val defaultMessages: List<ChatMessage> = listOf(
        // 0 — 欢迎语
        ChatMessage(
            role = "assistant",
            content = "你好！我是你的AI导购助手 🛍️\n" +
                "可以帮你推荐商品、对比型号、查询价格，还能管理购物车。\n" +
                "试试问我：\"推荐一款500元以内的蓝牙耳机\"",
            timestamp = System.currentTimeMillis() - 600_000
        ),
        // 1 — 用户问推荐耳机
        ChatMessage(
            role = "user",
            content = "推荐一款500元以内的蓝牙耳机，降噪好一点的",
            timestamp = System.currentTimeMillis() - 580_000
        ),
        // 2 — AI 回复 + 商品卡
        ChatMessage(
            role = "assistant",
            content = "为你挑选了3款降噪不错的蓝牙耳机，都在500以内：",
            products = listOf(
                Product(
                    id = "mock-001",
                    name = "漫步者 W820NB 双金标版",
                    price = 299.0,
                    imageUrl = "https://img.alicdn.com/imgextra/i4/2213913544732/O1CN01abc123_mock.jpg",
                    highlights = listOf("Hi-Res双金标", "-42dB主动降噪", "49小时续航"),
                    matchScore = 0.95,
                    rating = 4.8,
                    brand = "漫步者",
                    category = "耳机"
                ),
                Product(
                    id = "mock-002",
                    name = "倍思 Bowie H1i",
                    price = 169.0,
                    imageUrl = "https://img.alicdn.com/imgextra/i3/2213913544732/O1CN01def456_mock.jpg",
                    highlights = listOf("40dB深度降噪", "低延迟游戏模式", "70小时超长续航"),
                    matchScore = 0.88,
                    rating = 4.6,
                    brand = "倍思",
                    category = "耳机"
                ),
                Product(
                    id = "mock-003",
                    name = "华为 FreeBuds SE 2",
                    price = 199.0,
                    imageUrl = "https://img.alicdn.com/imgextra/i2/2213913544732/O1CN01ghi789_mock.jpg",
                    highlights = listOf("半入耳舒适佩戴", "蓝牙5.3", "IP54防水"),
                    matchScore = 0.82,
                    rating = 4.5,
                    brand = "华为",
                    category = "耳机"
                )
            ),
            timestamp = System.currentTimeMillis() - 570_000
        ),
        // 3 — 用户追问对比
        ChatMessage(
            role = "user",
            content = "漫步者那款和华为的比，哪个更值得买？主要通勤用",
            timestamp = System.currentTimeMillis() - 560_000
        ),
        // 4 — AI 对比回复
        ChatMessage(
            role = "assistant",
            content = "通勤场景下我推荐漫步者 W820NB：\n\n" +
                "🔇 降噪：漫步者-42dB vs 华为不支持主动降噪，通勤时降噪很关键\n" +
                "🔋 续航：漫步者49小时 vs 华为约9小时，一周一充更省心\n" +
                "💰 性价比：虽然漫步者贵100元，但多了主动降噪和双金标音质\n\n" +
                "如果预算敏感且偏爱半入耳，华为也是不错的选择。",
            timestamp = System.currentTimeMillis() - 550_000
        ),
        // 5 — 拍图/购物车咨询
        ChatMessage(
            role = "user",
            content = "帮我把漫步者那个加购物车",
            timestamp = System.currentTimeMillis() - 540_000
        ),
        // 6 — AI 确认操作
        ChatMessage(
            role = "assistant",
            content = "已为你将「漫步者 W820NB 双金标版」(¥299) 加入购物车 ✅\n\n" +
                "当前购物车共 2 件商品，合计 ¥468。\n" +
                "需要下单或继续挑选其他商品吗？",
            timestamp = System.currentTimeMillis() - 530_000
        ),
        // 7 — 手机比价场景
        ChatMessage(
            role = "user",
            content = "3000左右拍照好的手机有什么推荐？",
            timestamp = System.currentTimeMillis() - 300_000
        ),
        // 8 — AI 推荐
        ChatMessage(
            role = "assistant",
            content = "3K价位拍照手机，这几款口碑不错：",
            products = listOf(
                Product(
                    id = "mock-010",
                    name = "荣耀 200 Pro",
                    price = 3299.0,
                    imageUrl = "https://img.alicdn.com/imgextra/i1/mock_phone_01.jpg",
                    highlights = listOf("5000万雅顾人像", "骁龙8s Gen3", "5200mAh+100W"),
                    matchScore = 0.93,
                    rating = 4.7,
                    brand = "荣耀",
                    category = "手机"
                ),
                Product(
                    id = "mock-011",
                    name = "vivo S19 Pro",
                    price = 2999.0,
                    imageUrl = "https://img.alicdn.com/imgextra/i2/mock_phone_02.jpg",
                    highlights = listOf("影棚级柔光环", "天玑8200", "6000mAh大电池"),
                    matchScore = 0.90,
                    rating = 4.6,
                    brand = "vivo",
                    category = "手机"
                ),
                Product(
                    id = "mock-012",
                    name = "OPPO Reno12",
                    price = 2699.0,
                    imageUrl = "https://img.alicdn.com/imgextra/i3/mock_phone_03.jpg",
                    highlights = listOf("AI修图大师", "天玑8250", "5000mAh+80W"),
                    matchScore = 0.85,
                    rating = 4.5,
                    brand = "OPPO",
                    category = "手机"
                )
            ),
            timestamp = System.currentTimeMillis() - 290_000
        )
    )

    /**
     * 空对话时的初始问候
     */
    val emptyWelcome: List<ChatMessage> = listOf(
        ChatMessage(
            role = "assistant",
            content = "👋 你好！我是AI导购助手。\n" +
                "试试问我：\n" +
                "• 推荐一款蓝牙耳机\n" +
                "• 对比两款手机\n" +
                "• 帮我找300以内的运动鞋",
            timestamp = System.currentTimeMillis()
        )
    )
}
