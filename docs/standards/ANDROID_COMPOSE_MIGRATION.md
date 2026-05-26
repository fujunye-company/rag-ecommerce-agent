# Android Compose 迁移开发方案 v1.0

> 项目: RAG 多模态电商智能导购 Agent
> 目标: Web DESIGN.md → Android Kotlin Compose 原生
> 基准: DESIGN.md v2.0 + 原型图分析 + REQS-竞赛核心需求
> 备份: `.backup_20260525_144414/`

---

## 一、设计系统映射

### 1.1 Color Token → Compose

```kotlin
// ui/theme/Color.kt
package com.shopping.agent.ui.theme

import androidx.compose.ui.graphics.Color

// ===== 主色系 — 珊瑚红 =====
val Primary          = Color(0xFFFF5C5C)
val PrimaryLight     = Color(0xFFFFF0F0)
val PrimaryDark      = Color(0xFFE04848)
val PrimaryGradientStart = Color(0xFFFF5C5C)
val PrimaryGradientEnd   = Color(0xFFFF8E8E)

// ===== 辅助色 =====
val Success          = Color(0xFF2ECC71)
val SuccessLight     = Color(0xFFE8F8EF)
val Warning          = Color(0xFFF39C12)
val WarningLight     = Color(0xFFFFF8E8)
val Info             = Color(0xFF4A90D9)
val InfoLight        = Color(0xFFEBF3FC)
val Error            = Color(0xFFE74C3C)
val ErrorLight       = Color(0xFFFDEDEC)

// ===== 中性色 =====
val Neutral0         = Color(0xFFFFFFFF)   // 卡片白
val Neutral50        = Color(0xFFF8F9FA)   // 页面底
val Neutral100       = Color(0xFFF0F0F0)   // 输入框背景
val Neutral200       = Color(0xFFE0E0E0)   // 边框/骨架屏
val Neutral300       = Color(0xFFCCCCCC)
val Neutral400       = Color(0xFFAAAAAA)   // 占位文字
val Neutral500       = Color(0xFF888888)   // 辅助文字
val Neutral600       = Color(0xFF666666)
val Neutral700       = Color(0xFF444444)   // 正文
val Neutral800       = Color(0xFF222222)   // 标题
val Neutral900       = Color(0xFF111111)   // 强强调

// ===== 语义色 =====
val TextPrice        = Primary           // 价格固定红色
val TextOnPrimary    = Color.White
val SourceTagColor   = Info              // 来源蓝
val RatingStar       = Color(0xFFFFB800) // 评分金
val SkeletonColor    = Neutral200
val OverlayColor     = Color(0x66000000)
```

### 1.2 Typography Token → MaterialTheme

```kotlin
// ui/theme/Type.kt
import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// 等宽数字字体 (价格专用)
val NumberFontFamily = FontFamily.Monospace

val ShoppingTypography = Typography(
    displayLarge = TextStyle(
        fontSize = 28.sp,
        fontWeight = FontWeight.Bold,
        lineHeight = 34.sp,
    ),
    headlineMedium = TextStyle(
        fontSize = 22.sp,
        fontWeight = FontWeight.SemiBold,
        lineHeight = 28.sp,
    ),
    titleMedium = TextStyle(
        fontSize = 18.sp,
        fontWeight = FontWeight.Medium,
        lineHeight = 24.sp,
    ),
    bodyLarge = TextStyle(
        fontSize = 16.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 24.sp,
    ),
    bodyMedium = TextStyle(
        fontSize = 14.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 20.sp,
    ),
    labelSmall = TextStyle(
        fontSize = 12.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 16.sp,
    ),
)

// 价格专用 TextStyle
val PriceLarge = TextStyle(
    fontSize = 24.sp,
    fontWeight = FontWeight.Bold,
    lineHeight = 28.sp,
    fontFamily = NumberFontFamily,
    color = TextPrice,
)
val PriceMedium = TextStyle(
    fontSize = 20.sp,
    fontWeight = FontWeight.Bold,
    lineHeight = 24.sp,
    fontFamily = NumberFontFamily,
    color = TextPrice,
)
val PriceSmall = TextStyle(
    fontSize = 16.sp,
    fontWeight = FontWeight.Bold,
    lineHeight = 20.sp,
    fontFamily = NumberFontFamily,
    color = TextPrice,
)
```

### 1.3 Spacing Token → Dimens

```kotlin
// ui/theme/Dimens.kt
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

object Dimens {
    // 4pt 栅格
    val space0  = 0.dp
    val space1  = 4.dp
    val space2  = 8.dp
    val space3  = 12.dp
    val space4  = 16.dp
    val space5  = 20.dp
    val space6  = 24.dp
    val space8  = 32.dp
    val space10 = 40.dp
    val space12 = 48.dp

    // 语义间距
    val pageHorizontal  = space4
    val cardPadding     = space4
    val cardGap         = space3
    val sectionGap      = space6
    val listItemGap     = space2
    val chipGap         = space2

    // 触控
    val touchTargetMin  = 44.dp

    // 组件尺寸
    val inputBarHeight  = 48.dp
    val inputBarMaxHeight = 104.dp
    val searchBarHeight = 44.dp
    val bottomNavHeight = 56.dp
    val categoryTabHeight = 36.dp
    val chipHeight      = 32.dp
    val sourceTagHeight = 22.dp

    // 卡片
    val productCardImageSize = 100.dp  // 对话嵌入
    val productGridImageRatio = 1f     // 1:1
}
```

### 1.4 Radius Token → RoundedCornerShape

```kotlin
// ui/theme/Shape.kt
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val RadiusNone  = RoundedCornerShape(0.dp)
val RadiusSm    = RoundedCornerShape(4.dp)
val RadiusMd    = RoundedCornerShape(8.dp)
val RadiusLg    = RoundedCornerShape(12.dp)
val RadiusXl    = RoundedCornerShape(16.dp)
val RadiusFull  = RoundedCornerShape(50)

// 聊天气泡专用形状
val UserBubbleShape  = RoundedCornerShape(16.dp, 16.dp, 4.dp, 16.dp)
val AgentBubbleShape = RoundedCornerShape(4.dp, 16.dp, 16.dp, 16.dp)

val ShoppingShapes = Shapes(
    small  = RadiusSm,
    medium = RadiusMd,
    large  = RadiusLg,
    extraLarge = RadiusXl,
)
```

