# FEAT-FRONTEND — 购物车完整功能实现

## 概述

为 Android 端实现完整的购物车功能，包括"我的"页面购物车预览卡片和独立的购物车管理页面。支持商品按商家分组、多选管理、快速清理、数量编辑等功能。数据库新增 `is_selected` 字段记录商品选中状态。

---

## 涉及文件

| 文件 | 操作 | 说明 |
|---|---|---|
| `apps/android/app/src/main/java/com/shopping/agent/data/local/LocalDatabase.kt` | 修改 | Schema v7→v8，`cart_items` 新增 `is_selected` 列 |
| `apps/android/app/src/main/java/com/shopping/agent/data/local/UserRepository.kt` | 修改 | 新增购物车选中状态、批量操作等方法 |
| `apps/android/app/src/main/java/com/shopping/agent/data/model/CartItem.kt` | 修改 | 新增 `isSelected` 字段 |
| `apps/android/app/src/main/java/com/shopping/agent/viewmodel/CartViewModel.kt` | 重写 | 从远程 API 切换为本地 SQLite，新增管理模式、选中状态、快速清理 |
| `apps/android/app/src/main/java/com/shopping/agent/ui/screens/CartScreen.kt` | 重写 | 完整购物车页面实现 |
| `apps/android/app/src/main/java/com/shopping/agent/ui/screens/ProfileScreen.kt` | 修改 | 购物车预览卡片从 Mock 数据切换为数据库真实数据 |
| `apps/android/app/src/main/java/com/shopping/agent/ui/navigation/NavGraph.kt` | 修改 | Profile 页面接入购物车导航回调 |
| `apps/android/app/src/main/java/com/shopping/agent/ui/navigation/Screen.kt` | 修改 | 新增 `Cart` 路由常量 |
| `apps/android/AGENTS.md` | 修改 | 标记所有购物车任务为已完成 |

---

## 1. 数据库 Schema 升级 — LocalDatabase.kt

### 变更点

- **`DATABASE_VERSION`**: `7` → `8`
- **`cart_items` 表新增列**: `is_selected INTEGER DEFAULT 0`

### 新建表 DDL（`cart_items` 完整结构）

```sql
CREATE TABLE cart_items (
    product_id  TEXT PRIMARY KEY,
    session_id  TEXT NOT NULL,
    user_id     TEXT DEFAULT '',
    title       TEXT DEFAULT '',
    price       REAL DEFAULT 0,
    brand       TEXT DEFAULT '',
    category    TEXT DEFAULT '',
    image_url   TEXT DEFAULT '',
    rating      REAL DEFAULT 0,
    quantity    INTEGER DEFAULT 1,
    is_selected INTEGER DEFAULT 0,
    added_at    INTEGER NOT NULL
)
```

### 迁移逻辑（`onUpgrade` v7→v8）

```sql
ALTER TABLE cart_items ADD COLUMN is_selected INTEGER DEFAULT 0
```

列已存在则忽略（try-catch 保护），兼容已有数据库升级。

---

## 2. 数据模型 — CartItem.kt

```kotlin
data class CartItem(
    val product: Product,
    val quantity: Int = 1,
    val isSelected: Boolean = false,  // 新增
)
```

---

## 3. 数据仓库扩展 — UserRepository.kt

### 新增方法

| 方法 | 说明 |
|---|---|
| `updateCartItemSelection(productId, isSelected)` | 更新单件商品选中状态 |
| `updateCartItemsSelection(productIds, isSelected)` | 批量更新商品选中状态 |
| `updateCartItemQuantity(productId, quantity)` | 更新商品数量 |
| `deleteCartItems(productIds)` | 批量删除购物车商品 |
| `getCartItemCount()` | 获取当前用户购物车商品总数 |

### 已有方法修改

| 方法 | 变更 |
|---|---|
| `saveCartItem()` | 新增写入 `is_selected = 1`（新加入商品默认选中） |
| `saveCartItemForCurrentUser()` | 同上 |
| `getCartItems()` | 读取 `is_selected` 列 |
| `buildCartItemFromCursor()` | 读取 `is_selected` 列 |

---

## 4. 购物车 ViewModel — CartViewModel.kt（重写）

### 架构变更

从原来通过 OkHttp 调用远程 API（`/api/v1/cart/*`）改为直接使用本地 SQLite（通过 `UserRepository`）管理购物车数据。

### 状态模型

```kotlin
data class CartUiState(
    val items: List<CartItem>,              // 全部商品
    val isManageMode: Boolean,              // 管理模式开关
    val selectedProductIds: Set<String>,     // 当前选中商品 ID 集合
    val selectedTotalPrice: Double,          // 选中商品总价
    val totalPrice: Double,                  // 全部商品总价
    val isLoading: Boolean,
    val error: String?,
    val orderResult: String?,               // 下单结果
    val showQuickClean: Boolean,            // 快速清理面板开关
    val quickCleanSelectedIds: Set<String>, // 快速清理选中状态
)
```

