package com.shopping.agent.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.shopping.agent.core.network.NetworkConfig
import com.shopping.agent.data.local.UserRepository
import com.shopping.agent.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/* ═══════════════════════════════════════════════════════════ */
/*         评分显示文本映射                                       */
/* ═══════════════════════════════════════════════════════════ */

/** 评分星级对应的文本描述 */
private val RATING_TEXTS = mapOf(
    0 to "点击星星评分",
    1 to "非常差",
    2 to "差",
    3 to "一般",
    4 to "好",
    5 to "非常好",
)

/** 文本输入最大字符数 */
private const val MAX_REVIEW_CHARS = 500

/** 最多添加的图片/视频数量 */
private const val MAX_MEDIA_COUNT = 9

/** 每行显示的媒体文件数 */
private const val MEDIA_PER_ROW = 3

/* ═══════════════════════════════════════════════════════════ */
/*         主 Composable                                         */
/* ═══════════════════════════════════════════════════════════ */

/**
 * 评价晒单页面。
 *
 * 通过 orderId 从 SQLite 加载订单数据（商品 ID、名称、图片）。
 *
 * 布局：
 * - 顶部栏：< 返回 + 商品图片&"写评价"（居中）+ 发布按钮（蓝色）
 * - 主体：评分栏（5星 + 文本） → 评价文本框（500字限制） → 图片/视频添加 → 匿名评价
 *
 * @param orderId 订单 ID（用于从数据库查询订单信息）
 * @param onBack 返回回调
 * @param onPublishSuccess 评价发布成功回调
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewScreen(
    orderId: Long = 0,
    onBack: () -> Unit = {},
    onPublishSuccess: () -> Unit = {},
) {
    val context = LocalContext.current
    val repository = remember { UserRepository(context) }
    val scope = rememberCoroutineScope()

    // 从数据库加载订单数据
    var productId by remember { mutableStateOf("") }
    var productTitle by remember { mutableStateOf("") }
    var productImage by remember { mutableStateOf("") }
    var isLoadingOrder by remember { mutableStateOf(true) }

    LaunchedEffect(orderId) {
        try {
            withContext(Dispatchers.IO) {
                val records = repository.getOrderRecords(statusFilter = null, limit = 100)
                val order = records.find { it.orderId == orderId }
                if (order != null) {
                    val json = org.json.JSONObject(order.orderBody)
                    val itemsArray = json.optJSONArray("items")
                    if (itemsArray != null && itemsArray.length() > 0) {
                        val firstItem = itemsArray.getJSONObject(0)
                        productId = firstItem.optString("product_id", "")
                        productTitle = firstItem.optString("title", "商品")
                        productImage = firstItem.optString("image_url", "")
                    }
                }
            }
        } catch (_: Exception) {
            // 加载失败，使用默认值
        }
        isLoadingOrder = false
    }

    // 评分状态（0 表示未评分，1-5 表示已评分）
    var rating by remember { mutableIntStateOf(5) }
    // 评价文本
    var reviewText by remember { mutableStateOf("") }
    // 选中的媒体文件 URI 列表
    var selectedMediaUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    // 是否匿名评价
    var isAnonymous by remember { mutableStateOf(false) }
    // 是否正在提交
    var isSubmitting by remember { mutableStateOf(false) }
    // 发布按钮是否已尝试提交过（用于显示评分空提示）
    var hasAttemptedSubmit by remember { mutableStateOf(false) }

    // 图片选择器 launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        // 追加新选中的图片，不超过 9 个
        val availableSlots = MAX_MEDIA_COUNT - selectedMediaUris.size
        if (availableSlots > 0) {
            selectedMediaUris = selectedMediaUris + uris.take(availableSlots)
        } else {
            Toast.makeText(context, "最多添加${MAX_MEDIA_COUNT}个图片/视频", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            // 顶部栏
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 4.dp, vertical = 8.dp)
                    .height(56.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // 返回按钮（靠左）
                IconButton(onClick = onBack, modifier = Modifier.size(40.dp)) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "返回",
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }

                // 商品图片 + "写评价"（居中）
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    AsyncImage(
                        model = NetworkConfig.resolveImageUrl(productImage),
                        contentDescription = productTitle,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(6.dp)),
                        contentScale = ContentScale.Crop,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "写评价",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }

                // 发布按钮（蓝色背景）
                Button(
                    onClick = {
                        hasAttemptedSubmit = true
                        if (rating == 0) {
                            Toast.makeText(context, "您还没有进行评分哦~", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        // 提交评价
                        isSubmitting = true
                        scope.launch {
                            try {
                                val nickname = withContext(Dispatchers.IO) {
                                    val profile = repository.getUserProfile()
                                    profile["nickname"]?.takeIf { it.isNotEmpty() } ?: "用户"
                                }
                                val finalContent = if (reviewText.isBlank()) {
                                    "用户未编写评价信息"
                                } else {
                                    reviewText.trim()
                                }
                                val success = withContext(Dispatchers.IO) {
                                    repository.submitReviewToBackend(
                                        productId = productId,
                                        nickname = nickname,
                                        rating = rating,
                                        content = finalContent,
                                        isAnonymous = isAnonymous,
                                    )
                                }
                                if (success) {
                                    // 评价成功后，将订单状态改为已完成
                                    if (orderId > 0) {
                                        withContext(Dispatchers.IO) {
                                            repository.updateOrderStatus(
                                                orderId,
                                                UserRepository.OrderStatus.COMPLETED,
                                            )
                                        }
                                    }
                                    Toast.makeText(context, "评价成功", Toast.LENGTH_SHORT).show()
                                    onPublishSuccess()
                                } else {
                                    Toast.makeText(context, "评价提交失败，请稍后重试", Toast.LENGTH_SHORT).show()
                                }
                            } catch (_: Exception) {
                                Toast.makeText(context, "评价提交失败", Toast.LENGTH_SHORT).show()
                            }
                            isSubmitting = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Primary,
                        disabledContainerColor = Primary.copy(alpha = 0.5f),
                    ),
                    shape = RoundedCornerShape(18.dp),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 6.dp),
                    enabled = !isSubmitting,
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(
                            color = OnPrimary,
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Text("发布", color = OnPrimary, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 16.dp, vertical = 16.dp),
        ) {
            // ═════════ 评分栏 ═════════
            RatingSection(
                rating = rating,
                onRatingChange = { rating = it },
            )

            Spacer(Modifier.height(20.dp))

            // ═════════ 评价文本框 ═════════
            ReviewTextSection(
                text = reviewText,
                onTextChange = { if (it.length <= MAX_REVIEW_CHARS) reviewText = it },
            )

            Spacer(Modifier.height(20.dp))

            // ═════════ 图片/视频添加 ═════════
            MediaPickerSection(
                mediaUris = selectedMediaUris,
                onAddClick = {
                    imagePickerLauncher.launch("image/*")
                },
                onRemoveMedia = { index ->
                    selectedMediaUris = selectedMediaUris.toMutableList().also { it.removeAt(index) }
                },
            )

            Spacer(Modifier.height(20.dp))

            // ═════════ 匿名评价 ═════════
            AnonymousToggle(
                isAnonymous = isAnonymous,
                onToggle = { isAnonymous = !isAnonymous },
            )

            Spacer(Modifier.height(40.dp))
        }
    }
}

/* ═══════════════════════════════════════════════════════════ */
/*         评分栏组件                                              */
/* ═══════════════════════════════════════════════════════════ */