### 1.5 Shadow → Card Elevation

```kotlin
// Elevation mapping
// --shadow-card           → CardDefaults.elevation(2.dp)
// --shadow-card-raised    → CardDefaults.elevation(4.dp)
// --shadow-overlay        → CardDefaults.elevation(8.dp)
// --shadow-bottom-nav     → BottomAppBar elevation = 3.dp
// --shadow-button         → Button elevation = 2.dp

// 在组件中:
// Card(elevation = CardDefaults.cardElevation(defaultElevation = 2.dp))
```

### 1.6 Theme 组装

```kotlin
// ui/theme/Theme.kt
import androidx.compose.material3.*
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary            = Primary,
    onPrimary          = TextOnPrimary,
    primaryContainer   = PrimaryLight,
    secondary          = Info,
    onSecondary        = TextOnPrimary,
    background         = Neutral50,
    onBackground       = Neutral900,
    surface            = Neutral0,
    onSurface          = Neutral900,
    onSurfaceVariant   = Neutral500,
    error              = Error,
    onError            = TextOnPrimary,
    outline            = Neutral200,
    outlineVariant     = Neutral100,
)

@Composable
fun ShoppingAgentTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography  = ShoppingTypography,
        shapes      = ShoppingShapes,
        content     = content,
    )
}
```

---

## 二、Compose 目录结构

```
apps/android/app/src/main/java/com/shopping/agent/
│
├── MainActivity.kt                    # Activity入口 + NavHost挂载
├── ShoppingApp.kt                     # Application (Hilt初始化)
│
├── ui/
│   ├── theme/
│   │   ├── Color.kt                   # 色彩 Token
│   │   ├── Type.kt                    # 字体 Token + 价格 Style
│   │   ├── Shape.kt                   # 圆角 + 气泡形状
│   │   ├── Dimens.kt                  # 间距 Token
│   │   └── Theme.kt                   # MaterialTheme 组装
│   │
│   ├── navigation/
│   │   ├── NavGraph.kt                # 路由图 (NavHost + composable)
│   │   ├── Screen.kt                  # 路由枚举 sealed class
│   │   └── NavAnimations.kt           # 转场动画 (fadeIn/slideUp)
│   │
│   ├── components/
│   │   ├── common/
│   │   │   ├── TopNavBar.kt           # 顶部导航 (菜单+电话+静音)
│   │   │   ├── BottomNavBar.kt        # 4Tab底部导航
│   │   │   ├── SearchBar.kt           # 统一搜索栏
│   │   │   ├── GradientBackground.kt  # 渐变背景容器
│   │   │   ├── CategoryTabs.kt        # 分类标签横向滚动
│   │   │   ├── LoadingSkeleton.kt     # 骨架屏 (商品/对话/列表)
│   │   │   ├── EmptyState.kt          # 空状态 (无结果/无历史)
│   │   │   └── ErrorState.kt          # 错误状态 (网络/发送失败)
│   │   │
│   │   ├── product/
│   │   │   ├── ProductCard.kt         # 标准商品卡片 (Grid)
│   │   │   ├── ProductCardHorizontal.kt # 横向商品卡片 (对话嵌入)
│   │   │   ├── ProductGrid.kt         # 商品网格 (LazyVerticalGrid)
│   │   │   ├── ProductDetailCard.kt   # 商品详情卡片
│   │   │   ├── PriceTrendChart.kt     # Canvas 价格走势折线图
│   │   │   ├── ColorVariantPicker.kt  # 颜色/规格选择器
│   │   │   ├── ProductSourceBadge.kt  # 来源标签
│   │   │   ├── RecommendationReasonTag.kt # 推荐理由标签
│   │   │   └── PriceFloatingPanel.kt  # 价格浮动面板
│   │   │
│   │   └── chat/
│   │       ├── MessageBubble.kt       # 聊天气泡 (user/agent)
│   │       ├── StreamingMessageBubble.kt # 流式气泡 (逐字+光标)
│   │       ├── ChatInputBar.kt        # 输入栏 (文本+相机+语音+发送)
│   │       ├── MultimodalSearchBar.kt # 多模态搜索栏 (拍照搜)
│   │       ├── ImageSearchButton.kt   # 拍照搜索按钮
│   │       ├── VoiceInputButton.kt    # 语音输入按钮
│   │       ├── QuickActionChips.kt    # 快捷功能Chip行
│   │       ├── ClarifyChip.kt         # 追问Chip
│   │       ├── HistoryDrawer.kt       # 历史对话侧栏
│   │       ├── HistoryItem.kt         # 历史对话条目
│   │       └── FeedbackWidget.kt      # 反馈组件 (有帮助/无帮助)
│   │
│   └── screens/
│       ├── guide/
│       │   └── ChatGuideScreen.kt     # ★ 对话导购页 (核心)
│       ├── product/
│       │   ├── HomeScreen.kt          # 每日推荐首页
│       │   ├── ExploreScreen.kt       # 探索发现页
│       │   ├── CompareScreen.kt       # 比价列表页
│       │   └── ProductDetailScreen.kt # 商品详情+价格跟踪
│       ├── profile/
│       │   ├── ProfileScreen.kt       # 个人中心
│       │   └── SettingsScreen.kt      # 设置页
│       └── history/
│           └── HistoryScreen.kt       # 历史对话全屏
│
├── data/
│   ├── model/
│   │   ├── Product.kt                 # 商品数据类
│   │   ├── ChatMessage.kt             # 消息数据类
│   │   ├── SSEEvent.kt                # SSE事件密封类
│   │   ├── User.kt                    # 用户模型
│   │   ├── Conversation.kt            # 对话历史
│   │   ├── Coupon.kt                  # 优惠券
│   │   └── PriceHistory.kt            # 价格历史
│   │
│   ├── remote/
│   │   ├── ApiClient.kt               # OkHttp配置 + Base URL
│   │   ├── ApiService.kt              # Retrofit接口定义
│   │   └── SseClient.kt               # OkHttp SSE流读取
│   │
│   ├── repository/
│   │   ├── ChatRepository.kt          # 对话仓库
│   │   ├── ProductRepository.kt       # 商品仓库
│   │   ├── FeedbackRepository.kt      # 反馈仓库
│   │   └── UserRepository.kt          # 用户仓库
│   │
│   └── mock/
│       ├── MockProducts.kt            # Mock商品 (10条3品类)
│       ├── MockChats.kt               # Mock对话
│       ├── MockUser.kt                # Mock用户
│       └── MockPriceHistory.kt        # Mock价格走势
│
└── viewmodel/
    ├── ChatViewModel.kt               # ★ 核心: SSE流式+消息+状态
    ├── ProductViewModel.kt            # 商品列表/详情
    ├── ExploreViewModel.kt            # 探索页
    ├── CompareViewModel.kt            # 比价页
    ├── ProfileViewModel.kt            # 个人中心
    └── SettingsViewModel.kt           # 设置
```

