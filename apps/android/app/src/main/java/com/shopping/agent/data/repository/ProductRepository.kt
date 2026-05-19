package com.shopping.agent.data.repository

import com.shopping.agent.data.model.Product

/**
 * 商品仓库 — 获取列表/详情
 */
class ProductRepository {
    private val _products = mutableListOf<Product>()
    private val _detail = mutableMapOf<String, Product>()

    val products: List<Product> get() = _products.toList()

    fun getDetail(id: String): Product? = _detail[id]
}
