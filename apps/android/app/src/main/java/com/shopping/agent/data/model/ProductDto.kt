package com.shopping.agent.data.model

/**
 * 后端 SSE product_cards 事件的 DTO — 对齐 DATA-CONTRACT.md v1.0
 * 对应后端 ProductCardEvent schema
 */
data class ProductDto(
    val product_id: String,
    val title: String,
    val price: Double,
    val rating: Double? = null,
    val match_score: Double? = null,
    val highlights: List<String>? = null,
    val image_url: String? = null,
    val image_urls: List<String>? = null,
    val brand: String? = null,
    val category: String? = null,
    val reason: String? = null,
    val source: String? = null,
    val index: Int = 0,
    val total: Int = 0,
)
