package com.shopping.agent.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.shopping.agent.core.network.NetworkConfig
import com.shopping.agent.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

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
    private val cartPrefs = application.getSharedPreferences("cart_prefs", Context.MODE_PRIVATE)

    private val client = NetworkConfig.httpClient

    private val baseUrl = NetworkConfig.BASE_URL

    private val sessionId: String
        get() {
            val existing = cartPrefs.getString("cart_session_id", null)
            if (existing != null) return existing
            val newId = java.util.UUID.randomUUID().toString()
            cartPrefs.edit().putString("cart_session_id", newId).apply()
            return newId
        }

    fun loadProduct(productId: String) {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            // 优先从后端 API 获取商品详情
            val apiDetail = fetchProductFromApi(productId)
            val detail = apiDetail ?: buildMockDetail(productId)
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
    }

    private suspend fun fetchProductFromApi(productId: String): ProductDetailData? {
        return withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url("$baseUrl/api/v1/products/$productId")
                    .get()
                    .build()
                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: return@withContext null
                    val json = JSONObject(body)
                    val data = json.optJSONObject("data") ?: return@withContext null
                    buildDetailFromApiJson(data)
                } else null
            } catch (e: Exception) {
                android.util.Log.e("ProductDetailVM", "API fetch failed: ${e.message}", e)
                null
            }
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

    fun addToCart() {
        val product = _uiState.value.product ?: return
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val body = JSONObject().apply {
                        put("session_id", sessionId)
                        put("product_id", product.productId)
                    }
                    val request = Request.Builder()
                        .url("$baseUrl/api/v1/cart/add")
                        .post(body.toString().toRequestBody("application/json".toMediaType()))
                        .build()
                    client.newCall(request).execute()
                }
                _uiState.update { it.copy(addToCartResult = "已将「${product.title.take(12)}…」加入购物车") }
            } catch (e: Exception) {
                android.util.Log.e("ProductDetailVM", "addToCart failed: ${e.message}", e)
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

        fun buildDetailFromApiJson(data: JSONObject): ProductDetailData {
            val productId = data.optString("product_id", "")
            val title = data.optString("title", "")
            val price = data.optDouble("price", 0.0)
            val rating = data.optDouble("rating", 3.0)
            val ratingCount = data.optInt("rating_count", 0)
            val brand = data.optString("brand").takeIf { data.has("brand") && !data.isNull("brand") }

            val highlights = mutableListOf<String>()
            val highlightsArr = data.optJSONArray("highlights")
            if (highlightsArr != null) {
                for (i in 0 until highlightsArr.length()) {
                    highlights.add(highlightsArr.optString(i, ""))
                }
            }

            val attributes = mutableMapOf<String, String>()
            val attrsObj = data.optJSONObject("attributes")
            if (attrsObj != null) {
                for (key in attrsObj.keys()) {
                    attributes[key] = attrsObj.optString(key, "")
                }
            }

            val imageUrls = mutableListOf<String>()
            val imgArr = data.optJSONArray("image_urls")
            if (imgArr != null && imgArr.length() > 0) {
                for (i in 0 until imgArr.length()) {
                    val url = NetworkConfig.resolveImageUrl(imgArr.optString(i, "")) ?: continue
                    imageUrls.add(url)
                }
            }
            if (imageUrls.isEmpty()) {
                val singleImg = NetworkConfig.resolveImageUrl(
                    data.optString("image_url").takeIf { data.has("image_url") && !data.isNull("image_url") }
                )
                if (singleImg != null) imageUrls.add(singleImg)
            }
            if (imageUrls.isEmpty()) {
                imageUrls.add("https://picsum.photos/seed/$productId/400/400")
            }

            val originPrice = if (price > 0) price * 1.3 else null
            val salesCount = ((ratingCount * 1.2).toInt().coerceAtLeast(1) * 100) + (productId.hashCode() % 100)
            val brandName = brand ?: "品牌"

            return ProductDetailData(
                productId = productId,
                images = imageUrls,
                campaign = null,
                price = PriceInfo(
                    current = price,
                    couponPrice = null,
                    origin = originPrice?.let { String.format("%.1f", it).toDouble() },
                    savedAmount = originPrice?.let { ((it - price).toInt()) } ?: 0,
                    salesText = "已售 ${salesCount}+",
                ),
                title = title,
                tags = highlights.take(3),
                coupons = emptyList(),
                delivery = DeliveryInfo(
                    estimate = "预计 48 小时内发货",
                    location = "广东广州",
                    shipping = if (price >= 99) "免运费" else "运费 ¥8",
                ),
                guarantee = listOf("假一赔十", "7天无理由退货", "正品保障"),
                specs = attributes.map { (k, v) -> SpecItem(k, v) },
                reviews = ReviewSummary(
                    count = ratingCount,
                    goodRate = "${((rating / 5.0) * 100).toInt()}%",
                    keywords = highlights.take(4),
                ),
                shop = ShopInfo(
                    name = "${brandName}官方旗舰店",
                    badge = "品牌认证",
                    score = rating,
                    fans = "${(ratingCount * 0.3 * 1000).toInt()}粉丝",
                    qualityScore = String.format("%.1f", rating.coerceAtMost(4.9)),
                    deliveryScore = String.format("%.1f", rating.coerceAtMost(4.8)),
                    serviceScore = String.format("%.1f", (rating + 0.1).coerceAtMost(5.0)),
                ),
            )
        }

        private fun buildDetailFromProduct(p: com.shopping.agent.data.model.Product): ProductDetailData {
            val imgList = if (p.imageUrls.isNotEmpty()) p.imageUrls
            else listOf(NetworkConfig.resolveImageUrl(p.imageUrl) ?: "https://picsum.photos/seed/${p.productId}/400/400")
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