---

## 三、核心组件 Compose 实现

### 3.1 MultimodalSearchBar

```kotlin
// ui/components/chat/MultimodalSearchBar.kt
@Composable
fun MultimodalSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onImageSearch: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .height(Dimens.searchBarHeight)
            .clip(RadiusFull)
            .background(Neutral100)
            .padding(horizontal = Dimens.space3),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Default.CameraAlt,
            contentDescription = "拍照搜索",
            tint = Neutral500,
            modifier = Modifier
                .size(Dimens.touchTargetMin)
                .clickable { onImageSearch() }
                .padding(Dimens.space2),
        )
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            textStyle = MaterialTheme.typography.bodyLarge,
            cursorBrush = SolidColor(Primary),
            decorationBox = { innerTextField ->
                if (query.isEmpty()) {
                    Text(
                        "搜索想探索的好物",
                        color = Neutral400,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
                innerTextField()
            },
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(Dimens.space2))
        Button(
            onClick = onSearch,
            enabled = query.isNotBlank(),
            colors = ButtonDefaults.buttonColors(
                containerColor = Primary,
                disabledContainerColor = Neutral200,
            ),
            shape = RadiusFull,
            contentPadding = PaddingValues(horizontal = Dimens.space4),
        ) {
            Text("搜索", color = TextOnPrimary)
        }
    }
}
```

### 3.2 ChatInputBar

```kotlin
// ui/components/chat/ChatInputBar.kt
@Composable
fun ChatInputBar(
    text: String,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    onCameraClick: () -> Unit,
    onVoiceStart: () -> Unit,
    onVoiceEnd: () -> Unit,
    isRecording: Boolean,
    selectedImageUri: Uri?,
    onClearImage: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shadowElevation = 3.dp,
        color = Neutral0,
    ) {
        Column(modifier = Modifier.padding(Dimens.space3)) {
            // 图片预览条
            if (selectedImageUri != null) {
                Row(
                    modifier = Modifier.padding(bottom = Dimens.space2),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AsyncImage(
                        model = selectedImageUri,
                        contentDescription = "已选图片",
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RadiusSm),
                        contentScale = ContentScale.Crop,
                    )
                    Spacer(Modifier.width(Dimens.space2))
                    IconButton(
                        onClick = onClearImage,
                        modifier = Modifier.size(Dimens.touchTargetMin),
                    ) {
                        Icon(Icons.Default.Close, "清除图片", tint = Neutral500)
                    }
                }
            }
            // 输入行
            Row(verticalAlignment = Alignment.CenterVertically) {
                // 相机按钮
                IconButton(
                    onClick = onCameraClick,
                    modifier = Modifier.size(Dimens.touchTargetMin),
                ) {
                    Icon(
                        Icons.Default.CameraAlt,
                        "拍照",
                        tint = if (selectedImageUri != null) Primary else Neutral500,
                    )
                }
                // 输入框
                BasicTextField(
                    value = text,
                    onValueChange = onTextChange,
                    textStyle = MaterialTheme.typography.bodyLarge,
                    cursorBrush = SolidColor(Primary),
                    decorationBox = { innerTextField ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = Dimens.inputBarHeight, max = Dimens.inputBarMaxHeight)
                                .clip(RadiusFull)
                                .background(Neutral100)
                                .padding(horizontal = Dimens.space4, vertical = Dimens.space3),
                        ) {
                            if (text.isEmpty()) {
                                Text(
                                    "点击发送或长按说话…",
                                    color = Neutral400,
                                    style = MaterialTheme.typography.bodyLarge,
                                )
                            }
                            innerTextField()
                        }
                    },
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(Dimens.space2))
                // 语音按钮
                VoiceInputButton(
                    isRecording = isRecording,
                    onStart = onVoiceStart,
                    onEnd = onVoiceEnd,
                )
                Spacer(Modifier.width(Dimens.space2))
                // 发送按钮
                IconButton(
                    onClick = onSend,
                    enabled = text.isNotBlank(),
                    modifier = Modifier.size(Dimens.touchTargetMin),
                ) {
                    Icon(
                        Icons.Default.Send,
                        "发送",
                        tint = if (text.isNotBlank()) Primary else Neutral300,
                    )
                }
            }
        }
    }
}
```

### 3.3 StreamingMessageBubble

