package com.shopping.agent.data.model

/**
 * 商品 DTO — 对齐后端 ProductCardEvent schema
 * 后端字段: product_id, title, price, rating, match_score, highlights, image_url, index, total
 */
data class Product(
    val id: String,
    val name: String,
    val price: Double,
    val imageUrl: String,
    val highlights: List<String> = emptyList(),
    val matchScore: Double = 0.0,
    // 扩展字段
    val brand: String? = null,
    val category: String? = null,
    val rating: Double = 0.0,
    val index: Int = 0,
    val total: Int = 0,
)
