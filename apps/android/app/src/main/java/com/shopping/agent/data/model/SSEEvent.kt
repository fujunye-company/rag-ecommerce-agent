package com.shopping.agent.data.model

/**
 * SSE 事件密封类 — 所有 type 分支 + unknown 兜底
 * 后端发送格式: {"event":"text_delta","data":"{\"type\":\"text_delta\",\"content\":\"...\"}"}
 */
sealed class SseEvent {
    data class TextDelta(val content: String) : SseEvent()
    data class ProductCards(val products: List<Product>) : SseEvent()
    object Done : SseEvent()
    data class Error(val message: String, val code: String) : SseEvent()
    data class Unknown(val type: String, val raw: String) : SseEvent()

    companion object {
        fun fromJson(type: String, rawData: String): SseEvent = try {
            val json = org.json.JSONObject(rawData)
            when (type) {
                "text_delta" -> TextDelta(json.optString("content", ""))
                "product_cards" -> {
                    val arr = json.optJSONArray("products") ?: org.json.JSONArray()
                    val products = (0 until arr.length()).map { i ->
                        val obj = arr.getJSONObject(i)
                        Product(
                            id = obj.optString("id", ""),
                            name = obj.optString("title", obj.optString("name", "")),
                            price = obj.optDouble("price", 0.0),
                            imageUrl = (obj.optJSONArray("image_urls")?.optString(0) ?: ""),
                            reason = "",
                            brand = obj.optString("brand", null),
                            category = obj.optString("category", null),
                            rating = obj.optDouble("rating", 0.0),
                            matchScore = obj.optDouble("match_score", 0.0),
                        )
                    }
                    ProductCards(products)
                }
                "done" -> Done
                "error" -> Error(
                    json.optString("message", "未知错误"),
                    json.optString("code", "UNKNOWN")
                )
                else -> Unknown(type, rawData)
            }
        } catch (e: Exception) {
            Error(e.message ?: "解析错误", "PARSE_ERROR")
        }
    }
}