/**
 * 评分栏：显示"商品评价"文本 + 评价状态 + 五颗可点击星星。
 *
 * 星星交互规则：
 * - 点击某颗星时，若该星未被点亮，则点亮该星及其左侧所有星
 * - 若该星已被点亮且是最右侧点亮的星，则取消该星及其右侧所有星
 * - 初始默认全点亮（5分）
 */
@Composable
private fun RatingSection(
    rating: Int,
    onRatingChange: (Int) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // 标题行
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "商品评价",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.width(12.dp))
            // 评价状态文本
            Text(
                RATING_TEXTS[rating] ?: "",
                style = MaterialTheme.typography.bodyMedium,
                color = if (rating > 0) Primary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(Modifier.height(12.dp))

        // 五颗星
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            for (starIndex in 1..5) {
                val isFilled = starIndex <= rating
                Icon(
                    Icons.Default.Star,
                    contentDescription = "${starIndex}星",
                    modifier = Modifier
                        .size(40.dp)
                        .clickable {
                            // 点击当前星级：若已是最右侧点亮星则降级，否则设为此星级
                            if (rating == starIndex) {
                                // 再次点击相同星星：取消该星及其右侧（即降为 starIndex - 1）
                                onRatingChange(starIndex - 1)
                            } else {
                                // 设为该星级（点亮该星及左侧）
                                onRatingChange(starIndex)
                            }
                        },
                    tint = if (isFilled) StarYellow else Neutral300,
                )
            }
        }
    }
}