### 核心方法

| 方法 | 说明 |
|---|---|
| `loadCart()` | 从 SQLite 加载购物车数据 |
| `toggleManageMode()` | 切换管理模式，退出时持久化选中状态到 DB |
| `toggleItemSelection(productId)` | 切换单个商品选中（仅缓存） |
| `toggleSelectAll()` | 全选/取消全选（仅缓存） |
| `toggleBrandSelection(brand)` | 按商家全选/取消全选 |
| `isBrandFullySelected(brand)` | 判断某商家商品是否全部选中 |
| `increaseQuantity(productId)` | 数量+1（上限 1000），异步写 DB |
| `decreaseQuantity(productId)` | 数量-1（减至 0 则删除），异步写 DB |
| `updateQuantity(productId, quantity)` | 用户手动输入数量（1-1000），异步写 DB |
| `removeFromCart(productId)` | 删除单件商品，异步写 DB |
| `performDeleteSelected()` | 批量删除选中商品，异步写 DB |
| `addToCart(product)` | 添加商品到购物车 |
| `clearCart()` | 清空购物车 |
| `placeOrder()` | 下单（生成本地订单号） |
| `openQuickClean()` / `closeQuickClean()` | 快速清理面板开关 |
| `toggleQuickCleanSelection(productId)` | 快速清理中切换选中 |
| `toggleQuickCleanSelectAll()` | 快速清理中全选 |
| `performQuickCleanDelete()` | 执行快速清理删除 |

### 关键常量

```kotlin
companion object {
    const val MAX_QUANTITY = 1000  // 单件商品最大数量
}
```

---

## 5. 购物车页面 — CartScreen.kt（重写）

### 5.1 页面结构

```
┌─ GradientTopBar ─────────────────────────┐
│  ← 返回   购物车           [管理]/[完成]    │
├──────────────────────────────────────────┤
│  [管理模式操作栏]                           │
│  全选 ○           🗑️快速清理    删除       │
├──────────────────────────────────────────┤
│  LazyColumn（按商家分组）                   │
│  ┌─ 商家卡片 ──────────────────────────┐  │
│  │  ○ 商家名称                          │  │
│  │  ┌─ 商品行 ─────────────────────┐   │  │
│  │  │ ○ [图] 商品名称...            │   │  │
│  │  │        ¥价格    [-] 1 [+]    │   │  │
│  │  └──────────────────────────────┘   │  │
│  └──────────────────────────────────────┘  │
├──────────────────────────────────────────┤
│  [底部栏 - 非管理模式]                      │
│  ○ 全选   合计 ¥xxx.xx     [去结算]        │
└──────────────────────────────────────────┘
```

### 5.2 顶栏 (`CartTopBar`)

- 左侧：返回箭头 + "购物车" 标题
- 右侧：`管理` 按钮（有商品时显示），点击变为 `完成` 按钮
- 点击 `完成`：退出管理模式，将当前选中状态持久化到数据库

### 5.3 管理模式操作栏 (`ManageActionBar`)

管理模式激活时，顶部栏下方出现操作栏（水平排列）：

| 位置 | 组件 | 功能 |
|---|---|---|
| 左 | ○ 全选 | 选中所有商品（仅缓存，不写 DB） |
| 右 | 🗑️ 快速清理（蓝色文字） | 打开底部弹出面板 |
| 右 | 删除 | 删除选中商品（需确认选中数 > 0） |

### 5.4 商家分组 (`BrandGroupCard`)

- 商品按 `product.brand` 分组（brand 为 null 时分入"其他"组）
- 每组顶部有商家全选栏：圆圈按钮 + 商家名称
- 商家全选按钮点击后切换该商家所有商品的选中状态

### 5.5 商品行 (`CartProductRow`)

三列布局：

| 列 | 内容 |
|---|---|
| 第 1 列 | 选中圆圈按钮（○ / ✓ 蓝色） |
| 第 2 列 | 商品图片（圆角正方形，占卡片宽度 ~32%） |
| 第 3 列 | 商品名称（单行省略）+ 价格 + 数量控制 |

数量控制组件：
- `-` 按钮：数量减 1（减至 0 时删除商品）
- 中间数字：显示当前数量，点击弹出数量编辑对话框
- `+` 按钮：数量加 1（上限 1000）

### 5.6 数量编辑弹窗 (`QuantityEditDialog`)

- 标题："修改数量"
- 数字输入框：标签 "数量（1-1000）"
- 仅允许输入数字，超出范围或为空时显示错误提示
- 取消 / 确定 按钮

### 5.7 底部结算栏 (`CartBottomBar`)

非管理模式显示：

| 位置 | 组件 | 功能 |
|---|---|---|
| 左 | ○ 全选 + 合计金额 | 全选按钮 + "合计 ¥xxx.xx" |
| 右 | 去结算 按钮 | 选中商品数量 > 0 时可用，点击下单 |

