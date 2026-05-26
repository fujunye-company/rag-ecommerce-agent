package com.shopping.agent.data.mock

import com.shopping.agent.data.model.Product

/** 探索页 Mock — 复用 MockCategoryProducts */
object MockExploreProducts {
    val products: List<Product> = MockCategoryProducts.allProducts
    val categories: List<String> = MockCategoryProducts.categoryTabs
}
