package com.shopping.agent.data.remote

import com.shopping.agent.data.model.Feedback
import com.shopping.agent.data.model.Product

/**
 * API 服务接口 — M1 占位，M2+ 接入 Retrofit
 */
interface ApiService {
    suspend fun getProducts(
        category: String? = null,
        page: Int = 1,
        size: Int = 20,
    ): List<Product>

    suspend fun getProductDetail(id: String): Product

    suspend fun submitFeedback(feedback: Feedback)
}
