package com.shopping.agent.ui.screens

import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import java.util.Locale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.shopping.agent.data.mock.MockCompareData
import com.shopping.agent.data.repository.CompareRepository
import com.shopping.agent.ui.components.*
import com.shopping.agent.ui.navigation.LocalOnMenuClick
import com.shopping.agent.ui.theme.*
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

@Composable
fun CompareTabScreen() {
    var allProducts by remember { mutableStateOf(MockCompareData.products) }
    val categories = MockCompareData.categories
    var selectedCategory by remember { mutableStateOf(MockCompareData.defaultCategory) }
    var selectedProduct by remember { mutableStateOf<String?>(null) }

    // 本地搜索状态
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<com.shopping.agent.data.model.Product>>(emptyList()) }
    var hasSearched by remember { mutableStateOf(false) }

    // AI 对比状态
    var aiCompareLoading by remember { mutableStateOf(false) }
    var aiCompareResult by remember { mutableStateOf<com.shopping.agent.data.repository.CompareResult?>(null) }
    var showCompareDialog by remember { mutableStateOf(false) }
    val compareRepo = remember { CompareRepository() }

    // 启动时从后端拉取真实商品，失败则保留 mock 数据
    LaunchedEffect(Unit) {
        val real = compareRepo.fetchProducts()
        if (!real.isNullOrEmpty()) {
            allProducts = real
        }
    }

    // 按选中分类过滤（非搜索模式下）
    val categoryProducts by remember {
        derivedStateOf {
            when (selectedCategory) {
                "推荐" -> allProducts
                "全部" -> allProducts
                else -> allProducts.filter { it.category == selectedCategory }
            }
        }
    }

    // 决定显示哪些商品：搜索模式用搜索结果，否则按分类
    val displayProducts = if (hasSearched) searchResults else categoryProducts

    // 本地搜索逻辑
    fun doSearch(query: String) {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return

        val scored = allProducts.map { product ->
            val name = product.title.lowercase()
            val brand = (product.brand ?: "").lowercase()
            val cat = product.category.lowercase()
            var score = 0
            if (name.contains(q)) score += 10
            if (brand.contains(q)) score += 5
            if (cat.contains(q)) score += 3
            // 部分匹配加分
            q.split(" ").forEach { token ->
                if (token.length >= 2) {
                    if (name.contains(token)) score += 3
                    if (brand.contains(token)) score += 2
                }
            }
            product to score
        }.filter { it.second > 0 }
         .sortedByDescending { it.second }
         .map { it.first }

        searchResults = scored.take(10)
        hasSearched = true
        selectedCategory = "推荐"  // 自动切回推荐栏
    }

    Column(modifier = Modifier.fillMaxSize().background(Neutral50)) {
        // ===== 渐变条 =====
        GradientTopBar(
            icons = {
                IconButton(onClick = LocalOnMenuClick.current, modifier = Modifier.size(34.dp)) {
                    Icon(Icons.Default.Menu, "菜单", tint = Neutral700, modifier = Modifier.size(26.dp))
                }
                Spacer(Modifier.width(4.dp))
                FilledTonalButton(
                    onClick = {
                        aiCompareLoading = true
                        kotlinx.coroutines.MainScope().launch {
                            aiCompareResult = compareRepo.compareProducts(
                                displayProducts.take(5).map { it.productId }
                            )
                            aiCompareLoading = false
                            showCompareDialog = aiCompareResult != null
                        }
                    },
                    enabled = !aiCompareLoading && displayProducts.size >= 2,
                    shape = RadiusFull,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.height(34.dp)
                ) {
                    if (aiCompareLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Primary
                        )
                        Spacer(Modifier.width(6.dp))
                    }
                    Text("AI对比", style = MaterialTheme.typography.labelMedium)
                }
            }
        )

        // ===== 分类标签 =====
        ScrollableTabRow(
            selectedTabIndex = categories.indexOf(selectedCategory).coerceAtLeast(0),
            containerColor = Neutral0,
            edgePadding = 16.dp,
            divider = { HorizontalDivider(color = Neutral100) }) {
            categories.forEach { cat ->
                val isSelected = cat == selectedCategory
                Tab(selected = isSelected, onClick = {
                    selectedCategory = cat
                    hasSearched = false  // 点击分类退出搜索模式
                },
                    text = {
                        Text(cat,
                            style = if (isSelected) MaterialTheme.typography.titleMedium
                                    else MaterialTheme.typography.bodyMedium,
                            color = if (isSelected) Primary else Neutral600)
                    })
            }
        }

        // ===== 内容区 =====
        Box(modifier = Modifier.weight(1f)) {
            if (selectedProduct != null) {
                // 挂画式价格跟踪面板
                CompareTrackingSheet(
                    productId = selectedProduct!!,
                    onClose = { selectedProduct = null },
                )
            } else {
                CompareProductGrid(
                    products = displayProducts,
                    onProductTap = { selectedProduct = it },
                )
            }
        }

        // ===== 底部搜索输入栏 (1:1 复刻主页) =====
        CompareSearchBar(
            query = searchQuery,
            onQueryChange = { searchQuery = it },
            onSend = {
                doSearch(searchQuery)
                searchQuery = ""
            },
            placeholder = "搜索商品或粘贴链接…",
        )

        // AI 对比结果弹窗
        if (showCompareDialog && aiCompareResult != null) {
            AiCompareDialog(result = aiCompareResult!!, onDismiss = { showCompareDialog = false })
        }
    }
}

