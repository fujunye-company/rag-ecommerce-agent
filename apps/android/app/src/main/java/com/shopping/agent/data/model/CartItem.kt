package com.shopping.agent.data.model

data class CartItem(
    val product: Product,
    val quantity: Int = 1,
)