```kotlin
// ui/components/chat/StreamingMessageBubble.kt
@Composable
fun StreamingMessageBubble(
    text: String,
    isActive: Boolean,
    productCards: List<Product> = emptyList(),
    onProductTap: (Product) -> Unit,
    modifier: Modifier = Modifier,
) {
    var cursorVisible by remember { mutableStateOf(true) }

    // 光标闪烁动画
    LaunchedEffect(isActive) {
        while (isActive) {
            delay(500)
            cursorVisible = !cursorVisible
        }
    }

    Column(modifier = modifier.fillMaxWidth().padding(horizontal = Dimens.pageHorizontal)) {
        // 搜索状态提示
        if (isActive && text.length < 20) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = Dimens.space1),
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(12.dp),
                    strokeWidth = 1.5.dp,
                    color = Primary,
                )
                Spacer(Modifier.width(Dimens.space2))
                Text(
                    "正在搜索中…",
                    style = MaterialTheme.typography.labelSmall,
                    color = Neutral500,
                )
            }
        }

        // AI 气泡
        Surface(
            shape = AgentBubbleShape,
            color = Neutral0,
            shadowElevation = 2.dp,
            modifier = Modifier.widthIn(max = 320.dp),
        ) {
            Column(modifier = Modifier.padding(Dimens.space3)) {
                // 流式文本
                Text(
                    text = buildAnnotatedString {
                        append(text)
                        if (isActive && cursorVisible) {
                            withStyle(SpanStyle(color = Primary, fontWeight = FontWeight.Bold)) {
                                append("▌")
                            }
                        }
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    color = Neutral900,
                )

                // 商品卡片 (渐入动画)
                if (productCards.isNotEmpty()) {
                    Spacer(Modifier.height(Dimens.space2))
                    productCards.forEachIndexed { index, product ->
                        AnimatedVisibility(
                            visible = true,
                            enter = fadeIn(animationSpec = tween(300, delayMillis = index * 100))
                                    + slideInVertically(initialOffsetY = { it / 2 }),
                        ) {
                            ProductCardHorizontal(
                                product = product,
                                onTap = { onProductTap(product) },
                                modifier = Modifier.padding(bottom = Dimens.space2),
                            )
                        }
                    }
                }
            }
        }
    }
}
```

### 3.4 ProductCard

```kotlin
// ui/components/product/ProductCard.kt
@Composable
fun ProductCard(
    product: Product,
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onTap,
        shape = RadiusLg,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = Neutral0),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column {
            // 商品图 1:1
            AsyncImage(
                model = product.imageUrl,
                contentDescription = product.name,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(Dimens.productGridImageRatio)
                    .clip(RadiusMd),
                contentScale = ContentScale.Crop,
            )
            Column(modifier = Modifier.padding(Dimens.cardPadding)) {
                // 商品名 (最多2行)
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = Neutral900,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(Dimens.space1))
                // 价格行
                Row(verticalAlignment = Alignment.Baseline) {
                    Text(
                        text = "¥${product.price}",
                        style = PriceMedium,
                    )
                    if (product.originalPrice != null && product.originalPrice > product.price) {
                        Spacer(Modifier.width(Dimens.space2))
                        Text(
                            text = "¥${product.originalPrice}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                textDecoration = TextDecoration.LineThrough,
                            ),
                            color = Neutral500,
                        )
                    }
                }
                Spacer(Modifier.height(Dimens.space1))
                // 销量
                if (product.salesCount != null) {
                    Text(
                        text = formatSalesCount(product.salesCount),
                        style = MaterialTheme.typography.labelSmall,
                        color = Neutral500,
                    )
                }
                Spacer(Modifier.height(Dimens.space1))
                // 来源标签
                ProductSourceBadge(source = product.source)
            }
        }
    }
}

// 水平版 (对话嵌入)
@Composable
fun ProductCardHorizontal(
    product: Product,
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onTap,
        shape = RadiusLg,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = Neutral0),
        modifier = modifier.fillMaxWidth().height(120.dp),
    ) {
        Row(modifier = Modifier.padding(Dimens.cardPadding)) {
            // 商品图
            AsyncImage(
                model = product.imageUrl,
                contentDescription = product.name,
                modifier = Modifier
                    .size(Dimens.productCardImageSize)
                    .clip(RadiusMd),
                contentScale = ContentScale.Crop,
            )
            Spacer(Modifier.width(Dimens.space3))
            // 信息区
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = Neutral900,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.weight(1f))
                Row(verticalAlignment = Alignment.Baseline) {
                    Text("¥${product.price}", style = PriceSmall)
                    if (product.originalPrice != null && product.originalPrice > product.price) {
                        Spacer(Modifier.width(Dimens.space2))
                        Text(
                            "¥${product.originalPrice}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                textDecoration = TextDecoration.LineThrough,
                            ),
                            color = Neutral500,
                        )
                    }
                }
                Spacer(Modifier.height(Dimens.space1))
                Row(horizontalArrangement = Arrangement.spacedBy(Dimens.space2)) {
                    ProductSourceBadge(source = product.source)
                    if (product.matchReason != null) {
                        RecommendationReasonTag(reason = product.matchReason)
                    }
                }
            }
        }
    }
}
```

### 3.5 其他核心组件 (精简签名)

