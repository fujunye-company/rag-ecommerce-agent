package com.shopping.agent.data

import com.shopping.agent.data.model.Product
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 商品 DTO 反序列化测试
 */
class ProductDtoTest {
    @Test
    fun `product DTO has required MVP fields`() {
        val product = Product(
            id = "p001",
            name = "Test",
            price = 99.0,
            imageUrl = "https://example.com/img.jpg",
            reason = "测试推荐理由"
        )
        assertEquals("p001", product.id)
        assertEquals("Test", product.name)
        assertEquals(99.0, product.price, 0.01)
    }

    @Test
    fun `product DTO accepts full-scale extension fields`() {
        val product = Product(
            id = "p001", name = "Test", price = 99.0,
            imageUrl = "", reason = "",
            brand = "BrandA", category = "数码/耳机",
            rating = 4.5, tags = listOf("降噪", "长续航"),
            matchScore = 0.87
        )
        assertEquals("BrandA", product.brand)
        assertEquals(4.5, product.rating, 0.01)
        assertEquals(0.87, product.matchScore, 0.01)
    }
}
