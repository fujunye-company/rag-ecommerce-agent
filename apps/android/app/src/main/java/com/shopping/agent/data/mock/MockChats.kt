package com.shopping.agent.data.mock

import com.shopping.agent.data.model.ChatMessage
import com.shopping.agent.data.model.MessageRole
import com.shopping.agent.data.model.MessageStatus
import com.shopping.agent.data.model.Product

/**
 * Mock 对话数据 — 3 轮示例对话
 * 适配 DESIGN.md v2.0 ChatMessage / Product 模型
 */
object MockChats {

    val sampleMessages: List<ChatMessage> = listOf(
        ChatMessage(
            id = "msg_001", role = MessageRole.User,
            content = "推荐适合油皮的洗面奶",
            status = MessageStatus.Sent,
        ),
        ChatMessage(
            id = "msg_002", role = MessageRole.Assistant,
            content = "为您推荐以下几款适合油皮的洗面奶：",
            productCards = listOf(
                Product(productId = "beauty_001", title = "兰蔻 小黑瓶精华肌底液 50ml",
                    price = 1150.0, imageUrl = "https://picsum.photos/seed/beauty1/400/400",
                    brand = "Lancôme", category = "美妆", rating = 4.8f, source = "天猫国际",
                    rankReason = "二裂酵母精粹，修复肌肤屏障",
                    attributes = mapOf("成分" to "二裂酵母精粹", "功效" to "修复肌肤屏障"),
                ),
                Product(productId = "beauty_002", title = "雅诗兰黛 DW持妆粉底液 30ml",
                    price = 420.0, imageUrl = "https://picsum.photos/seed/beauty2/400/400",
                    brand = "Estée Lauder", category = "美妆", rating = 4.7f, source = "天猫旗舰店",
                    rankReason = "24H持妆，控油遮瑕效果出色",
                    attributes = mapOf("持妆" to "24H", "功效" to "控油遮瑕"),
                ),
                Product(productId = "beauty_004", title = "SK-II 神仙水 230ml",
                    price = 1540.0, imageUrl = "https://picsum.photos/seed/beauty4/400/400",
                    brand = "SK-II", category = "美妆", rating = 4.8f, source = "天猫国际",
                    rankReason = "PITERA精华调理油皮，口碑之选",
                    attributes = mapOf("成分" to "PITERA精华", "功效" to "晶莹剔透"),
                ),
            ),
            status = MessageStatus.Sent,
        ),
        ChatMessage(
            id = "msg_003", role = MessageRole.User,
            content = "200元以下的有吗？",
            status = MessageStatus.Sent,
        ),
        ChatMessage(
            id = "msg_004", role = MessageRole.Assistant,
            content = "在200元预算内，为您找到以下商品：",
            productCards = listOf(
                Product(productId = "sport_006", title = "Keep 瑜伽垫 加厚防滑 6mm",
                    price = 129.0, imageUrl = "https://picsum.photos/seed/sport6/400/400",
                    brand = "Keep", category = "运动", rating = 4.4f, source = "京东自营",
                    rankReason = "双层防滑，环保TPE材质，性价比高",
                    attributes = mapOf("材质" to "环保TPE", "特点" to "双层防滑"),
                ),
            ),
            status = MessageStatus.Sent,
        ),
        ChatMessage(
            id = "msg_005", role = MessageRole.User,
            content = "对比一下这两款",
            status = MessageStatus.Sent,
        ),
        ChatMessage(
            id = "msg_006", role = MessageRole.Assistant,
            content = "好的，帮您对比兰蔻小黑瓶和雅诗兰黛DW粉底液：兰蔻侧重精华修护适合长期调理，雅诗兰黛侧重即时控油遮瑕。您更关注修护还是控油？",
            status = MessageStatus.Sent,
        ),
    )
}