```kotlin
// --- ProductSourceBadge ---
@Composable
fun ProductSourceBadge(source: String) {
    Surface(
        shape = RadiusSm,
        color = InfoLight,
    ) {
        Text(
            text = source,
            style = MaterialTheme.typography.labelSmall,
            color = Info,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
        )
    }
}

// --- RecommendationReasonTag ---
@Composable
fun RecommendationReasonTag(reason: String) {
    Text(
        text = "✓ $reason",
        style = MaterialTheme.typography.labelSmall,
        color = Success,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

// --- PriceFloatingPanel ---
@Composable
fun PriceFloatingPanel(
    lowestPrice: Double,
    platform: String,
    onBuyClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shadowElevation = 8.dp,
        color = Neutral0,
    ) {
        Row(
            modifier = Modifier.padding(Dimens.cardPadding),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text("最低价", style = MaterialTheme.typography.labelSmall, color = Neutral500)
                Text(
                    "¥$lowestPrice",
                    style = PriceLarge,
                )
                Text("来源: $platform", style = MaterialTheme.typography.labelSmall, color = Neutral500)
            }
            Spacer(Modifier.weight(1f))
            Button(
                onClick = onBuyClick,
                shape = RadiusFull,
                colors = ButtonDefaults.buttonColors(containerColor = Primary),
            ) {
                Text("去购买", color = TextOnPrimary)
            }
        }
    }
}

// --- VoiceInputButton ---
@Composable
fun VoiceInputButton(
    isRecording: Boolean,
    onStart: () -> Unit,
    onEnd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    LaunchedEffect(isPressed) {
        if (isPressed) {
            delay(200) // 长按200ms触发
            onStart()
        }
    }

    IconButton(
        onClick = {}, // 长按触发，非点击
        modifier = modifier
            .size(Dimens.touchTargetMin)
            .pointerInput(Unit) {
                detectTapGestures(
                    onLongPress = { onStart() },
                    onPress = { /* 处理松手 */ },
                )
            },
    ) {
        Icon(
            Icons.Default.Mic,
            "语音输入",
            tint = if (isRecording) Primary else Neutral500,
            modifier = Modifier.scale(if (isRecording) 1.2f else 1f),
        )
    }
}

// --- ImageSearchButton ---
@Composable
fun ImageSearchButton(
    onTakePhoto: () -> Unit,
    onPickFromGallery: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showSheet by remember { mutableStateOf(false) }

    IconButton(
        onClick = { showSheet = true },
        modifier = modifier.size(Dimens.touchTargetMin),
    ) {
        Icon(Icons.Default.CameraAlt, "拍照搜索", tint = Neutral500)
    }

    if (showSheet) {
        ModalBottomSheet(onDismissRequest = { showSheet = false }) {
            Column(modifier = Modifier.padding(Dimens.cardPadding)) {
                ListItem(
                    headlineContent = { Text("拍照") },
                    leadingContent = { Icon(Icons.Default.CameraAlt, null) },
                    modifier = Modifier.clickable { showSheet = false; onTakePhoto() },
                )
                ListItem(
                    headlineContent = { Text("从相册选择") },
                    leadingContent = { Icon(Icons.Default.PhotoLibrary, null) },
                    modifier = Modifier.clickable { showSheet = false; onPickFromGallery() },
                )
            }
        }
    }
}

// --- LoadingSkeleton ---
@Composable
fun ProductCardSkeleton(modifier: Modifier = Modifier) {
    Card(shape = RadiusLg, modifier = modifier.fillMaxWidth()) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .background(SkeletonColor)
                    .shimmerEffect(),
            )
            Column(modifier = Modifier.padding(Dimens.cardPadding)) {
                Box(Modifier.fillMaxWidth(0.8f).height(18.dp).background(SkeletonColor).shimmerEffect())
                Spacer(Modifier.height(Dimens.space1))
                Box(Modifier.fillMaxWidth(0.4f).height(24.dp).background(SkeletonColor).shimmerEffect())
                Spacer(Modifier.height(Dimens.space1))
                Box(Modifier.fillMaxWidth(0.25f).height(16.dp).background(SkeletonColor).shimmerEffect())
            }
        }
    }
}

fun Modifier.shimmerEffect(): Modifier = this
    // 简化版 shimmer: 用 animateFloatAsState 在调用处实现
    // 完整版需自建 ShimmerModifier

// --- EmptyState ---
@Composable
fun EmptyState(
    title: String,
    subtitle: String,
    onAction: (() -> Unit)? = null,
    actionLabel: String? = null,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(Dimens.space12),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(Dimens.space10))
        Icon(
            imageVector = Icons.Default.SearchOff,
            contentDescription = null,
            tint = Neutral300,
            modifier = Modifier.size(80.dp),
        )
        Spacer(Modifier.height(Dimens.space4))
        Text(title, style = MaterialTheme.typography.headlineMedium, color = Neutral900)
        Spacer(Modifier.height(Dimens.space2))
        Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = Neutral500)
        if (onAction != null && actionLabel != null) {
            Spacer(Modifier.height(Dimens.space6))
            OutlinedButton(onClick = onAction, shape = RadiusFull) {
                Text(actionLabel)
            }
        }
    }
}

// --- ErrorState ---
@Composable
fun ErrorState(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(Dimens.space12),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(Dimens.space10))
        Icon(
            imageVector = Icons.Default.WifiOff,
            contentDescription = null,
            tint = Error,
            modifier = Modifier.size(80.dp),
        )
        Spacer(Modifier.height(Dimens.space4))
        Text("网络连接已断开", style = MaterialTheme.typography.headlineMedium, color = Neutral900)
        Spacer(Modifier.height(Dimens.space2))
        Text(message, style = MaterialTheme.typography.bodyMedium, color = Neutral500)
        Spacer(Modifier.height(Dimens.space6))
        Button(onClick = onRetry, shape = RadiusFull, colors = ButtonDefaults.buttonColors(containerColor = Primary)) {
            Text("重试", color = TextOnPrimary)
        }
    }
}
```

### 3.6 通用辅助函数

```kotlin
// 格式化销量
fun formatSalesCount(count: Int): String = when {
    count >= 10000 -> "${count / 10000}.${(count % 10000) / 1000}万人付款"
    else -> "${count}人付款"
}
```

---

## 四、状态管理

### 4.1 ChatViewModel (核心)