### 5.8 快速清理面板 (`QuickCleanSheet`)

`ModalBottomSheet`，填充高度 70%：

- **顶部栏**: "快速清理" + "共 N 件商品" + 全选按钮 + × 关闭
- **主体**: 4 列 `LazyVerticalGrid`，每个商品显示圆角正方形图片
  - 选中时右上角显示蓝色 ✓ 标记
  - 点击切换选中状态
- **底部栏**: 红色删除按钮 "删除（N）"，选中数 > 0 时可用

### 5.9 空状态 (`CartEmptyState`)

- 购物车图标（64dp）
- "购物车为空" 文字
- "去首页逛逛 AI 导购推荐吧" 副文字

### 5.10 下单成功弹窗 (`OrderSuccessDialog`)

- ✓ 图标（圆形蓝色背景）
- "下单成功！" 标题
- 订单号 + 实付金额
- "完成" 按钮关闭弹窗

---

## 6. "我的"页面购物车预览 — ProfileScreen.kt

### 变更点

| 组件 | 变更 |
|---|---|
| `ProfileScreen` 函数 | 新增 `onCartClick: () -> Unit` 参数；新增 `cartItems` 状态 + `LaunchedEffect` 加载 |
| `CartPreviewSection` | 函数签名从无参变为接收 `cartItems: List<CartItem>`；空购物车显示"购物车空空如也~" |
| `CartProductMiniCard` | 从 Mock 数据卡片改为展示真实 `CartItem` 数据（使用 `AsyncImage` 加载商品图） |

### 数据加载

```kotlin
LaunchedEffect(Unit) {
    val prefs = context.getSharedPreferences("cart_prefs", Context.MODE_PRIVATE)
    val sessionId = prefs.getString("cart_session_id", "") ?: ""
    val items = repository.getCartItemsForCurrentUser(sessionId)
    cartItems = items
}
```

### 显示逻辑

- 购物车非空：标题行显示 "N件商品"，最多展示 4 件商品
- 购物车为空：标题行不显示数量，内容区显示 "购物车空空如也~"
- 点击整个卡片区域导航到购物车页面

---

## 7. 导航对接

### NavGraph.kt

- `profile` composable 传入 `onCartClick = { navController.navigate(Screen.Cart.route) }`
- `cart` composable 已存在，路由改为使用 `Screen.Cart.route`

### Screen.kt

- 新增 `data object Cart : Screen("cart")` 路由常量

### 导航路径

```
ProfileScreen → (点击购物车卡片) → CartScreen
```

---

## 8. 技术要点

- **数据库**: 原生 Android SQLite（SQLiteOpenHelper v8），协程 `Dispatchers.IO` 异步读写
- **Compose UI**: 全部使用 Jetpack Compose + Material 3
- **图片加载**: Coil `AsyncImage`，圆角裁剪 `RoundedCornerShape`
- **暗色模式**: 所有组件使用 `MaterialTheme.colorScheme` 动态色值，自动适配深色主题
- **状态管理**: `CartViewModel` 使用 `StateFlow` + `MutableStateFlow`，UI 通过 `collectAsState()` 订阅
- **数量编辑**: `Dialog` + `OutlinedTextField` + `KeyboardType.Number`，前端校验 1-1000 范围
- **商家分组**: `groupBy { it.product.brand ?: "其他" }` 实现，使用 `remember` 缓存分组结果
- **快速清理**: `ModalBottomSheet` + `LazyVerticalGrid(GridCells.Fixed(4))` 实现 4 列网格
- **选中圆圈组件**: `SelectCircle` Composable，内部分离关注点，统一选中/未选中视觉样式

### 数据流

```
UserRepository (SQLite)
    ↕ IO 线程读写
CartViewModel (StateFlow)
    ↕ collectAsState()
CartScreen / ProfileScreen (Compose UI)
```

所有写操作采用"乐观更新"策略：先更新 UI 状态，再异步写数据库。失败时由 `error` 字段通知用户。

---

## 9. 设计决策

### 为何从远程 API 切换到本地 SQLite？

原 `CartViewModel` 通过 OkHttp 调用后端 `/api/v1/cart/*` API。在离线场景或后端不可用时购物车功能完全失效。改为本地 SQLite 后：

1. 购物车数据本地持久化，离线可用
2. 读写延迟从网络 RTT（~100ms）降至本地 I/O（~1ms）
3. 与现有 `UserRepository` 架构一致，复用已有 cart_items 表

### 为何 `is_selected` 默认值为 1？

新加入购物车的商品默认为选中状态（`is_selected = 1`），符合用户预期——刚加购的商品通常需要结算。

### 管理模式为何不直接写数据库？

管理模式的"全选"操作仅修改内存中的 `selectedProductIds` 集合。点击"完成"退出管理时才批量写数据库。这避免了每次勾选都触发 DB I/O，提升操作响应速度。