/** 星星黄色 */
private val StarYellow = androidx.compose.ui.graphics.Color(0xFFFFB800)
/** 中性灰色（未点亮星星） */
private val Neutral300 = androidx.compose.ui.graphics.Color(0xFFE0E0E0)

/* ═══════════════════════════════════════════════════════════ */
/*         评价文本框组件                                          */
/* ═══════════════════════════════════════════════════════════ */

/**
 * 评价文本输入区：大文本框（占半屏）+ 字符计数显示。
 */
@Composable
private fun ReviewTextSection(
    text: String,
    onTextChange: (String) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // 文本框（固定高度约半屏）
        OutlinedTextField(
            value = text,
            onValueChange = onTextChange,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 200.dp, max = 280.dp),
            placeholder = {
                Text(
                    "这次购物感觉怎么样？跟大家分享一下吧~",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                )
            },
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
            ),
        )

        Spacer(Modifier.height(8.dp))

        // 字符计数（右下角）
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            Text(
                "已写${text.length}个字",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/* ═══════════════════════════════════════════════════════════ */
/*         图片/视频添加组件                                        */
/* ═══════════════════════════════════════════════════════════ */

/**
 * 图片/视频添加区：最多 9 个，每行 3 个。
 * 已选媒体在前，添加按钮（+号 + "视频/图片"）在最后。
 */
@Composable
private fun MediaPickerSection(
    mediaUris: List<Uri>,
    onAddClick: () -> Unit,
    onRemoveMedia: (Int) -> Unit,
) {
    // 总格数：已选媒体 + （未满 9 个时）1 个添加按钮
    val hasAddSlot = mediaUris.size < MAX_MEDIA_COUNT
    val totalSlots = mediaUris.size + (if (hasAddSlot) 1 else 0)
    val rows = (totalSlots + MEDIA_PER_ROW - 1) / MEDIA_PER_ROW

    Column {
        for (row in 0 until rows) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                for (col in 0 until MEDIA_PER_ROW) {
                    val index = row * MEDIA_PER_ROW + col
                    when {
                        // 已选媒体在前
                        index < mediaUris.size -> {
                            MediaPreviewItem(
                                uri = mediaUris[index],
                                modifier = Modifier.weight(1f),
                                onRemove = { onRemoveMedia(index) },
                            )
                        }
                        // 添加按钮在最后
                        hasAddSlot && index == mediaUris.size -> {
                            AddMediaButton(
                                modifier = Modifier.weight(1f),
                                onClick = onAddClick,
                            )
                        }
                        // 空白占位
                        else -> {
                            Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
            if (row < rows - 1) {
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

/** 添加媒体按钮：正方形框 + 中间 + 号 + 下方 "视频/图片" */
@Composable
private fun AddMediaButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.Add,
                contentDescription = "添加图片",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(28.dp),
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "视频/图片",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** 已选媒体预览：显示缩略图 + 右上角删除按钮 */
@Composable
private fun MediaPreviewItem(
    uri: Uri,
    modifier: Modifier = Modifier,
    onRemove: () -> Unit,
) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp)),
    ) {
        AsyncImage(
            model = uri,
            contentDescription = "已选图片",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )

        // 右上角删除按钮
        IconButton(
            onClick = onRemove,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(24.dp)
                .background(
                    color = androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.5f),
                    shape = CircleShape,
                ),
        ) {
            Icon(
                Icons.Default.Close,
                contentDescription = "移除",
                tint = androidx.compose.ui.graphics.Color.White,
                modifier = Modifier.size(14.dp),
            )
        }
    }
}

/* ═══════════════════════════════════════════════════════════ */
/*         匿名评价开关                                            */
/* ═══════════════════════════════════════════════════════════ */

/**
 * 匿名评价选项：蓝色勾选按钮 + "匿名评价"文本。
 */
@Composable
private fun AnonymousToggle(
    isAnonymous: Boolean,
    onToggle: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // 勾选框
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(if (isAnonymous) Primary else MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            if (isAnonymous) {
                Text("✓", color = OnPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(Modifier.width(10.dp))

        Text(
            "匿名评价",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}