```kotlin
// viewmodel/ChatViewModel.kt
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

data class GuideUiState(
    // 消息列表
    val messages: List<ChatMessageUiModel> = emptyList(),

    // 输入
    val inputText: String = "",
    val isRecording: Boolean = false,
    val selectedImageUri: Uri? = null,
    val imageUploadProgress: Float? = null,

    // 流式
    val isStreaming: Boolean = false,
    val streamingText: String = "",
    val streamingCards: List<ProductCardUiModel> = emptyList(),
    val searchStatus: String = "",   // "正在搜索中…"

    // 追问
    val clarifyChips: List<String> = emptyList(),

    // 页面状态
    val screenState: ScreenState = ScreenState.Idle,

    // 对话
    val sessionId: String = UUID.randomUUID().toString(),
)

sealed class ScreenState {
    object Idle : ScreenState()       // 初始/新会话
    object Loading : ScreenState()    // 加载历史
    data class Streaming(val text: String) : ScreenState()
    data class Content(val hasProducts: Boolean) : ScreenState()
    data class Error(val message: String) : ScreenState()
    object Empty : ScreenState()      // 无历史
}

data class ChatMessageUiModel(
    val id: String,
    val role: MessageRole,
    val content: String,
    val productCards: List<ProductCardUiModel> = emptyList(),
    val timestamp: Long = System.currentTimeMillis(),
    val status: MessageStatus = MessageStatus.Sent,
    val errorMessage: String? = null,
)

enum class MessageRole { User, Assistant }
enum class MessageStatus { Sending, Sent, Streaming, Error }

data class ProductCardUiModel(
    val productId: String,
    val name: String,
    val price: Double,
    val originalPrice: Double? = null,
    val imageUrl: String,
    val source: String,
    val matchReason: String? = null,
    val rating: Float? = null,
    val salesCount: Int? = null,
)

class ChatViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(GuideUiState())
    val uiState: StateFlow<GuideUiState> = _uiState.asStateFlow()

    private val repository = ChatRepository()
    private val sseClient = SseClient()

    fun onInputChange(text: String) {
        _uiState.update { it.copy(inputText = text) }
    }

    fun sendMessage() {
        val text = _uiState.value.inputText.trim()
        if (text.isEmpty()) return

        val userMessage = ChatMessageUiModel(
            id = UUID.randomUUID().toString(),
            role = MessageRole.User,
            content = text,
        )

        _uiState.update {
            it.copy(
                messages = it.messages + userMessage,
                inputText = "",
                screenState = ScreenState.Streaming(""),
                isStreaming = true,
                streamingText = "",
                streamingCards = emptyList(),
                searchStatus = "正在搜索中…",
            )
        }

        viewModelScope.launch {
            sseClient.connect(
                sessionId = _uiState.value.sessionId,
                message = text,
                imageBase64 = null,
            ).collect { event ->
                when (event) {
                    is SSEEvent.TextDelta -> {
                        _uiState.update {
                            it.copy(
                                streamingText = it.streamingText + event.delta,
                                searchStatus = if (it.streamingText.length > 30) "" else it.searchStatus,
                            )
                        }
                    }
                    is SSEEvent.ProductCards -> {
                        _uiState.update {
                            it.copy(streamingCards = event.cards.map { p -> p.toUiModel() })
                        }
                    }
                    is SSEEvent.ClarifyChips -> {
                        _uiState.update {
                            it.copy(clarifyChips = event.chips)
                        }
                    }
                    is SSEEvent.Done -> {
                        val aiMessage = ChatMessageUiModel(
                            id = UUID.randomUUID().toString(),
                            role = MessageRole.Assistant,
                            content = _uiState.value.streamingText,
                            productCards = _uiState.value.streamingCards,
                        )
                        _uiState.update {
                            it.copy(
                                messages = it.messages + aiMessage,
                                isStreaming = false,
                                streamingText = "",
                                streamingCards = emptyList(),
                                clarifyChips = emptyList(),
                                searchStatus = "",
                                screenState = ScreenState.Content(aiMessage.productCards.isNotEmpty()),
                            )
                        }
                    }
                    is SSEEvent.Error -> {
                        _uiState.update {
                            it.copy(
                                isStreaming = false,
                                screenState = ScreenState.Error(event.message),
                            )
                        }
                    }
                }
            }
        }
    }

    fun onClarifyChipClick(chip: String) {
        _uiState.update { it.copy(inputText = chip) }
        // 自动发送
        viewModelScope.launch {
            delay(100) // 等状态更新
            sendMessage()
        }
    }

    fun retry() {
        // 重试最后一条用户消息
    }

    fun clearImage() {
        _uiState.update { it.copy(selectedImageUri = null) }
    }
}

// SSE 事件模型
sealed class SSEEvent {
    data class TextDelta(val delta: String) : SSEEvent()
    data class ProductCards(val cards: List<Product>) : SSEEvent()
    data class ClarifyChips(val chips: List<String>) : SSEEvent()
    data class Done(val sessionId: String) : SSEEvent()
    data class Error(val message: String) : SSEEvent()
}
```

### 4.2 其他 ViewModel 状态

```kotlin
// ProductViewModel 状态
data class ProductListUiState(
    val products: List<Product> = emptyList(),
    val selectedCategory: String = "推荐",
    val categories: List<String> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
)

// CompareViewModel 状态
data class CompareUiState(
    val products: List<Product> = emptyList(),
    val priceHistory: Map<String, List<PricePoint>> = emptyMap(),
    val selectedColor: String? = null,
    val isLoading: Boolean = false,
)

// ProfileViewModel 状态
data class ProfileUiState(
    val user: User? = null,
    val cartItems: List<Product> = emptyList(),
    val orderCounts: OrderCounts = OrderCounts(),
    val coupons: List<Coupon> = emptyList(),
    val isLoading: Boolean = false,
)
```

---

## 五、接口联调方案

### 5.1 SSE 方案 (首选 — 比赛加分)

```kotlin
// data/remote/SseClient.kt
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class SseClient {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS)  // 无限读取 (SSE长连接)
        .build()

    // 数据类同上 SSEEvent sealed class

    fun connect(
        sessionId: String,
        message: String,
        imageBase64: String?,
    ): Flow<SSEEvent> = callbackFlow {
        val json = JSONObject().apply {
            put("session_id", sessionId)
            put("message", message)
            if (imageBase64 != null) put("image", imageBase64)
        }

        val requestBody = json.toString()
            .toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url("${ApiClient.BASE_URL}/api/chat")
            .post(requestBody)
            .header("Accept", "text/event-stream")
            .build()

        val call = client.newCall(request)
        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                trySend(SSEEvent.Error("网络连接失败: ${e.message}"))
                close()
            }

            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful) {
                    trySend(SSEEvent.Error("服务错误: ${response.code}"))
                    close()
                    return
                }

                val source = response.body?.source() ?: run {
                    trySend(SSEEvent.Error("空响应"))
                    close()
                    return
                }

                val buffer = StringBuilder()
                while (!source.exhausted()) {
                    val line = source.readUtf8Line() ?: break

                    if (line.startsWith("data: ")) {
                        val data = line.removePrefix("data: ")
                        try {
                            val json = JSONObject(data)
                            val type = json.getString("type")
                            when (type) {
                                "text_delta" -> {
                                    val delta = json.optString("delta", "")
                                    trySend(SSEEvent.TextDelta(delta))
                                }
                                "product_cards" -> {
                                    val cardsArray = json.getJSONArray("cards")
                                    val cards = (0 until cardsArray.length()).map { i ->
                                        parseProduct(cardsArray.getJSONObject(i))
                                    }
                                    trySend(SSEEvent.ProductCards(cards))
                                }
                                "clarify_chips" -> {
                                    val chipsArray = json.getJSONArray("chips")
                                    val chips = (0 until chipsArray.length()).map { it.string }
                                    trySend(SSEEvent.ClarifyChips(chips))
                                }
                                "done" -> {
                                    trySend(SSEEvent.Done(sessionId))
                                    close()
                                    return
                                }
                            }
                        } catch (e: Exception) {
                            // 跳过解析失败的行
                        }
                    }
                }
                close()
            }
        })

        awaitClose { call.cancel() }
    }
}
```

