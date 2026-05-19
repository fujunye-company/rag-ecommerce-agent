package com.shopping.agent.data.model

/**
 * SSE 事件密封类 — 所有 type 分支 + unknown 兜底
 * 遵循开发规约 v2.0 §4.3.6
 */
sealed class SseEvent {
    data class TextDelta(val content: String) : SseEvent()
    data class ProductCards(val products: List<Product>) : SseEvent()
    object Done : SseEvent()
    data class Error(val message: String, val code: String) : SseEvent()
    data class Unknown(val type: String, val raw: String) : SseEvent()

    companion object {
        fun fromJson(type: String, data: Map<String, Any?>): SseEvent = when (type) {
            "text_delta" -> TextDelta(data["content"] as? String ?: "")
            "product_cards" -> ProductCards(emptyList())  // TODO: 解析 products 数组
            "done" -> Done
            "error" -> Error(
                data["message"] as? String ?: "未知错误",
                data["code"] as? String ?: "UNKNOWN"
            )
            else -> Unknown(type, data.toString())
        }
    }
}
