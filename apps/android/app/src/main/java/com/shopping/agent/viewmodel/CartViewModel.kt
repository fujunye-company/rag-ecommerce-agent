package com.shopping.agent.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.shopping.agent.core.network.NetworkConfig
import com.shopping.agent.data.model.CartItem
import com.shopping.agent.data.model.Product
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class CartUiState(
    val items: List<CartItem> = emptyList(),
    val totalPrice: Double = 0.0,
    val isLoading: Boolean = false,
    val error: String? = null,
    val orderResult: String? = null,  // 下单结果信息
)

class CartViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(CartUiState())
    val uiState: StateFlow<CartUiState> = _uiState.asStateFlow()

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val baseUrl = NetworkConfig.BASE_URL
    private val prefs = application.getSharedPreferences("cart_prefs", Context.MODE_PRIVATE)

    // 持久化 sessionId：安装后首次生成，之后复用
    private val sessionId: String
        get() {
            val existing = prefs.getString("cart_session_id", null)
            if (existing != null) return existing
            val newId = java.util.UUID.randomUUID().toString()
            prefs.edit().putString("cart_session_id", newId).apply()
            return newId
        }

    fun loadCart() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val response = withContext(Dispatchers.IO) {
                    val request = Request.Builder()
                        .url("$baseUrl/api/v1/cart?session_id=$sessionId")
                        .get()
                        .build()
                    client.newCall(request).execute()
                }
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: "{}"
                    val json = JSONObject(body)
                    val itemsArray = json.optJSONArray("items") ?: org.json.JSONArray()
                    val items = mutableListOf<CartItem>()
                    for (i in 0 until itemsArray.length()) {
                        val item = itemsArray.getJSONObject(i)
                        val product = Product(
                            productId = item.optString("product_id", ""),
                            title = item.optString("title", ""),
                            price = item.optDouble("price", 0.0),
                            imageUrl = item.optString("image_url", null).takeIf { it != "null" },
                            brand = item.optString("brand", null).takeIf { it != "null" },
                            category = item.optString("category", ""),
                        )
                        items.add(CartItem(product, item.optInt("quantity", 1)))
                    }
                    val total = items.sumOf { it.product.price * it.quantity }
                    _uiState.update { it.copy(items = items, totalPrice = total, isLoading = false) }
                } else {
                    _uiState.update { it.copy(isLoading = false, error = "加载失败") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun addToCart(product: Product) {
        viewModelScope.launch {
            val existing = _uiState.value.items.find { it.product.productId == product.productId }
            if (existing != null) {
                _uiState.update {
                    val updated = it.items.map { item ->
                        if (item.product.productId == product.productId)
                            item.copy(quantity = item.quantity + 1)
                        else item
                    }
                    it.copy(items = updated, totalPrice = updated.sumOf { c -> c.product.price * c.quantity })
                }
            } else {
                val newItem = CartItem(product, 1)
                _uiState.update {
                    val updated = it.items + newItem
                    it.copy(items = updated, totalPrice = updated.sumOf { c -> c.product.price * c.quantity })
                }
            }

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
            } catch (e: Exception) {
                android.util.Log.e("CartViewModel", "addToCart server sync failed", e)
                _uiState.update { it.copy(error = "加购同步失败，请下拉刷新") }
            }
        }
    }

    fun removeFromCart(productId: String) {
        viewModelScope.launch {
            val previousItems = _uiState.value.items
            val previousTotal = _uiState.value.totalPrice
            _uiState.update {
                val updated = it.items.filter { item -> item.product.productId != productId }
                it.copy(items = updated, totalPrice = updated.sumOf { c -> c.product.price * c.quantity })
            }
            try {
                withContext(Dispatchers.IO) {
                    val body = JSONObject().apply {
                        put("session_id", sessionId)
                        put("product_id", productId)
                    }
                    val request = Request.Builder()
                        .url("$baseUrl/api/v1/cart/remove")
                        .post(body.toString().toRequestBody("application/json".toMediaType()))
                        .build()
                    client.newCall(request).execute()
                }
            } catch (e: Exception) {
                android.util.Log.e("CartViewModel", "removeFromCart server sync failed", e)
                _uiState.update { it.copy(items = previousItems, totalPrice = previousTotal, error = "删除同步失败，请下拉刷新") }
            }
        }
    }

    fun updateQuantity(productId: String, quantity: Int) {
        if (quantity <= 0) { removeFromCart(productId); return }
        viewModelScope.launch {
            val previousItems = _uiState.value.items
            val previousTotal = _uiState.value.totalPrice
            _uiState.update {
                val updated = it.items.map { item ->
                    if (item.product.productId == productId) item.copy(quantity = quantity)
                    else item
                }
                it.copy(items = updated, totalPrice = updated.sumOf { c -> c.product.price * c.quantity })
            }
            try {
                withContext(Dispatchers.IO) {
                    val body = JSONObject().apply {
                        put("session_id", sessionId)
                        put("product_id", productId)
                        put("quantity", quantity)
                    }
                    val request = Request.Builder()
                        .url("$baseUrl/api/v1/cart/quantity")
                        .put(body.toString().toRequestBody("application/json".toMediaType()))
                        .build()
                    client.newCall(request).execute()
                }
            } catch (e: Exception) {
                android.util.Log.e("CartViewModel", "updateQuantity server sync failed", e)
                _uiState.update { it.copy(items = previousItems, totalPrice = previousTotal, error = "修改同步失败，请下拉刷新") }
            }
        }
    }

    fun clearCart() {
        viewModelScope.launch {
            val previousItems = _uiState.value.items
            val previousTotal = _uiState.value.totalPrice
            _uiState.update { it.copy(items = emptyList(), totalPrice = 0.0) }
            try {
                withContext(Dispatchers.IO) {
                    val body = JSONObject().apply { put("session_id", sessionId) }
                    val request = Request.Builder()
                        .url("$baseUrl/api/v1/cart/clear")
                        .post(body.toString().toRequestBody("application/json".toMediaType()))
                        .build()
                    client.newCall(request).execute()
                }
            } catch (e: Exception) {
                android.util.Log.e("CartViewModel", "clearCart server sync failed", e)
                _uiState.update { it.copy(items = previousItems, totalPrice = previousTotal, error = "清空同步失败，请下拉刷新") }
            }
        }
    }

    fun placeOrder() {
        if (_uiState.value.items.isEmpty()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val response = withContext(Dispatchers.IO) {
                    val body = JSONObject().apply {
                        put("session_id", sessionId)
                        put("address", "默认地址")
                    }
                    val request = Request.Builder()
                        .url("$baseUrl/api/v1/orders")
                        .post(body.toString().toRequestBody("application/json".toMediaType()))
                        .build()
                    client.newCall(request).execute()
                }
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: "{}"
                    val json = JSONObject(body)
                    val orderNo = json.optString("order_no", "")
                    val total = json.optDouble("total", 0.0)
                    _uiState.update {
                        it.copy(
                            items = emptyList(),
                            totalPrice = 0.0,
                            isLoading = false,
                            orderResult = "下单成功！\n订单号：$orderNo\n实付：¥${"%.2f".format(total)}"
                        )
                    }
                } else {
                    val body = response.body?.string() ?: ""
                    _uiState.update { it.copy(isLoading = false, error = "下单失败：${response.code}") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = "网络错误：${e.message}") }
            }
        }
    }

    fun dismissOrderResult() {
        _uiState.update { it.copy(orderResult = null) }
    }

    val itemCount: Int get() = _uiState.value.items.sumOf { it.quantity }
}
