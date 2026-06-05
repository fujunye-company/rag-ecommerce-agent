package com.shopping.agent.data.model

/**
 * 探索页社交分享贴数据模型
 * 对应 ExploreProductPostScreen 的 Mock 数据结构
 */
data class ExplorePost(
    val postId: String,
    val author: PostAuthor,
    val product: PostProduct,
    val content: PostContent,
)

data class PostAuthor(
    val id: String,
    val name: String,
    val avatar: String,
)

data class PostProduct(
    val id: String,
    val category: String,
    val title: String,
    val images: List<String>,
)

data class PostContent(
    val title: String,
    val body: List<String>,
)