### 5.2 MVP 替代方案 — 分段轮询 + 打字机效果

```
如果 SSE 在移动端遇到防火墙/NAT/超时问题，降级方案:

方案A: 分段轮询 (最简单)
  1. POST /api/chat → 返回 {task_id: "uuid"}
  2. GET /api/chat/{task_id}/status → 返回 {status: "processing", text: "已生成部分", cards: []}
  3. 前端每 500ms 轮询一次
  4. status = "done" 时停止

方案B: 打字机效果模拟 (无后端支持时)
  1. POST /api/chat → 返回完整结果 {text: "...", cards: [...]}
  2. 前端用 typewriter 算法逐字显示文本
  3. 文本全部显示后, 卡片渐入

// 打字机效果实现
@Composable
fun TypewriterText(
    fullText: String,
    speedMs: Long = 30,
    onComplete: () -> Unit,
) {
    var displayedLength by remember { mutableIntStateOf(0) }

    LaunchedEffect(fullText) {
        while (displayedLength < fullText.length) {
            delay(speedMs)
            displayedLength++
        }
        onComplete()
    }

    Text(
        text = buildAnnotatedString {
            append(fullText.take(displayedLength))
            if (displayedLength < fullText.length) {
                withStyle(SpanStyle(color = Primary)) { append("▌") }
            }
        },
        style = MaterialTheme.typography.bodyLarge,
    )
}
```

### 5.3 Retrofit 配置

```kotlin
// data/remote/ApiClient.kt
object ApiClient {
    // Android 模拟器用 10.0.2.2, 真机用实际IP
    const val BASE_URL = "http://10.0.2.2:8000"

    val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .addHeader("Accept", "application/json")
                    .build()
                chain.proceed(request)
            }
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            })
            .build()
    }

    val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
}

// data/remote/ApiService.kt
interface ApiService {
    @POST("api/chat")
    suspend fun chat(@Body request: ChatRequest): ChatResponse

    @GET("api/products")
    suspend fun getProducts(
        @Query("category") category: String? = null,
        @Query("page") page: Int = 1,
        @Query("size") size: Int = 20,
    ): ProductListResponse

    @GET("api/products/{id}")
    suspend fun getProductDetail(@Path("id") id: String): ProductDetailResponse

    @GET("api/products/{id}/price-history")
    suspend fun getPriceHistory(@Path("id") id: String): PriceHistoryResponse

    @POST("api/feedback")
    suspend fun sendFeedback(@Body feedback: FeedbackRequest): FeedbackResponse
}
```

---

## 六、页面实现顺序

### P0 — Sprint 1: 地基 + 核心对话 (3h)

```
□ 1.1  创建 Color.kt / Type.kt / Shape.kt / Dimens.kt / Theme.kt
□ 1.2  实现 Screen sealed class + NavGraph 路由框架
□ 1.3  实现 BottomNavBar (4 Tab) + TopNavBar
□ 1.4  实现 Product / ChatMessage / SSEEvent 数据模型
□ 1.5  实现 MockProducts (10条) + MockChats + MockUser
□ 1.6  实现 ProductCard + ProductCardHorizontal
□ 1.7  实现 MessageBubble (user/agent 两种样式)
□ 1.8  实现 StreamingMessageBubble (逐字+光标+卡片渐入)
□ 1.9  实现 ChatInputBar (文本输入+发送)
□ 1.10 实现 ChatViewModel (发送→SSE→流式→done)
□ 1.11 实现 ChatGuideScreen (组合: 消息列表+输入栏)
□ 1.12 实现 LoadingSkeleton / EmptyState / ErrorState
□ 1.13 真机编译验证 → ChatGuideScreen 可演示对话闭环
```

### P1 — Sprint 2: 商品浏览 (2h)

```
□ 2.1  实现 SearchBar (统一搜索栏)
□ 2.2  实现 CategoryTabs (横向滚动)
□ 2.3  实现 ProductGrid (LazyVerticalGrid, 2列)
□ 2.4  实现 HomeScreen (每日推荐, 2×2Grid + 问候语)
□ 2.5  实现 ExploreScreen (分类Tab + 商品Grid)
□ 2.6  实现 CompareScreen (比价列表)
□ 2.7  实现 ProductViewModel + ExploreViewModel + CompareViewModel
□ 2.8  验证: 3个浏览页面可滚动、可切换分类
```

### P2 — Sprint 3: 商品详情 + 比价 (1.5h)

```
□ 3.1  实现 ProductDetailCard
□ 3.2  实现 PriceTrendChart (Canvas折线图)
□ 3.3  实现 ColorVariantPicker
□ 3.4  实现 ProductSourceBadge + RecommendationReasonTag
□ 3.5  实现 PriceFloatingPanel
□ 3.6  实现 ProductDetailScreen
□ 3.7  验证: 详情页完整交互闭环
```

### P2 — Sprint 4: 个人中心 + 设置 (1h)

```
□ 4.1  实现 ProfileScreen (用户+购物车+订单+领券)
□ 4.2  实现 SettingsScreen (设置列表)
□ 4.3  实现 HistoryDrawer + HistoryItem
□ 4.4  验证: 个人中心可浏览
```

### P2 — Sprint 5: 多模态输入 (1h)

```
□ 5.1  实现 ImageSearchButton + 拍照/选图流程
□ 5.2  实现 VoiceInputButton + 长按录音流程
□ 5.3  实现 ClarifyChip + QuickActionChips
□ 5.4  实现 MultimodalSearchBar
□ 5.5  验证: 拍照/语音入口可触发
```

### P2 — Sprint 6: 联调 + 打磨 (1.5h)

```
□ 6.1  SseClient 对接真实后端
□ 6.2  Retrofit 对接商品列表/详情接口
□ 6.3  反馈接口联调
□ 6.4  所有页面添加错误处理 (网络超时/服务异常)
□ 6.5  添加页面转场动画 (NavAnimations)
□ 6.6  真机适配测试 (320/375/428宽度)
□ 6.7  Release APK 打包
```