// ===== 1:1 复刻主页 ChatInputBar 外观的本地搜索栏 =====
@Composable
private fun CompareSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onSend: () -> Unit,
    placeholder: String,
) {
    Row(
        Modifier
            .padding(horizontal = Dimens.space3, vertical = 6.dp)
            .padding(bottom = 4.dp)
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
            // 拍照搜物图标
            IconButton(onClick = {}, modifier = Modifier.size(44.dp)) {
                Icon(Icons.Default.CameraAlt, "拍照搜物", tint = Neutral500)
            }
            // 输入框
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                placeholder = { Text(placeholder, color = Neutral400) },
                textStyle = MaterialTheme.typography.bodyLarge,
                shape = RadiusFull,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Neutral100,
                    unfocusedContainerColor = Neutral100,
                    focusedBorderColor = Primary,
                    unfocusedBorderColor = Neutral100
                ),
                modifier = Modifier.weight(1f),
                maxLines = 4
            )
            // 语音输入
            val context = LocalContext.current
            val hasSpeech = remember { android.speech.SpeechRecognizer.isRecognitionAvailable(context) }
            val voiceLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.StartActivityForResult()
            ) { result ->
                if (result.resultCode == android.app.Activity.RESULT_OK) {
                    val spoken = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull() ?: return@rememberLauncherForActivityResult
                    onQueryChange(spoken)
                }
            }
            IconButton(
                onClick = {
                    if (hasSpeech) {
                        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                            putExtra(RecognizerIntent.EXTRA_PROMPT, "说出想对比的商品...")
                        }
                        voiceLauncher.launch(intent)
                    }
                },
                enabled = hasSpeech,
                modifier = Modifier.size(44.dp)
            ) {
                Icon(Icons.Default.Mic, "语音输入", tint = if (hasSpeech) Neutral500 else Neutral300)
            }
            // 发送按钮
            FilledIconButton(
                onClick = onSend,
                enabled = query.isNotBlank(),
                shape = CircleShape,
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = Primary,
                    disabledContainerColor = Neutral200
                ),
                modifier = Modifier.size(48.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, "发送", tint = OnPrimary)
            }
        }
}

