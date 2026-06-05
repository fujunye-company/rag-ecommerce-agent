package com.shopping.agent.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.shopping.agent.core.network.NetworkConfig
import com.shopping.agent.data.local.UserRepository
import com.shopping.agent.data.mock.mockProducts
import com.shopping.agent.data.model.Product
import com.shopping.agent.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.json.JSONObject

/** 收藏页面管理状态枚举 */
private enum class FavoritesManageState {
    /** 正常浏览状态 — 显示收藏列表，点击卡片进入商品详情 */
    BROWSING,
    /** 管理模式 — 显示选择框，底部栏显示"取消"和"移除收藏"按钮 */
    MANAGING,
    /** 确认移除弹窗状态 */
    CONFIRM_REMOVE,
}

/**
 * 收藏页面。
 *
 * 布局结构：
 * - 顶部栏：< 返回 + "收藏"文本 + "管理"按钮
 * - 主体：收藏商品卡片列表（占满一行）
 * - 管理模式底部栏："取消" + "移除收藏"按钮
 * - 确认移除弹窗
 *
 * @param onBack 返回个人页面回调
 * @param onProductClick 点击商品卡片回调，传入商品 ID
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(
    onBack: () -> Unit = {},
    onProductClick: (String) -> Unit = {},
) {
    val context = LocalContext.current
    val repository = remember { UserRepository(context) }
    val scope = rememberCoroutineScope()

    var manageState by remember { mutableStateOf(FavoritesManageState.BROWSING) }
    var selectedIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var favoriteProducts by remember { mutableStateOf<List<Product>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    /** 上一次加载的 Job，用于取消并发加载 */
    var loadJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }

    /** 加载收藏商品列表 — 从 SQLite 读取收藏记录，mockProducts 优先，UUID 则调 API 获取完整信息 */
    fun loadFavorites() {
        // 取消前次加载，防止并发导致数据错乱
        loadJob?.cancel()
        loadJob = scope.launch {
            isLoading = true
            try {
                val records = withContext(Dispatchers.IO) {
                    repository.getFavorites()
                }
                // 分离匹配和未匹配的记录
                val (matched, unmatched) = records.partition { record ->
                    mockProducts.any { it.productId == record.productId }
                }
                // mockProducts 匹配的：直接使用完整商品数据
                val matchedProducts = matched.mapNotNull { record ->
                    mockProducts.find { it.productId == record.productId }
                }
                // UUID 等未匹配的：并行调 API 获取完整商品信息
                val apiProducts = if (unmatched.isNotEmpty()) {
                    withContext(Dispatchers.IO) {
                        unmatched.map { record ->
                            async<Product?> { fetchFavoriteProductFromApi(record.productId) }
                        }.awaitAll().mapNotNull { it }
                    }
                } else emptyList()
                // 合并并去重（防止 API 返回的 productId 与 mockProducts 碰撞）
                favoriteProducts = (matchedProducts + apiProducts).distinctBy { it.productId }
            } catch (_: Exception) {
                favoriteProducts = emptyList()
            }
            isLoading = false
        }
    }

    // 初始加载
    LaunchedEffect(Unit) {
        loadFavorites()
    }

    // 确认移除弹窗
    if (manageState == FavoritesManageState.CONFIRM_REMOVE) {
        AlertDialog(
            onDismissRequest = {
                manageState = FavoritesManageState.MANAGING
            },
            title = {
                Text("确认移除收藏吗？", fontWeight = FontWeight.Bold)
            },
            text = {
                Text("移除收藏后，将无法恢复，是否继续？")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            try {
                                // 从数据库删除选中的收藏记录
                                withContext(Dispatchers.IO) {
                                    repository.removeFavorites(selectedIds.toList())
                                }
                                // 同步到后端
                                withContext(Dispatchers.IO) {
                                    repository.syncFavoriteRemoveToBackend(selectedIds.toList())
                                }
                            } catch (_: Exception) {}
                            // 刷新列表
                            selectedIds = emptySet()
                            manageState = FavoritesManageState.BROWSING
                            loadFavorites()
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFFF5C5C)),
                ) {
                    Text("确认")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    manageState = FavoritesManageState.MANAGING
                }) {
                    Text("取消")
                }
            },
        )
    }

    Scaffold(
        topBar = {
            // 顶部栏
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = {
                    if (manageState == FavoritesManageState.MANAGING) {
                        // 管理模式按返回 → 退出管理模式
                        manageState = FavoritesManageState.BROWSING
                        selectedIds = emptySet()
                    } else {
                        onBack()
                    }
                }) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack, "返回",
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
                Text(
                    "收藏",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium,
                )

                Spacer(Modifier.weight(1f))

                // 管理按钮（屏幕右上角）
                TextButton(
                    onClick = {
                        if (manageState == FavoritesManageState.MANAGING) {
                            // 退出管理模式
                            manageState = FavoritesManageState.BROWSING
                            selectedIds = emptySet()
                        } else {
                            // 进入管理模式
                            manageState = FavoritesManageState.MANAGING
                        }
                    },
                ) {
                    Text(
                        if (manageState == FavoritesManageState.MANAGING) "完成" else "管理",
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        },
        bottomBar = {
            // 管理模式底部栏
            if (manageState == FavoritesManageState.MANAGING) {
                Surface(
                    shadowElevation = 8.dp,
                    color = MaterialTheme.colorScheme.surface,
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        // 取消按钮（默认背景）
                        OutlinedButton(
                            onClick = {
                                manageState = FavoritesManageState.BROWSING
                                selectedIds = emptySet()
                            },
                            modifier = Modifier.weight(1f).padding(end = 8.dp),
                        ) {
                            Text("取消")
                        }

                        // 移除收藏按钮（蓝色背景）
                        Button(
                            onClick = {
                                if (selectedIds.isNotEmpty()) {
                                    manageState = FavoritesManageState.CONFIRM_REMOVE
                                }
                            },
                            modifier = Modifier.weight(1f).padding(start = 8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Primary,
                            ),
                            enabled = selectedIds.isNotEmpty(),
                        ) {
                            Text("移除收藏", color = Color.White)
                        }
                    }
                }
            }
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background),
        ) {
            if (isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Primary)
                }
            } else if (favoriteProducts.isEmpty()) {
                // 空收藏状态
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Favorite,
                            "暂无收藏",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(48.dp),
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "暂无收藏",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            } else {
                // 收藏商品列表
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    itemsIndexed(favoriteProducts, key = { index, product -> "${product.productId}_$index" }) { _, product ->
                        FavoriteProductCard(
                            product = product,
                            isManageMode = manageState == FavoritesManageState.MANAGING,
                            isSelected = selectedIds.contains(product.productId),
                            onTap = {
                                if (manageState == FavoritesManageState.MANAGING) {
                                    // 管理模式：切换选中状态
                                    selectedIds = if (selectedIds.contains(product.productId)) {
                                        selectedIds - product.productId
                                    } else {
                                        selectedIds + product.productId
                                    }
                                } else {
                                    // 浏览模式：跳转商品详情
                                    onProductClick(product.productId)
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}

/** 从后端 API 获取商品信息并转为 Product 对象（用于 UUID 等 mockProducts 无法匹配的 ID） */
private suspend fun fetchFavoriteProductFromApi(productId: String): Product? {
    return try {
        val request = Request.Builder()
            .url("${NetworkConfig.BASE_URL}/api/v1/products/$productId")
            .get()
            .build()
        val response = NetworkConfig.httpClient.newCall(request).execute()
        if (!response.isSuccessful) return null
        val body = response.body?.string() ?: return null
        val data = JSONObject(body).optJSONObject("data") ?: return null
        val imageUrls = data.optJSONArray("image_urls")
        val firstImage = if (imageUrls != null && imageUrls.length() > 0) {
            NetworkConfig.resolveImageUrl(imageUrls.optString(0))
        } else {
            NetworkConfig.resolveImageUrl(data.optString("image_url").takeIf { it.isNotBlank() })
        }
        // productId 使用参数原始值（DB 中存储的 ID），保证与收藏表记录一致，移除/状态查询可正确匹配
        Product(
            productId = productId,
            title = data.optString("title", ""),
            price = data.optDouble("price", 0.0),
            brand = data.optString("brand").takeIf { it.isNotBlank() },
            category = data.optString("category", ""),
            imageUrl = firstImage,
            rating = data.optDouble("rating", 3.0).toFloat(),
        )
    } catch (_: Exception) {
        null
    }
}

/**
 * 收藏商品卡片 — 与主页商品卡片设计一致，占满一行。
 *
 * 管理模式：点击选中，左上角显示蓝色背景勾选图形。
 */
@Composable
private fun FavoriteProductCard(
    product: Product,
    isManageMode: Boolean,
    isSelected: Boolean,
    onTap: () -> Unit,
) {
    Card(
        onClick = onTap,
        shape = RadiusLg,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // 商品图片
            AsyncImage(
                model = product.imageUrl,
                contentDescription = product.title,
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop,
            )

            Spacer(Modifier.width(12.dp))

            // 商品信息
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    product.title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        "¥${product.price}",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrice,
                        fontWeight = FontWeight.Bold,
                    )
                }
                if (product.brand != null) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        product.brand,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(Modifier.width(8.dp))

            // 管理模式：选中状态图标（左上角蓝色背景勾选图形）
            if (isManageMode) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(
                            if (isSelected) Primary else MaterialTheme.colorScheme.outlineVariant
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    if (isSelected) {
                        Icon(
                            Icons.Default.Check,
                            "已选中",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
            }
        }
    }
}
