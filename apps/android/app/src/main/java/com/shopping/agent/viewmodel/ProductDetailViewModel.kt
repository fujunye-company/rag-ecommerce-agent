package com.shopping.agent.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.shopping.agent.data.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

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

    fun loadProduct(productId: String) {
        _uiState.update { it.copy(isLoading = true) }
        // 用 mock 数据驱动 — 后续可替换为 API 调用
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

    fun addToCart() {
        val product = _uiState.value.product ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(addToCartResult = "已将「${product.title.take(12)}…」加入购物车") }
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
        fun buildMockDetail(productId: String): ProductDetailData = ProductDetailData(
            productId = productId,
            images = listOf(
                "https://placehold.co/600x600/F0F4FF/4A90D9?text=Product+Main+01",
                "https://placehold.co/600x600/FFF5F0/E8917E?text=Product+Main+02",
                "https://placehold.co/600x600/F0FFF4/2ECC71?text=Product+Detail+03",
            ),
            campaign = CampaignInfo(
                title = "618 抢先购",
                subsidyText = "平台礼金补贴 7 元",
                specBubble = "500ml 大容量",
            ),
            price = PriceInfo(
                current = 42.9,
                couponPrice = 35.9,
                origin = 58.8,
                savedAmount = 32,
                salesText = "已售 100+",
            ),
            title = "山乘酿造 冰茶奇兰乌龙茶西瓜冬瓜茶椰香葡萄酒果酒精酿啤酒茶啤 500ml*3度",
            tags = listOf("店铺新品", "一年回头客4千+", "超200人加购"),
            coupons = listOf(
                CouponInfo(type = "coupon", text = "满200减25"),
                CouponInfo(type = "subsidy", text = "平台礼金补贴7元"),
            ),
            delivery = DeliveryInfo(
                estimate = "预计 6 小时内发货",
                location = "浙江绍兴",
                shipping = "免运费",
            ),
            guarantee = listOf("拾物价保", "假一赔十", "7天无理由退货"),
            specs = listOf(
                SpecItem("酒精度数", "3度"),
                SpecItem("产地", "绍兴市"),
                SpecItem("净含量", "500ml"),
                SpecItem("保质期", "12个月"),
            ),
            reviews = ReviewSummary(
                count = 12,
                goodRate = "100%",
                keywords = listOf("回头客81", "颜值很高7", "甜而不腻2", "保质期长4"),
            ),
            shop = ShopInfo(
                name = "山乘酿造旗舰店",
                badge = "拾物认证",
                score = 4.8,
                fans = "1.5万粉丝",
                qualityScore = "4.8 高",
                deliveryScore = "4.8 高",
                serviceScore = "4.9 高",
            ),
        )
    }
}