// ===== 统一 2 列商品网格 =====
@Composable
private fun CompareProductGrid(
    products: List<com.shopping.agent.data.model.Product>,
    onProductTap: (String) -> Unit,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(products, key = { it.productId }) { product ->
            Card(onClick = { onProductTap(product.productId) },
                modifier = Modifier.fillMaxWidth(), shape = RadiusLg,
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                colors = CardDefaults.cardColors(containerColor = Neutral0)) {
                Column {
                    AsyncImage(model = product.imageUrl,
                        contentDescription = product.title,
                        modifier = Modifier.fillMaxWidth().aspectRatio(1f),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop)
                    Column(Modifier.padding(12.dp)) {
                        Text(product.title, style = MaterialTheme.typography.titleMedium,
                            color = Neutral900, maxLines = 2)
                        Spacer(Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text("¥${"%.0f".format(product.price)}", style = PriceMedium,
                                color = TextPrice, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.width(8.dp))
                            if (product.ratingCount > 0) {
                                Text(formatSalesCount(product.ratingCount),
                                    style = MaterialTheme.typography.bodySmall, color = Neutral500)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ===== 挂画式价格跟踪面板 =====
@Composable
private fun CompareTrackingSheet(
    productId: String,
    onClose: () -> Unit,
) {
    var dragOffset by remember { mutableStateOf(0f) }
    val dismissThreshold = 150f

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragEnd = {
                        if (dragOffset > dismissThreshold) onClose()
                        dragOffset = 0f
                    },
                    onDragCancel = { dragOffset = 0f },
                    onVerticalDrag = { _, amount ->
                        dragOffset = (dragOffset + amount).coerceAtLeast(0f)
                    }
                )
            },
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        color = Neutral0,
        shadowElevation = 8.dp,
    ) {
        Column {
            // 拖拽把手 + 上方可点击区域
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onClose() }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Surface(
                    modifier = Modifier.width(48.dp).height(5.dp),
                    shape = RoundedCornerShape(3.dp),
                    color = Neutral300,
                ) {}
            }
            // 价格趋势卡片列表
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(MockCompareData.platformTrends) { pt ->
                    PriceTrendCard(
                        platform = pt.platform,
                        lowestPrice = pt.lowestPrice,
                        trend = pt.trend,
                    )
                }
            }
        }

    }
}

@Composable
private fun PriceTrendCard(
    platform: String,
    lowestPrice: Double,
    trend: List<Float>,
) {
    Card(shape = RadiusLg, colors = CardDefaults.cardColors(containerColor = Neutral0),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(Modifier.size(60.dp), shape = RadiusMd, color = Neutral100) {}
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(platform, style = MaterialTheme.typography.titleMedium, color = Neutral900)
                Spacer(Modifier.height(6.dp))
                PriceTrendChart(trend = trend, modifier = Modifier.fillMaxWidth().height(60.dp))
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text("最低价来源: $platform", style = MaterialTheme.typography.bodySmall, color = Neutral500)
                    Text("最低价 ¥$lowestPrice", style = MaterialTheme.typography.bodySmall,
                        color = TextPrice, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun PriceTrendChart(
    trend: List<Float>,
    modifier: Modifier = Modifier,
) {
    if (trend.size < 2) return
    val lineColor = TextPrice
    val fillColor = BrandPink.copy(alpha = 0.12f)
    val dotColor = TextPrice
    val axisLabels = listOf("30天前", "15天前", "今天")

    Column(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxWidth().weight(1f)) {
            val w = size.width; val h = size.height
            val maxV = trend.max(); val minV = trend.min()
            val range = (maxV - minV).coerceAtLeast(1f)
            val padLeft = 4f; val padRight = 4f; val padTop = 8f; val padBottom = 4f
            val chartW = w - padLeft - padRight; val chartH = h - padTop - padBottom
            val pts = trend.mapIndexed { i, v ->
                Offset(padLeft + (i.toFloat() / (trend.size - 1)) * chartW,
                       padTop + (1f - (v - minV) / range) * chartH)
            }
            val fillPath = Path().apply {
                moveTo(pts.first().x, h - padBottom)
                pts.forEach { lineTo(it.x, it.y) }
                lineTo(pts.last().x, h - padBottom); close()
            }
            drawPath(fillPath, fillColor)
            val linePath = Path().apply {
                moveTo(pts.first().x, pts.first().y)
                for (i in 1 until pts.size) lineTo(pts[i].x, pts[i].y)
            }
            drawPath(linePath, lineColor, style = Stroke(width = 2.5f * density))
            pts.forEachIndexed { i, pt ->
                if (i == pts.size - 1) {
                    drawCircle(color = Neutral0, radius = 5f * density, center = pt)
                    drawCircle(color = dotColor, radius = 5f * density, center = pt,
                        style = Stroke(width = 2.5f * density))
                } else drawCircle(color = lineColor, radius = 3f * density, center = pt)
            }
        }
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween) {
            axisLabels.forEach { label ->
                Text(label, style = MaterialTheme.typography.labelSmall, color = Neutral400)
            }
        }
    }
}

// AI 对比结果弹窗
@Composable
private fun AiCompareDialog(result: com.shopping.agent.data.repository.CompareResult, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("AI 智能对比", style = MaterialTheme.typography.titleMedium) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                result.dimensions.forEach { dim ->
                    Card(colors = CardDefaults.cardColors(containerColor = Neutral50)) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(dim.name, style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold, color = Neutral900)
                                if (dim.winner != null) {
                                    Spacer(Modifier.width(8.dp))
                                    Surface(shape = RoundedCornerShape(4.dp), color = PrimaryLight) {
                                        Text("最佳: ${dim.winner}", modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            style = MaterialTheme.typography.labelSmall, color = Primary)
                                    }
                                }
                            }
                            Spacer(Modifier.height(4.dp))
                            dim.values.forEach { (product, value) ->
                                Text("$product: $value", style = MaterialTheme.typography.bodySmall, color = Neutral600)
                            }
                        }
                    }
                }
                if (result.summary.isNotEmpty()) {
                    Card(colors = CardDefaults.cardColors(containerColor = PrimaryLight)) {
                        Text(result.summary, modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodyMedium, color = Primary)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("关闭") } }
    )
}

private fun formatSalesCount(count: Int): String = when {
    count >= 10000 -> "${count / 10000}.${(count % 10000) / 1000}万人付款"
    else -> "${count}人付款"
}
