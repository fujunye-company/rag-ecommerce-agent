package com.shopping.agent.data.model

/**
 * SSE 事件类型 — 与后端 /api/v1/chat SSE 协议对齐。
 *
 * 后端事件:
 *   text_delta   → 流式文本增量（逐 token）
 *   product_cards → 单张推荐商品卡片（逐张推送）
 *   progress     → 流水线进度提示
 *   done         → 流结束
 *   error        → 错误
 */
sealed class SSEEvent {
    data class TextDelta(val delta: String, val content: String = "") : SSEEvent()
    
    data class ProductCard(
        val productId: String,
        val title: String,
        val price: Double,
        val rating: Double,
        val matchScore: Double,
        val highlights: List<String>,
        val imageUrl: String?,
        val imageUrls: List<String>,
        val brand: String?,
        val category: String,
        val index: Int,
        val total: Int
    ) : SSEEvent()
    
    data class Progress(val message: String) : SSEEvent()
    
    data class Done(
        val sessionId: String = "",
        val totalCards: Int = 0,
        val latencyMs: Int = 0
    ) : SSEEvent()
    
    data class Error(val message: String) : SSEEvent()

    data class Clarify(
        val question: String,
        val missingSlots: List<String>,
        val options: List<String>
    ) : SSEEvent()
}

// ── JSON payload 映射（仅 SseClient 内部解析用） ──

data class TextDeltaPayload(val content: String)

data class ProductCardPayload(
    val product_id: String,
    val title: String,
    val price: Double,
    val rating: Double,
    val match_score: Double,
    val highlights: List<String>?,
    val image_url: String?,
    val image_urls: List<String>?,
    val brand: String?,
    val category: String?,
    val index: Int,
    val total: Int
)

data class ProgressPayload(val message: String)

data class DonePayload(
    val total_cards: Int,
    val latency_ms: Int,
    val message: String?
)

data class ErrorPayload(val message: String, val code: String?)

data class ClarifyPayload(
    val question: String,
    val missing_slots: List<String>?,
    val options: List<String>?
)