---

## 七、验收标准

### 7.1 功能验收

```
□ 对话导购闭环: 输入需求 → 看到AI流式回复 → 看到商品卡片 → 点击卡片
□ 商品浏览: 首页/探索/比价 3页面可正常滚动和切换
□ 底部导航: 4 Tab 可切换, 当前页高亮
□ 分类切换: 标签横向滚动, 点击切换商品列表
□ 流式输出: 文字逐字显示 + ▽光标 + 卡片渐入 (或打字机模拟)
□ 错误处理: 断网时显示错误页 + 重试按钮, 不崩溃
□ 空状态: 无搜索结果时显示空状态插图
```

### 7.2 视觉验收

```
□ 品牌色: 所有价格红色 #FF5C5C, 按钮/标签颜色统一
□ 价格突出: 价格字号 ≥ 商品名字号, 视觉权重 3:1
□ 卡片稳定: 所有商品卡片图片 1:1 比例, 无布局跳动
□ 字体一致: 全文不超过 3 种字重
□ 无紫色渐变: 全局搜索无紫色渐变残留
```

### 7.3 代码质量

```
□ 组件拆分: 无单个 .kt 文件超过 300 行
□ 主题统一: 所有颜色/字号/间距走 Theme 变量, 无硬编码值
□ Mock 分离: Mock 数据在 data/mock/ 目录, 不混入业务层
□ 状态覆盖: 每个列表页有 Loading/Empty/Error 三态
□ 可编译: ./gradlew assembleDebug 零错误
```

### 7.4 适配

```
□ 320pt (SE): 商品Grid自动变为2列
□ 375pt (12/13): 基准宽度正常
□ 428pt (PM): 卡片间距自动适配
□ 底部安全区: 输入栏不被导航栏遮挡
```

---

## 八、Claude Code 执行任务提示词

以下提示词可直接复制给 Claude Code，按 Sprint 逐批执行。

### Task 1: 地基 (Sprint 0)

```
你是 Android Compose 专家。
工作目录: /mnt/c/Users/fujunye/Desktop/Hermes/04-rag-ecommerce/apps/android/

任务: 创建 Compose 主题系统和项目地基

1. 创建 ui/theme/Color.kt:
   - 复制下方完整代码 (含 Primary=#FF5C5C, Neutral0-900, Success/Warning/Info/Error)
   [粘贴 Color.kt 完整代码]

2. 创建 ui/theme/Type.kt:
   - Typography + PriceLarge/PriceMedium/PriceSmall TextStyle
   [粘贴 Type.kt 完整代码]

3. 创建 ui/theme/Shape.kt:
   - RadiusNone/Sm/Md/Lg/Xl/Full + UserBubbleShape + AgentBubbleShape
   [粘贴 Shape.kt 完整代码]

4. 创建 ui/theme/Dimens.kt:
   - 4pt 栅格间距系统
   [粘贴 Dimens.kt 完整代码]

5. 创建 ui/theme/Theme.kt:
   - ShoppingAgentTheme composable
   [粘贴 Theme.kt 完整代码]

6. 修改 MainActivity.kt:
   - 用 ShoppingAgentTheme 包裹
   - 挂载 NavGraph (4个占位Page)
   - 实现 BottomNavBar

7. 创建 Screen.kt sealed class (路由定义)

8. 创建 NavGraph.kt (Navigation Compose)

约束:
- 包名 com.shopping.agent
- 不引入未声明的第三方库
- 编译通过

产出: 新建 Color.kt/Type.kt/Shape.kt/Dimens.kt/Theme.kt/Screen.kt/NavGraph.kt, 修改 MainActivity.kt
```

### Task 2: 核心对话 (Sprint 1)

```
你是 Android Compose 专家。
工作目录: 同上。

任务: 实现对话导购核心页面

1. 创建数据模型 (data/model/):
   - Product.kt (id, name, price, originalPrice, imageUrl, source, matchReason, salesCount, rating, colorVariants)
   - ChatMessage.kt (id, role, content, productCards, timestamp, status)
   - SSEEvent.kt (sealed class: TextDelta, ProductCards, Done, Error)

2. 创建 Mock 数据 (data/mock/):
   - MockProducts.kt: 10条商品, 3品类 (运动鞋/耳机/水杯)
   - MockChats.kt: 3轮示例对话

3. 实现组件:
   - ProductCard.kt (Grid版 + Horizontal版)
   - MessageBubble.kt (user红色圆角右对齐, agent白色左对齐微阴影)
   - StreamingMessageBubble.kt (逐字追加 + 闪烁光标▌ + 商品卡渐入)
   - ChatInputBar.kt (文本+发送, 先不做相机/语音)
   - LoadingSkeleton.kt (ProductCardSkeleton + ChatSkeleton)
   - EmptyState.kt + ErrorState.kt

4. 实现 ChatViewModel:
   - GuideUiState (messages, inputText, isStreaming, streamingText, streamingCards, screenState)
   - sendMessage() 用 Mock SSE 模拟 (500ms延迟后逐字输出)
   - onInputChange()

5. 实现 ChatGuideScreen:
   - LazyColumn 消息列表 + ChatInputBar 底部固定
   - 流式消息: 显示 StreamingMessageBubble
   - 空状态: "早上好, 想买什么？" + 示例问题Chip

约束: 同上
产出: 以上全部文件
```

### Task 3-6: 后续 Sprint 提示词 (模板)

```
Sprint N: [具体任务]

工作目录: 同上。现有文件清单已了解, 只修改和新增。

任务:
1. [具体文件路径 + 功能描述]
2. [具体文件路径 + 功能描述]
...

约束:
- 不修改已完成的核心对话逻辑
- 所有新增组件走 Theme 变量
- 包名 com.shopping.agent
- 编译通过

产出: 列出新建/修改的文件
```

---

> **文档路径**: `04-rag-ecommerce/docs/standards/ANDROID_COMPOSE_MIGRATION.md`
> **备份路径**: `.backup_20260525_144414/`
> **总工时**: 6 Sprint, 10h
> **目标**: 可演示比赛原型, 后续无缝对接真实后端
