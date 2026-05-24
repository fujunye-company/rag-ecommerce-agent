package com.shopping.agent.data.model

/**
 * SSE 事件密封类 — 所有 type 分支 + unknown 兜底
 * 后端发送格式: {"event":"product_cards","data":"{\"type\":\"product_cards\",\"product_id\":\"...\",\"title\":\"...\",...}"}
 * ProductCardEvent 逐卡片单条下发，含 product_id/title/price/rating/match_score/highlights/image_url/index/total
 */
sealed class SseEvent {
    data class TextDelta(val content: String) : SseEvent()
    data class ProductCard(val product: Product) : SseEvent()
    object Done : SseEvent()
    data class Error(val message: String, val code: String) : SseEvent()
    data class Unknown(val type: String, val raw: String) : SseEvent()

    companion object {
        fun fromJson(type: String, rawData: String): SseEvent = try {
            val json = org.json.JSONObject(rawData)
            when (type) {
                "text_delta" -> TextDelta(json.optString("content", ""))
                "product_cards" -> {
                    // 逐卡片单条下发，字段在 data JSON 顶层
                    val highlightsArr = json.optJSONArray("highlights") ?: org.json.JSONArray()
                    val highlights = (0 until highlightsArr.length()).map { highlightsArr.optString(it, "") }
                    val product = Product(
                        id = json.optString("product_id", ""),
                        name = json.optString("title", ""),
                        price = json.optDouble("price", 0.0),
                        imageUrl = json.optString("image_url", ""),
                        highlights = highlights,
                        matchScore = json.optDouble("match_score", 0.0),
                        rating = json.optDouble("rating", 0.0),
                        brand = json.optString("brand", null),
                        category = json.optString("category", null),
                        index = json.optInt("index", 0),
                        total = json.optInt("total", 0),
                    )
                    ProductCard(product)
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
