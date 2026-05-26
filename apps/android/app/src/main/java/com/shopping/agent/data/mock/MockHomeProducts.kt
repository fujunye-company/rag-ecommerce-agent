package com.shopping.agent.data.mock

import com.shopping.agent.data.model.Product

/** 首页推荐 Mock — 复用 MockProducts 精选子集 */
object MockHomeProducts {
    val products: List<Product> = mockProducts.toList().take(6)
}
