package com.shopping.agent.data.mock

/**
 * 单条历史会话
 */
data class HistorySession(
    val id: String,
    val title: String,
    val date: String,       // "2025-05-20"
    val time: String,       // "14:30"
    val tags: List<String>, // 标签：推荐/对比/购物车
)

/**
 * 按月份分组
 */
data class HistoryGroup(
    val monthLabel: String,       // "2025年5月"
    val sessions: List<HistorySession>,
)

/**
 * P06 历史侧边栏 Mock 数据 — 12 条历史会话，分 3 个月份
 */
object MockHistory {

    val sessions: List<HistoryGroup> = listOf(
        HistoryGroup(
            monthLabel = "2025年5月",
            sessions = listOf(
                HistorySession(
                    id = "h_001",
                    title = "推荐一款降噪耳机",
                    date = "2025-05-20",
                    time = "14:30",
                    tags = listOf("推荐"),
                ),
                HistorySession(
                    id = "h_002",
                    title = "iPhone 15 vs 小米14 对比",
                    date = "2025-05-18",
                    time = "10:15",
                    tags = listOf("对比"),
                ),
                HistorySession(
                    id = "h_003",
                    title = "查看购物车中的耳机",
                    date = "2025-05-15",
                    time = "16:45",
                    tags = listOf("购物车"),
                ),
                HistorySession(
                    id = "h_004",
                    title = "推荐一款智能手表",
                    date = "2025-05-12",
                    time = "09:00",
                    tags = listOf("推荐"),
                ),
                HistorySession(
                    id = "h_005",
                    title = "氮化镓充电器对比",
                    date = "2025-05-08",
                    time = "11:30",
                    tags = listOf("对比"),
                ),
            ),
        ),
        HistoryGroup(
            monthLabel = "2025年4月",
            sessions = listOf(
                HistorySession(
                    id = "h_006",
                    title = "蓝牙音箱推荐",
                    date = "2025-04-28",
                    time = "15:20",
                    tags = listOf("推荐"),
                ),
                HistorySession(
                    id = "h_007",
                    title = "微单相机选购对比",
                    date = "2025-04-22",
                    time = "13:10",
                    tags = listOf("对比"),
                ),
                HistorySession(
                    id = "h_008",
                    title = "添加到购物车",
                    date = "2025-04-15",
                    time = "17:00",
                    tags = listOf("购物车"),
                ),
                HistorySession(
                    id = "h_009",
                    title = "千元耳机推荐",
                    date = "2025-04-10",
                    time = "08:45",
                    tags = listOf("推荐"),
                ),
            ),
        ),
        HistoryGroup(
            monthLabel = "2025年3月",
            sessions = listOf(
                HistorySession(
                    id = "h_010",
                    title = "手机拍照对比评测",
                    date = "2025-03-28",
                    time = "12:00",
                    tags = listOf("对比"),
                ),
                HistorySession(
                    id = "h_011",
                    title = "春季数码好物推荐",
                    date = "2025-03-15",
                    time = "10:30",
                    tags = listOf("推荐"),
                ),
                HistorySession(
                    id = "h_012",
                    title = "清理购物车",
                    date = "2025-03-05",
                    time = "19:20",
                    tags = listOf("购物车"),
                ),
            ),
        ),
    )
}
