package com.shopping.agent.data.model

/**
 * 商品 DTO — 前后端字段一致
 * 遵循开发规约 v2.0 §8
 */
data class Product(
    val id: String,
    val name: String,
    val price: Double,
    val imageUrl: String,
    val reason: String,
    // 全量扩展字段
    val brand: String? = null,
    val category: String? = null,
    val rating: Double = 0.0,
    val tags: List<String> = emptyList(),
    val matchScore: Double = 0.0,
)
