package com.shopping.agent.data.mapper

import com.shopping.agent.data.model.ProductDto
import com.shopping.agent.data.model.Product

/**
 * ProductDto (SSE 后端) → Product (前端领域模型) 映射
 * 对齐 DATA-CONTRACT.md v1.0
 */
fun ProductDto.toProduct(): Product {
    return Product(
        productId = product_id,
        title = title,
        price = price,
        rating = rating?.toFloat() ?: 3.0f,
        highlights = highlights ?: emptyList(),
        imageUrl = image_url,
        imageUrls = image_urls ?: emptyList(),
        brand = brand,
        category = category ?: "",
        matchScore = match_score ?: 0.0,
        rankReason = reason ?: "",
        source = source ?: "AI 检索结果",
    )
}
