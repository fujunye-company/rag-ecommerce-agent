package com.shopping.agent.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.shopping.agent.data.local.UserRepository
import com.shopping.agent.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ProductDetailUiState(
    val product: ProductDetailData? = null,
    val isLoading: Boolean = true,
    val isFavorited: Boolean = false,
    val isFollowingShop: Boolean = false,
    val addToCartResult: String? = null,
    val error: String? = null,
)

class ProductDetailViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(ProductDetailUiState())
    val uiState: StateFlow<ProductDetailUiState> = _uiState.asStateFlow()

    private val prefs = application.getSharedPreferences("detail_prefs", Context.MODE_PRIVATE)
    private val repository = UserRepository(application)

    /** 加载商品时缓存的 Product 对象，用于加购时写入数据库 */
    private var cachedProduct: Product? = null

    fun loadProduct(productId: String) {
        _uiState.update { it.copy(isLoading = true) }
        // 用 mock 数据驱动 — 后续可替换为 API 调用
        val sourceProduct = com.shopping.agent.data.mock.mockProducts.find { it.productId == productId }
        cachedProduct = sourceProduct
        val detail = buildMockDetail(productId)
        val faved = prefs.getBoolean("fav_$productId", false)
        val following = prefs.getBoolean("follow_${detail.shop.name}", false)
        _uiState.update {
            it.copy(
                product = detail,
                isLoading = false,
                isFavorited = faved,
                isFollowingShop = following,
            )
        }
    }

    fun toggleFavorite() {
        val product = _uiState.value.product ?: return
        val newState = !_uiState.value.isFavorited
        prefs.edit().putBoolean("fav_${product.productId}", newState).apply()
        _uiState.update { it.copy(isFavorited = newState) }
    }

    fun toggleFollowShop() {
        val product = _uiState.value.product ?: return
        val newState = !_uiState.value.isFollowingShop
        prefs.edit().putBoolean("follow_${product.shop.name}", newState).apply()
        _uiState.update { it.copy(isFollowingShop = newState) }
    }

    /** 将当前商品加入购物车，写入本地 SQLite 数据库。 */
    fun addToCart() {
        val detail = _uiState.value.product ?: return
        val product = cachedProduct
        if (product == null) {
            _uiState.update { it.copy(addToCartResult = "加购失败：商品数据异常") }
            return
        }
        viewModelScope.launch {
            try {
                val cartPrefs = getApplication<Application>()
                    .getSharedPreferences("cart_prefs", Context.MODE_PRIVATE)
                val sessionId = cartPrefs.getString("cart_session_id", "") ?: ""
                withContext(Dispatchers.IO) {
                    repository.saveCartItemForCurrentUser(product, sessionId, 1)
                }
                _uiState.update {
                    it.copy(addToCartResult = "已将「${detail.title.take(12)}…」加入购物车")
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(addToCartResult = "加购失败：${e.message}") }
            }
        }
    }

    fun buyNow() {
        val product = _uiState.value.product ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(addToCartResult = "正在跳转下单：${product.title.take(12)}…") }
        }
    }

    fun dismissResult() {
        _uiState.update { it.copy(addToCartResult = null) }
    }

    companion object {
        fun buildMockDetail(productId: String): ProductDetailData {
            val product = com.shopping.agent.data.mock.mockProducts.find { it.productId == productId }
            if (product != null) {
                return buildDetailFromProduct(product)
            }
            // 兜底：未找到商品时显示占位信息
            return ProductDetailData(
                productId = productId,
                images = listOf("https://picsum.photos/seed/fallback/400/400"),
                campaign = null,
                price = PriceInfo(current = 0.0, origin = null),
                title = "商品未找到 ($productId)",
                tags = emptyList(),
                coupons = emptyList(),
                delivery = DeliveryInfo(estimate = "", location = "", shipping = ""),
                guarantee = emptyList(),
                specs = emptyList(),
                reviews = ReviewSummary(count = 0, goodRate = "", keywords = emptyList()),
                shop = ShopInfo(name = "未知店铺", badge = "", score = 0.0, fans = ""),
            )
        }

        private fun buildDetailFromProduct(p: com.shopping.agent.data.model.Product): ProductDetailData {
            val imgList = if (p.imageUrls.isNotEmpty()) p.imageUrls
            else listOf(p.imageUrl ?: "https://picsum.photos/seed/${p.productId}/400/400")
            val originPrice = if (p.price > 0) p.price * 1.3 else null
            val salesCount = ((p.ratingCount * 1.2).toInt().coerceAtLeast(1) * 100) + (p.productId.hashCode() % 100)
            val brandName = p.brand ?: "品牌"
            return ProductDetailData(
                productId = p.productId,
                images = imgList,
                campaign = null,
                price = PriceInfo(
                    current = p.price,
                    couponPrice = null,
                    origin = originPrice?.let { String.format("%.1f", it).toDouble() },
                    savedAmount = originPrice?.let { ((it - p.price).toInt()) } ?: 0,
                    salesText = "已售 ${salesCount}+",
                ),
                title = p.title,
                tags = p.rankReason.split("|").map { it.trim() }.filter { it.isNotBlank() }.take(3),
                coupons = emptyList(),
                delivery = DeliveryInfo(
                    estimate = "预计 48 小时内发货",
                    location = "广东广州",
                    shipping = if (p.price >= 99) "免运费" else "运费 ¥8",
                ),
                guarantee = listOf("假一赔十", "7天无理由退货", "正品保障"),
                specs = p.attributes.map { (k, v) -> SpecItem(k, v) },
                reviews = ReviewSummary(
                    count = p.ratingCount,
                    goodRate = "${((p.rating / 5.0) * 100).toInt()}%",
                    keywords = p.rankReason.split("|").map { it.trim() }.filter { it.isNotBlank() }.take(4),
                ),
                shop = ShopInfo(
                    name = "${brandName}官方旗舰店",
                    badge = "品牌认证",
                    score = p.rating.toDouble(),
                    fans = "${(p.ratingCount * 0.3 * 1000).toInt()}粉丝",
                    qualityScore = String.format("%.1f", p.rating.coerceAtMost(4.9f)),
                    deliveryScore = String.format("%.1f", p.rating.coerceAtMost(4.8f)),
                    serviceScore = String.format("%.1f", (p.rating + 0.1f).coerceAtMost(5.0f)),
                ),
            )
        }
    }
}
