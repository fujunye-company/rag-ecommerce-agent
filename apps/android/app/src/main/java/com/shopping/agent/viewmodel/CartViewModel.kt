package com.shopping.agent.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.shopping.agent.data.local.UserRepository
import com.shopping.agent.data.model.CartItem
import com.shopping.agent.data.model.Product
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** 购物车 UI 状态 */
data class CartUiState(
    /** 购物车全部商品列表 */
    val items: List<CartItem> = emptyList(),
    /** 是否处于管理模式 */
    val isManageMode: Boolean = false,
    /** 当前选中的商品 productId 集合（仅当前缓存生效） */
    val selectedProductIds: Set<String> = emptySet(),
    /** 选中商品总价（两位小数） */
    val selectedTotalPrice: Double = 0.0,
    /** 全部商品总价 */
    val totalPrice: Double = 0.0,
    /** 是否正在加载 */
    val isLoading: Boolean = false,
    /** 加载错误信息 */
    val error: String? = null,
    /** 下单结果信息 */
    val orderResult: String? = null,
    /** 快速清理面板是否可见 */
    val showQuickClean: Boolean = false,
    /** 快速清理中选中的商品 productId 集合 */
    val quickCleanSelectedIds: Set<String> = emptySet(),
)

/** 购物车商品数量 */
val CartUiState.itemCount: Int get() = items.size

class CartViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(CartUiState())
    val uiState: StateFlow<CartUiState> = _uiState.asStateFlow()

    private val repository = UserRepository(application)

    /** 会话 ID，用于兼容旧数据 */
    private val sessionId: String by lazy {
        val prefs = application.getSharedPreferences("cart_prefs", android.content.Context.MODE_PRIVATE)
        val existing = prefs.getString("cart_session_id", null)
        if (existing != null) {
            existing
        } else {
            val newId = java.util.UUID.randomUUID().toString()
            prefs.edit().putString("cart_session_id", newId).apply()
            newId
        }
    }

    /** 加载购物车数据 */
    fun loadCart() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val items = withContext(Dispatchers.IO) {
                    repository.getCartItemsForCurrentUser(sessionId)
                }
                val total = items.sumOf { it.product.price * it.quantity }
                val selectedIds = items.filter { it.isSelected }.map { it.product.productId }.toSet()
                val selectedTotal = items.filter { it.isSelected }.sumOf { it.product.price * it.quantity }
                _uiState.update {
                    it.copy(
                        items = items,
                        totalPrice = total,
                        selectedProductIds = selectedIds,
                        selectedTotalPrice = selectedTotal,
                        isLoading = false,
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    /** 切换管理模式 */
    fun toggleManageMode() {
        val currentState = _uiState.value
        if (currentState.isManageMode) {
            // 退出管理模式：从数据库重新加载选中状态
            viewModelScope.launch {
                try {
                    val items = withContext(Dispatchers.IO) {
                        repository.getCartItemsForCurrentUser(sessionId)
                    }
                    val selectedIds = items.filter { it.isSelected }.map { it.product.productId }.toSet()
                    val selectedTotal = items.filter { it.isSelected }.sumOf { it.product.price * it.quantity }
                    _uiState.update {
                        it.copy(
                            isManageMode = false,
                            items = items,
                            selectedProductIds = selectedIds,
                            selectedTotalPrice = selectedTotal,
                        )
                    }
                } catch (_: Exception) {
                    _uiState.update { it.copy(isManageMode = false) }
                }
            }
        } else {
            // 进入管理模式：清空选中缓存，不从数据库读取
            _uiState.update {
                it.copy(isManageMode = true, selectedProductIds = emptySet(), selectedTotalPrice = 0.0)
            }
        }
    }

    /** 切换单个商品选中状态 */
    fun toggleItemSelection(productId: String) {
        _uiState.update { state ->
            val newSelected = state.selectedProductIds.toMutableSet()
            if (newSelected.contains(productId)) {
                newSelected.remove(productId)
            } else {
                newSelected.add(productId)
            }
            val selectedTotal = state.items
                .filter { newSelected.contains(it.product.productId) }
                .sumOf { it.product.price * it.quantity }
            state.copy(
                selectedProductIds = newSelected,
                selectedTotalPrice = selectedTotal,
            )
        }
    }

    /** 全选 / 取消全选所有商品 */
    fun toggleSelectAll() {
        _uiState.update { state ->
            val allIds = state.items.map { it.product.productId }.toSet()
            val allSelected = allIds.all { state.selectedProductIds.contains(it) }
            val newSelected = if (allSelected) {
                emptySet<String>()
            } else {
                allIds
            }
            val selectedTotal = state.items
                .filter { newSelected.contains(it.product.productId) }
                .sumOf { it.product.price * it.quantity }
            state.copy(
                selectedProductIds = newSelected,
                selectedTotalPrice = selectedTotal,
            )
        }
    }

    /** 全选 / 取消全选指定商家的商品 */
    fun toggleBrandSelection(brand: String) {
        _uiState.update { state ->
            val brandProductIds = state.items
                .filter { it.product.brand == brand }
                .map { it.product.productId }
                .toSet()
            val allBrandSelected = brandProductIds.all { state.selectedProductIds.contains(it) }
            val newSelected = state.selectedProductIds.toMutableSet()
            if (allBrandSelected) {
                newSelected.removeAll(brandProductIds)
            } else {
                newSelected.addAll(brandProductIds)
            }
            val selectedTotal = state.items
                .filter { newSelected.contains(it.product.productId) }
                .sumOf { it.product.price * it.quantity }
            state.copy(
                selectedProductIds = newSelected,
                selectedTotalPrice = selectedTotal,
            )
        }
    }

    /** 检查指定商家商品是否全部选中 */
    fun isBrandFullySelected(brand: String): Boolean {
        val state = _uiState.value
        val brandProductIds = state.items
            .filter { it.product.brand == brand }
            .map { it.product.productId }
            .toSet()
        if (brandProductIds.isEmpty()) return false
        return brandProductIds.all { state.selectedProductIds.contains(it) }
    }

    /** 增加商品数量 */
    fun increaseQuantity(productId: String) {
        _uiState.update { state ->
            val updated = state.items.map { item ->
                if (item.product.productId == productId && item.quantity < MAX_QUANTITY) {
                    item.copy(quantity = item.quantity + 1)
                } else item
            }
            val total = updated.sumOf { it.product.price * it.quantity }
            val selectedTotal = updated
                .filter { state.selectedProductIds.contains(it.product.productId) }
                .sumOf { it.product.price * it.quantity }
            state.copy(items = updated, totalPrice = total, selectedTotalPrice = selectedTotal)
        }
        // 异步持久化到数据库
        viewModelScope.launch {
            val item = _uiState.value.items.find { it.product.productId == productId } ?: return@launch
            withContext(Dispatchers.IO) {
                repository.updateCartItemQuantity(productId, item.quantity)
            }
        }
    }

    /** 减少商品数量，减至 0 则删除 */
    fun decreaseQuantity(productId: String) {
        val item = _uiState.value.items.find { it.product.productId == productId } ?: return
        if (item.quantity <= 1) {
            removeFromCart(productId)
            return
        }
        _uiState.update { state ->
            val updated = state.items.map { item ->
                if (item.product.productId == productId) {
                    item.copy(quantity = item.quantity - 1)
                } else item
            }
            val total = updated.sumOf { it.product.price * it.quantity }
            val selectedTotal = updated
                .filter { state.selectedProductIds.contains(it.product.productId) }
                .sumOf { it.product.price * it.quantity }
            state.copy(items = updated, totalPrice = total, selectedTotalPrice = selectedTotal)
        }
        viewModelScope.launch {
            val updatedItem = _uiState.value.items.find { it.product.productId == productId } ?: return@launch
            withContext(Dispatchers.IO) {
                repository.updateCartItemQuantity(productId, updatedItem.quantity)
            }
        }
    }

    /** 更新商品数量（用户手动输入） */
    fun updateQuantity(productId: String, quantity: Int) {
        val clamped = quantity.coerceIn(1, MAX_QUANTITY)
        _uiState.update { state ->
            val updated = state.items.map { item ->
                if (item.product.productId == productId) {
                    item.copy(quantity = clamped)
                } else item
            }
            val total = updated.sumOf { it.product.price * it.quantity }
            val selectedTotal = updated
                .filter { state.selectedProductIds.contains(it.product.productId) }
                .sumOf { it.product.price * it.quantity }
            state.copy(items = updated, totalPrice = total, selectedTotalPrice = selectedTotal)
        }
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                repository.updateCartItemQuantity(productId, clamped)
            }
        }
    }

    /** 从购物车移除单件商品 */
    fun removeFromCart(productId: String) {
        _uiState.update { state ->
            val updated = state.items.filter { it.product.productId != productId }
            val newSelected = state.selectedProductIds - productId
            val total = updated.sumOf { it.product.price * it.quantity }
            val selectedTotal = updated
                .filter { newSelected.contains(it.product.productId) }
                .sumOf { it.product.price * it.quantity }
            state.copy(
                items = updated,
                totalPrice = total,
                selectedProductIds = newSelected,
                selectedTotalPrice = selectedTotal,
            )
        }
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                repository.removeCartItem(productId)
            }
        }
    }

    /** 执行删除选中商品（含数据库操作） */
    fun performDeleteSelected() {
        val idsToDelete = _uiState.value.selectedProductIds.toList()
        if (idsToDelete.isEmpty()) return
        _uiState.update { state ->
            val updated = state.items.filter { it.product.productId !in idsToDelete }
            val total = updated.sumOf { it.product.price * it.quantity }
            state.copy(
                items = updated,
                totalPrice = total,
                selectedProductIds = emptySet(),
                selectedTotalPrice = 0.0,
            )
        }
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                repository.deleteCartItems(idsToDelete)
            }
        }
    }

    /** 添加商品到购物车 */
    fun addToCart(product: Product) {
        viewModelScope.launch {
            val existing = _uiState.value.items.find { it.product.productId == product.productId }
            if (existing != null) {
                updateQuantity(product.productId, existing.quantity + 1)
            } else {
                val newItem = CartItem(product, 1, isSelected = true)
                _uiState.update { state ->
                    val updated = state.items + newItem
                    val total = updated.sumOf { it.product.price * it.quantity }
                    val newSelected = state.selectedProductIds + product.productId
                    val selectedTotal = updated
                        .filter { newSelected.contains(it.product.productId) }
                        .sumOf { it.product.price * it.quantity }
                    state.copy(
                        items = updated,
                        totalPrice = total,
                        selectedProductIds = newSelected,
                        selectedTotalPrice = selectedTotal,
                    )
                }
                withContext(Dispatchers.IO) {
                    repository.saveCartItemForCurrentUser(product, sessionId, 1)
                }
            }
        }
    }

    /** 清空购物车 */
    fun clearCart() {
        _uiState.update { it.copy(items = emptyList(), totalPrice = 0.0, selectedProductIds = emptySet(), selectedTotalPrice = 0.0) }
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                repository.clearCart(sessionId)
            }
        }
    }

    /** 下单 */
    fun placeOrder() {
        if (_uiState.value.items.isEmpty()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                // 使用选中的商品下单，若未选中任何商品则用全部商品
                val selectedItems = _uiState.value.items.filter {
                    _uiState.value.selectedProductIds.contains(it.product.productId)
                }.ifEmpty { _uiState.value.items }
                val orderNo = "ORD${System.currentTimeMillis()}"
                val total = selectedItems.sumOf { it.product.price * it.quantity }
                _uiState.update {
                    it.copy(
                        items = emptyList(),
                        totalPrice = 0.0,
                        selectedProductIds = emptySet(),
                        selectedTotalPrice = 0.0,
                        isLoading = false,
                        orderResult = "下单成功！\n订单号：$orderNo\n实付：¥${"%.2f".format(total)}"
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = "网络错误：${e.message}") }
            }
        }
    }

    /** 关闭下单成功弹窗 */
    fun dismissOrderResult() {
        _uiState.update { it.copy(orderResult = null) }
    }

    // ═══════════════════════════════════════════════════════
    // 快速清理面板
    // ═══════════════════════════════════════════════════════

    /** 打开快速清理面板 */
    fun openQuickClean() {
        _uiState.update { it.copy(showQuickClean = true, quickCleanSelectedIds = emptySet()) }
    }

    /** 关闭快速清理面板 */
    fun closeQuickClean() {
        _uiState.update { it.copy(showQuickClean = false, quickCleanSelectedIds = emptySet()) }
    }

    /** 切换快速清理中商品的选中状态 */
    fun toggleQuickCleanSelection(productId: String) {
        _uiState.update { state ->
            val newSet = state.quickCleanSelectedIds.toMutableSet()
            if (newSet.contains(productId)) {
                newSet.remove(productId)
            } else {
                newSet.add(productId)
            }
            state.copy(quickCleanSelectedIds = newSet)
        }
    }

    /** 快速清理中全选/取消全选 */
    fun toggleQuickCleanSelectAll() {
        _uiState.update { state ->
            val allIds = state.items.map { it.product.productId }.toSet()
            val allSelected = allIds.all { state.quickCleanSelectedIds.contains(it) }
            state.copy(
                quickCleanSelectedIds = if (allSelected) emptySet() else allIds
            )
        }
    }

    /** 快速清理 — 删除选中的商品 */
    fun performQuickCleanDelete() {
        val idsToDelete = _uiState.value.quickCleanSelectedIds.toList()
        if (idsToDelete.isEmpty()) return
        _uiState.update { state ->
            val updated = state.items.filter { it.product.productId !in idsToDelete }
            val total = updated.sumOf { it.product.price * it.quantity }
            val newSelected = state.selectedProductIds - idsToDelete.toSet()
            val selectedTotal = updated
                .filter { newSelected.contains(it.product.productId) }
                .sumOf { it.product.price * it.quantity }
            state.copy(
                items = updated,
                totalPrice = total,
                selectedProductIds = newSelected,
                selectedTotalPrice = selectedTotal,
                showQuickClean = false,
                quickCleanSelectedIds = emptySet(),
            )
        }
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                repository.deleteCartItems(idsToDelete)
            }
        }
    }

    companion object {
        /** 商品数量最大值 */
        const val MAX_QUANTITY = 1000
    }
}
