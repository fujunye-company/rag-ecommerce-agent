# FEAT-ANDROID-SETTINGS — 用户唯一标识 + 个人信息编辑

## 概述

为 Android 端实现用户唯一标识（拾物号）与个人信息编辑功能，包括数据库 schema 升级、UserRepository 适配、新建 ProfileEditScreen 页面以及导航对接。

---

## 涉及文件

| 文件 | 操作 | 说明 |
|---|---|---|
| `apps/android/app/src/main/java/com/shopping/agent/data/local/LocalDatabase.kt` | 修改 | Schema 升级 v1→v2 |
| `apps/android/app/src/main/java/com/shopping/agent/data/local/UserRepository.kt` | 修改 | 适配新表结构，新增头像读写 |
| `apps/android/app/src/main/java/com/shopping/agent/ui/screens/ProfileEditScreen.kt` | 新建+修复 | 个人信息编辑页面，相机/相册/昵称/年龄段修复 |
| `apps/android/app/src/main/java/com/shopping/agent/ui/screens/SettingsScreen.kt` | 修改 | 个人信息行对接导航 |
| `apps/android/app/src/main/java/com/shopping/agent/ui/navigation/NavGraph.kt` | 修改 | 新增 `profile_edit` 路由 |
| `apps/android/app/src/main/java/com/shopping/agent/ui/screens/ProfileScreen.kt` | 修改 | 我的页面显示头像 |
| `apps/android/AGENTS.md` | 修改 | 标记已完成 |

---

## 1. 数据库 Schema 升级 — LocalDatabase.kt

### 变更点

- **`DATABASE_VERSION`**: `1` → `2`
- **`user_profile.id`**: `INTEGER PRIMARY KEY DEFAULT 1` → `TEXT PRIMARY KEY`（UUID，以 `"sw"` 前缀开头）
- **新增列 `avatar`**: `BLOB` 类型，存储用户头像的 JPEG 二进制数据

### 新建表 DDL

```sql
CREATE TABLE user_profile (
    id          TEXT PRIMARY KEY,
    nickname    TEXT DEFAULT '',
    gender      TEXT DEFAULT '',
    age_range   TEXT DEFAULT '',
    budget_min  REAL DEFAULT 0,
    budget_max  REAL DEFAULT 99999,
    preferred_categories TEXT DEFAULT '[]',
    avatar      BLOB,
    created_at  INTEGER NOT NULL,
    updated_at  INTEGER NOT NULL
);
```

初始数据插入时自动生成 `"sw" + UUID.randomUUID()` 作为用户唯一标识。

### 迁移逻辑 (`onUpgrade`)

v1 → v2 迁移步骤：
1. 从旧表读取用户已有数据（昵称、性别、年龄段、预算、偏好分类）
2. `DROP TABLE IF EXISTS user_profile`
3. 按新 schema 重建表
4. 生成新的 `"sw"` UUID 并插入迁移后的用户数据

---

## 2. 数据仓库适配 — UserRepository.kt

### 变更点

| 方法 | 说明 |
|---|---|
| `getUserId()` (新增, private) | 从数据库查询当前用户 UUID，所有用户操作统一使用此 ID |
| `getUserProfile()` | 查询条件从 `id=1` 改为 `id=?` 动态匹配 |
| `updateUserProfile()` | 更新条件从 `id=1` 改为 `id=?` 动态匹配 |
| `getUserAvatar()` (新增) | 返回 `ByteArray?`，读取 `avatar` BLOB 列 |
| `updateUserAvatar(avatarBytes)` (新增) | 写入 `avatar` BLOB 列，同步更新 `updated_at` |

---

## 3. 个人信息页面 — ProfileEditScreen.kt

新建 `ProfileEditScreen` Composable，包含以下模块：

### 3.1 头像

- **显示**: 圆形 96dp，顶部居中，带 `shadowElevation = 2.dp`
- **默认状态**: 显示相机图标占位
- **已有头像**: 从 SQLite 加载 BLOB → Bitmap → 圆形裁剪显示
- **点击**: 弹出 `AvatarSourceBottomSheet` 选择器

#### 头像来源选择器

两个圆角卡片容器，有间距分隔：
- **卡片 1（拍照 + 相册）**: `拍照` / `从手机相册选择` 两个 `TextButton` 竖向排列
- **卡片 2（取消）**: `取消` 按钮，点击关闭选择器

#### 图片裁剪界面

拍照或选图后进入 `ImageCropScreen`:
- **TopBar**: `取消`(左) | `修剪图片`(中) | `确定`(右)
- **Body**: 全屏黑色背景，图片支持双指缩放/平移（`detectTransformGestures`），中心叠加圆形裁剪框（白色半透明圆环）
- **确定**: 以裁剪框位置为基准从原图截取正方形区域 → JPEG 压缩 85% → `updateUserAvatar()` 写入 SQLite → Toast `"头像已更新"`

### 3.2 个人信息卡片

`Card(RadiusLg)` 包含 4 行，每行选项名靠左、值靠右：

#### 昵称

- 显示当前昵称，右侧浅灰色 `>` 箭头
- 点击弹出 `NicknameEditSheet`（ModalBottomSheet）:
  - 顶部栏: `修改昵称`(居中) + `×`(右，关闭)
  - 输入行: `昵称`(20%) + `OutlinedTextField`(80%)，输入框右侧灰底白叉清空按钮
  - 提示: `"昵称限制1-30个字符，一个汉字为1个字符"`
  - 保存按钮: 蓝色背景圆角，输入为空时禁用（灰底暗字）
  - 两侧 margin 7%
  - 保存后 Toast `"昵称已更新"`

#### 拾物号

- 显示 `user_profile.id`（即 `"sw"` UUID）
- 无 `>` 箭头
- 点击弹出 Toast: `"拾物号为拾物APP用户唯一标识，暂不支持修改。"`

#### 性别

- 显示: `"male"` → `"男"`, `"female"` → `"女"`, 其他 → `""`
- 点击弹出 `GenderPickerSheet`（ModalBottomSheet）:
  - 顶部栏: `选择性别`(居中) + `×`(右)
  - `RadioButton` 列表: `男` / `女`
  - 底部 `确认` 蓝色按钮
  - 确认后 Toast `"性别已更新"`

#### 年龄段

- 显示当前年龄段（如 `"20-29"`）
- 点击弹出 `AgeRangePickerSheet`（ModalBottomSheet）:
  - 顶部栏: `选择年龄段`(居中) + `×`(右)
  - `RadioButton` 列表: 11 个选项（`0-9` ~ `100及以上`），10 岁间隔
  - 底部 `确认` 蓝色按钮
  - 确认后 Toast `"年龄段已更新"`

### 3.3 数据同步

所有修改通过 `UserRepository` 直接写入 SQLite，页面重新显示时重新查询数据库。

---

## 4. 导航对接

### SettingsScreen.kt

- 函数签名新增 `onNavigateToProfileEdit: () -> Unit` 参数
- `"个人信息"` 行的 `onClick` 从 `{}` 改为实际导航回调

### NavGraph.kt

- `settings` 路由传入 `onNavigateToProfileEdit = { navController.navigate("profile_edit") }`
- 新增 `composable("profile_edit")` 路由，渲染 `ProfileEditScreen`

### 导航路径

```
SettingsScreen → (点击"个人信息") → ProfileEditScreen
```

---

## 5. 技术要点

- **Compose UI**: 全部使用 Jetpack Compose + Material 3
- **头像裁剪**: `graphicsLayer` 实现缩放平移 + `Canvas` 绘制裁剪指示圈 + `Bitmap.createBitmap()` 像素级裁剪
- **图片来源**: `ActivityResultContracts.TakePicture`（拍照，FileProvider URI） + `ActivityResultContracts.GetContent`（相册）
- **图片分辨率压缩**: 所有图片来源均经过 `BitmapFactory.Options.inSampleSize` 降采样（最大 480px），存储前再经 `createScaledBitmap` + JPEG 压缩（质量 80%）
- **数据库**: 原生 Android SQLite（SQLiteOpenHelper），协程 `Dispatchers.IO` 异步读写
- **UserRepository 单例**: 通过 `remember { UserRepository(context) }` 在 Composable 作用域内持有

---

## 6. Bug 修复记录

### 6.1 相机拍照闪退

**问题**: 点击"拍照"选项后闪退，无法打开相机。

**原因**: 使用 `ActivityResultContracts.TakePicturePreview` 可能在某些设备/模拟器上不稳定，且返回的缩略图 Bitmap 无法控制分辨率。

**修复**: 改用 `ActivityResultContracts.TakePicture` + `FileProvider` 临时文件 URI 方案：
- 在 `context.cacheDir` 创建临时 `.jpg` 文件
- 通过 `FileProvider.getUriForFile()` 获取 content URI（使用已有的 `${applicationId}.fileprovider` authority）
- `cameraLauncher.launch(uri)` 传入 URI，系统相机将照片写入该文件
- 读取时通过 `decodeSampledBitmapFromUri()` 降采样到最大 480px
- 外层包裹 try-catch，失败时 Toast 提示"无法打开相机"

### 6.2 相册选择头像闪退

**问题**: 相册选择完成后头像无法正常加载，几秒后闪退；再次进入个人信息页面也会闪退。

**原因**: 原始代码直接将相册返回的大分辨率图片（可能 4000×3000 像素以上）加载为 Bitmap，导致 `OutOfMemoryError`。同时存入 SQLite 的 BLOB 数据量过大，再次加载时同样 OOM。

**修复**:
- 相册图片读取使用 `BitmapFactory.Options.inSampleSize` 降采样（`decodeSampledBitmapFromUri()`），确保解码后不超过 480px
- 存入数据库前额外调用 `compressBitmapToMaxDim()` 二次压缩到 480px，并以 JPEG 质量 80% 写入
- 从数据库读取头像时同样使用 `decodeSampledBitmapFromBytes()` 降采样解码
- 所有 Bitmap 操作包裹 try-catch，失败时有 Toast 提示

### 6.3 头像无法在"我的"页面显示

**问题**: ProfileScreen（我的页面）的 `ProfileHeader` 硬编码显示"海"字，不显示用户上传的头像。

**修复**:
- `ProfileScreen` 新增 `UserRepository` 实例和 `LaunchedEffect` 加载头像 BLOB
- `ProfileHeader` 新增 `avatarBitmap: Bitmap?` 参数
- 有头像时显示 `Image(bitmap, ContentScale.Crop)`，无头像时保持原有"海"字占位

### 6.4 昵称修改 — 输入框限制改为确认时校验

**问题**: 原实现在 `OutlinedTextField.onValueChange` 中限制 `it.length <= 30`，用户无法输入超过 30 个字符。

**修复**: 移除 `onValueChange` 中的长度限制，改为在"保存"按钮 `onClick` 中校验：若 `nickname.length > 30` 则 Toast 提示"你提交的昵称不能使用，请更换其他昵称后再试"，不执行保存。

### 6.5 年龄段选择器 — 滑动容器 + 高度限制

**问题**: 年龄段 11 个选项全量展开，可能超出屏幕范围。

**修复**: 将 `RadioButton` 列表包裹在 `Column(Modifier.fillMaxWidth().fillMaxHeight(0.25f).verticalScroll())` 中：
- 选项区域最高不超过屏幕 25% 高度
- 超出部分可垂直滚动
- 确认按钮位于滑动容器下方，有 12dp 间距

### 6.6 设置页顶部通知铃铛注释

**问题**: 设置页 `GradientTopBar` 右侧曾计划添加带红色角标（数字 3）的通知铃铛图标（`BadgedBox` + `Icons.Default.Notifications`），但该功能暂未实现。

**处理**: 将通知铃铛代码注释掉，`Icons.Default.MoreVert`（"更多"）按钮为原有功能，保持不变。

**涉及代码**（`SettingsScreen.kt` `GradientTopBar` 区域）:

```kotlin
// 通知角标的 BadgedBox 已被注释
//BadgedBox(badge = { Badge(containerColor = Warning) { Text("3") } }) {
//    IconButton(onClick = {}, modifier = Modifier.size(34.dp)) {
//        Icon(Icons.Default.Notifications, "通知", ...)
//    }
//}

// 原有"更多"按钮保持不变
IconButton(onClick = {}, modifier = Modifier.size(34.dp)) {
    Icon(Icons.Default.MoreVert, "更多", ...)
}
```

---

## 7. 深色模式 — Dark Mode

### 涉及文件

| 文件 | 操作 | 说明 |
|---|---|---|
| `apps/android/app/src/main/java/com/shopping/agent/ui/theme/ThemeState.kt` | 新建 | 全局深色模式状态管理，SharedPreferences 持久化 |
| `apps/android/app/src/main/java/com/shopping/agent/ui/theme/Theme.kt` | 修改 | ShoppingTheme 从 LocalThemeState 读取暗色模式开关 |
| `apps/android/app/src/main/java/com/shopping/agent/MainActivity.kt` | 修改 | 通过 CompositionLocalProvider 提供 ThemeState |
| `apps/android/app/src/main/java/com/shopping/agent/ui/screens/SettingsScreen.kt` | 修改 | 深色模式开关连接全局状态，组件颜色适配暗色主题 |

### 7.1 ThemeState — 全局主题状态管理

新建 `ThemeState.kt`，包含：

- **`ThemeState(context)` 类**: 持有 `MutableState<Boolean>` 类型的 `isDarkMode` 状态，通过 `SharedPreferences` 持久化用户选择（key: `dark_mode_enabled`）
- **`LocalThemeState`**: `staticCompositionLocalOf`，用于在 Compose 组件树中传递 `ThemeState` 实例
- **`rememberThemeState(context)`**: Composable 函数，在 Activity 级别创建并记住 `ThemeState` 实例

### 7.2 ShoppingTheme 改造

`ShoppingTheme` 不再接收 `darkTheme: Boolean` 参数，改为从 `LocalThemeState.current.isDarkMode` 读取当前状态，动态切换 `LightColorScheme` / `DarkColorScheme`。

暗色模式配色：
- `background`: `#1A1A1A` (深灰色)
- `surface`: `#2C2C2C`
- `onBackground` / `onSurface`: `Color.White`
- `onSurfaceVariant`: `Neutral400`
- `primary`: `#7AB8FF` (浅蓝色)

### 7.3 MainActivity 集成

在 `setContent` 中：
1. 调用 `rememberThemeState(this@MainActivity)` 创建全局状态实例
2. 通过 `CompositionLocalProvider(LocalThemeState provides themeState)` 提供给整个组件树
3. `ShoppingTheme` 在 `CompositionLocalProvider` 内部读取状态

### 7.4 SettingsScreen 深色模式开关

原深色模式行使用本地 `remember { mutableStateOf(false) }` 状态，现改为：
- 读取 `LocalThemeState.current` 获取全局状态
- `Switch.checked` 绑定 `themeState.isDarkMode.value`
- `Switch.onCheckedChange` 调用 `themeState.toggleDarkMode(it)` 切换并持久化

### 7.5 SettingsScreen 颜色适配

将 SettingsScreen 中所有硬编码的 Neutral 色值替换为 `MaterialTheme.colorScheme` 动态色值，确保暗色模式下文字和图标可见：

| 组件 | 原色值 | 新色值 |
|---|---|---|
| 页面背景 | `Neutral50` | `MaterialTheme.colorScheme.background` |
| 卡片背景 | `Neutral0` | `MaterialTheme.colorScheme.surface` |
| 卡片分区标题 | `Neutral400` | `MaterialTheme.colorScheme.onSurfaceVariant` |
| 行标题文字 | `Neutral900` | `MaterialTheme.colorScheme.onSurface` |
| 行值文字 | `Neutral400` | `MaterialTheme.colorScheme.onSurfaceVariant` |
| 图标颜色 | `Neutral500` | `MaterialTheme.colorScheme.onSurfaceVariant` |
| 箭头颜色 | `Neutral300` | `MaterialTheme.colorScheme.onSurfaceVariant` |
| 分割线 | `Neutral100` | `MaterialTheme.colorScheme.outlineVariant` |
| 顶部栏图标/文字 | `Neutral700`/`Neutral900` | `MaterialTheme.colorScheme.onSurface` |
| 法律链接 | `Neutral400` | `MaterialTheme.colorScheme.onSurfaceVariant` |

### 7.6 全局页面和组件深色模式适配

将所有 screen 和 component 中对 Neutral/Color 硬编码颜色的引用替换为 `MaterialTheme.colorScheme` 动态色值，确保所有页面在暗色模式下正确显示。

**涉及文件：**

| 文件 | 说明 |
|---|---|
| `ui/theme/Theme.kt` | Crossfade 动画包裹主题切换（400ms 淡入淡出），消除闪烁 |
| `ui/components/MainBottomNavBar.kt` | 底部导航栏背景和图标颜色适配 |
| `ui/components/GradientScreenBackground.kt` | PageBody 背景色适配 |
| `ui/components/ChatInputBar.kt` | 输入栏背景、文字、图标、输入框颜色适配 |
| `ui/components/StreamingBubble.kt` | 流式气泡背景和文字颜色适配 |
| `ui/components/MessageBubble.kt` | 消息气泡背景和文字颜色适配 |
| `ui/components/LoadingStates.kt` | 空状态/错误状态文字颜色适配 |
| `ui/components/ProductCard.kt` | 商品卡片背景、标签、文字颜色适配 |
| `ui/components/ProductCardHorizontal.kt` | 横向商品卡片背景、标签、文字颜色适配 |
| `ui/components/HistoryDrawer.kt` | 历史侧边栏面板、搜索框、会话列表、页脚颜色适配 |
| `ui/screens/HomeScreen.kt` | 主页背景、图标、空状态文字、澄清面板颜色适配 |
| `ui/screens/CompareScreen.kt` | 比价页背景、标签栏、搜索栏、商品网格、价格趋势卡、AI 对比弹窗颜色适配 |
| `ui/screens/ProfileScreen.kt` | 我的页面背景、卡片、图标、文字颜色适配 |
| `ui/screens/ProfileEditScreen.kt` | 个人信息页背景、卡片、弹窗（昵称/性别/年龄段/头像来源）颜色适配 |
| `ui/screens/CartScreen.kt` | 购物车背景、空状态、商品卡片、底部结算栏、弹窗颜色适配 |
| `ui/screens/SettingsScreen.kt` | 设置页底部按钮颜色适配 |
| `ui/screens/ExploreScreen.kt` | 探索页背景、卡片、网格颜色适配 |
| `ui/screens/ExploreProductPostScreen.kt` | 探索详情页背景、作者行、正文颜色适配 |
| `ui/navigation/NavGraph.kt` | Scaffold 容器色跟随主题 |

**颜色映射规则：**

| 原硬编码色彩 | 替换为 | 暗色模式下实际值 |
|---|---|---|
| `Neutral0` (#FFF) | `colorScheme.surface` | `#2C2C2C` |
| `Neutral50` (#F8F9FA) | `colorScheme.background` | `#1A1A1A` |
| `Neutral100` (#F0F0F0) | `colorScheme.outlineVariant` | `#444444` |
| `Neutral200` (#E0E0E0) | `colorScheme.outline` | `#888888` |
| `Neutral300` (#CCC) | `colorScheme.onSurfaceVariant` | `#AAAAAA` |
| `Neutral400` (#AAA) | `colorScheme.onSurfaceVariant` | `#AAAAAA` |
| `Neutral500` (#888) | `colorScheme.onSurfaceVariant` | `#AAAAAA` |
| `Neutral600` (#666) | `colorScheme.onSurfaceVariant` | `#AAAAAA` |
| `Neutral700` (#444) | `colorScheme.onSurface` | `#FFFFFF` |
| `Neutral800` (#222) | `colorScheme.onSurface` | `#FFFFFF` |
| `Neutral900` (#111) | `colorScheme.onSurface` | `#FFFFFF` |
| `Color(0xFFF0F0F3)` | `colorScheme.outlineVariant` | `#444444` |

---

## 8. 侧边栏历史会话增强

### 涉及文件

| 文件 | 操作 | 说明 |
|---|---|---|
| `apps/android/app/src/main/java/com/shopping/agent/ui/components/HistoryDrawer.kt` | 修改 | 搜索过滤 + 底部头像卡片真实数据 |
| `apps/android/app/src/main/java/com/shopping/agent/ui/navigation/NavGraph.kt` | 修改 | 传入 `onProfileClick` 回调，跳转"我的"页面 |

### 8.1 搜索历史会话

**原有代码**: `DrawerTopSection` 中的 `searchQuery` 仅为本地 `remember` 状态，未实际参与会话列表过滤。

**实现**:
- 将 `searchQuery` 状态提升至 `HistoryDrawer` Composable 层级
- 通过 `onSearchQueryChange` 回调向下传递到 `DrawerTopSection`
- `DrawerSessionList` 新增 `searchQuery` 参数，对会话列表进行实时过滤：
  - `searchQuery` 为空时：显示全部会话，按月份分组，保持原有顺序
  - `searchQuery` 非空时：按 `title.contains(searchQuery, ignoreCase = true)` 过滤，匹配的会话保留原有月份分组
- 空结果时显示"没有找到相关会话"提示

### 8.2 底部头像卡片

**原有代码**: `DrawerUserFooter` 硬编码显示首字母 "f" 和用户名 "fujunye"，点击无响应。

**实现**:
- `DrawerUserFooter` 新增 `onProfileClick: () -> Unit` 参数，点击整行跳转"我的"页面
- 从 SQLite 数据库加载真实用户数据：
  - `LaunchedEffect(Unit)` 中通过 `UserRepository.getUserProfile()` 获取昵称
  - 通过 `UserRepository.getUserAvatar()` 获取头像 BLOB
  - 头像降采样到最大 80px 后转为 `ImageBitmap`
- 显示逻辑：
  - 有头像时：`Image(bitmap, ContentScale.Crop, Clip(CircleShape))`
  - 无头像时：显示昵称首字符作为占位符
  - 昵称为空时：显示"拾物用户"作为默认名
- `HistoryDrawer` 函数签名新增 `onProfileClick: () -> Unit = {}` 参数
- `NavGraph.kt` 中传入 `onProfileClick`，关闭侧边栏后以与底部导航栏相同的方式跳转 `profile`：`popUpTo("home") { saveState = true }` + `launchSingleTop = true` + `restoreState = true`，避免破坏回退栈导致底部 Tab 导航异常
- 移除不再使用的 `ChevronRight` 图标导入

---

## 9. 客服页面

### 涉及文件

| 文件 | 操作 | 说明 |
|---|---|---|
| `apps/android/app/src/main/java/com/shopping/agent/data/local/LocalDatabase.kt` | 修改 | Schema v2→v3，新增 `customer_service_messages` 表 |
| `apps/android/app/src/main/java/com/shopping/agent/data/local/UserRepository.kt` | 修改 | 新增客服对话 CRUD 方法 |
| `apps/android/app/src/main/java/com/shopping/agent/ui/screens/CustomerServiceScreen.kt` | 新建 | 拾物客服页面完整 UI |
| `apps/android/app/src/main/java/com/shopping/agent/ui/screens/ProfileScreen.kt` | 修改 | 客服按钮对接导航回调 |
| `apps/android/app/src/main/java/com/shopping/agent/ui/navigation/NavGraph.kt` | 修改 | 新增 `customer_service` 路由 |

### 9.1 数据库升级 v2 → v3

新增 `customer_service_messages` 表，DDL：

```sql
CREATE TABLE customer_service_messages (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id     TEXT NOT NULL,
    role        TEXT NOT NULL,
    content     TEXT DEFAULT '',
    created_at  INTEGER NOT NULL,
    FOREIGN KEY (user_id) REFERENCES user_profile(id)
)
```

- `id`: 对话消息 id，自增主键
- `user_id`: 账号 id，外键关联 `user_profile.id`
- `role`: 消息角色（`user` / `assistant`）
- `content`: 对话文本主体
- `created_at`: 消息创建时间戳

添加索引 `idx_cs_user ON (user_id, created_at)`。

### 9.2 UserRepository 客服方法

| 方法 | 说明 |
|---|---|
| `saveCustomerServiceMessage(role, content)` | 保存一条客服消息 |
| `getCustomerServiceMessages()` | 读取当前用户所有客服消息，按时间升序 |
| `getLastCustomerServiceMessageId()` | 获取最后一条消息 id，用于判断新消息分隔位置 |
| `CustomerServiceMessage` | 客服消息数据类（id, role, content, createdAt） |

### 9.3 CustomerServiceScreen 页面

#### 导航路径

```
ProfileScreen → (点击右上角客服图标) → CustomerServiceScreen
```

#### 9.3.1 顶部栏

- `"<"` 返回按钮（`Icons.AutoMirrored.Filled.ArrowBack`），靠左
- `"拾物客服"` 文本，紧挨返回按钮

#### 9.3.2 Body — 对话区

布局结构（每次进入都是新对话）：

```
[历史时间戳]           ← 取自 DB 首条消息的 createdAt
  系统问候1 (AI左, 不存DB)
  系统问候2 (AI左, 不存DB)
  历史用户消息 (DB, 无逐条时间戳)
  历史AI回复   (DB, 无逐条时间戳)

—— 以下为新消息 ——     ← 仅当有历史时显示

[当前时间戳]           ← sessionStartTime
  系统问候1 (AI左, 不存DB)
  系统问候2 (AI左, 不存DB)
  本轮用户消息 (DB, 无逐条时间戳)
  本轮AI回复   (DB, 无逐条时间戳)

  [猜你想问]
```

- **每条对话只记录一个开始时间戳**，逐条回复不显示也不记录时间
- 系统问候语（两条）不存入数据库，纯 UI 渲染
- 历史消息从 SQLite 加载，按 `created_at ASC` 排序
- 分隔符 `"—— 以下为新消息 ——"` 仅在存在历史消息时显示，用户发消息不会触发其重新出现

#### 9.3.3 "猜你想问"容器

- **标题行**: `"猜你想问"`（加粗靠左）+ `"换一批问题"` 按钮 + 刷新 emoji（点击从 15 题缓存池中轮换 3 题）
- **问题列表**: 3 道问题，每题一行
  - 序号（纯文本，序号 1/2/3 分别用红/橙/蓝三色）
  - 问题文本（靠左）
  - `">"` 箭头（靠右）
- **点击问题**: 直接发送到客服对话框，触发 AI 回复

#### 9.3.4 底部输入栏

- **按钮行**: `"📦 发送订单"` 占位按钮
- **输入行**:
  - 语音按钮（`Icons.Default.Mic`，点击调用 `SpeechRecognizer`）
  - 输入框（`OutlinedTextField`，圆角 50，约占 65-70% 宽度，右侧 wink emoji 占位符，placeholder: `"请输入..."`）
  - 订单 emoji 按钮（占位符）
  - 输入为空时：`"+"` 按钮
  - 输入非空时：`"+"` 变为蓝色发送按钮（`FilledIconButton` + `Send` 图标）

#### 9.3.5 AI 回复

- 用户发送消息后，延迟 800ms 模拟客服回复
- 回复基于关键词匹配 14 组预设问答模板（物流/售后/优惠券/地址/支付/取消/降价/发货/人工/积分/退货/不符/历史订单/会员）
- 无关键词匹配时随机返回一条兜底回复
- 用户和客服消息均持久化到 `customer_service_messages` 表

### 9.4 ProfileScreen 对接

- `ProfileScreen` 新增 `onCustomerServiceClick: () -> Unit` 参数
- `ProfileHeader` 右上角客服图标从 `onClick = {}` 改为 `onClick = onCustomerServiceClick`
- `NavGraph` `profile` composable 传入 `{ navController.navigate("customer_service") }`

---

## 10. 数据库 v4 升级 — 5 张新业务表 + 购物车用户关联

### 涉及文件

| 文件 | 操作 | 说明 |
|---|---|---|
| `apps/android/app/src/main/java/com/shopping/agent/data/local/LocalDatabase.kt` | 修改 | Schema v3→v4，新增 5 张表 + cart_items 添加 user_id |
| `apps/android/app/src/main/java/com/shopping/agent/data/local/UserRepository.kt` | 修改 | 新增全部新表 CRUD 方法 + 实体类 |
| `apps/android/app/src/main/java/com/shopping/agent/data/local/CryptoUtil.kt` | 新建 | AES-256-CBC 密码加密工具 |
| `apps/android/app/src/main/java/com/shopping/agent/ui/screens/ShippingAddressScreen.kt` | 新建 | 收货地址管理页面 |
| `apps/android/app/src/main/java/com/shopping/agent/ui/screens/PaymentSettingsScreen.kt` | 新建 | 支付设置页面 |
| `apps/android/app/src/main/java/com/shopping/agent/ui/screens/CountryRegionScreen.kt` | 新建 | 国家与地区选择页面 |
| `apps/android/app/src/main/java/com/shopping/agent/ui/screens/SettingsScreen.kt` | 修改 | 收货地址/支付设置/国家与地区行对接导航 |
| `apps/android/app/src/main/java/com/shopping/agent/ui/navigation/NavGraph.kt` | 修改 | 新增 3 条路由 |
| `apps/android/app/src/main/java/com/shopping/agent/ui/navigation/Screen.kt` | 修改 | 新增 3 个路由常量 |

### 10.1 数据库升级 v3 → v4

`DATABASE_VERSION` 从 `3` 递增至 `4`。

#### 10.1.1 `user_credentials` — 登录凭证表

```sql
CREATE TABLE user_credentials (
    user_id         TEXT PRIMARY KEY,
    login_method    TEXT NOT NULL,        -- "phone" / "email"
    password        TEXT DEFAULT '',      -- AES-256-CBC 加密存储
    FOREIGN KEY (user_id) REFERENCES user_profile(id)
)
```

- 游客登录时该表无记录
- 退出游客登录时若凭证为空则删除 `user_profile` 记录

#### 10.1.2 `shipping_addresses` — 收货地址表

```sql
CREATE TABLE shipping_addresses (
    user_id         TEXT NOT NULL,
    address_id      INTEGER PRIMARY KEY AUTOINCREMENT,
    is_default      INTEGER DEFAULT 0,    -- 唯一默认地址约束
    phone           TEXT DEFAULT '',
    recipient_name  TEXT DEFAULT '',
    address_detail  TEXT DEFAULT '',
    address_type    TEXT DEFAULT '',       -- "" / "家" / "公司" / "学校"
    FOREIGN KEY (user_id) REFERENCES user_profile(id)
)
CREATE INDEX idx_shipping_user ON shipping_addresses(user_id)
```

- `is_default`: 每个用户仅允许一个默认地址（保存时自动清除其他默认标记）

#### 10.1.3 `payment_settings` — 支付设置表

```sql
CREATE TABLE payment_settings (
    user_id                     TEXT PRIMARY KEY,
    default_payment_method      TEXT DEFAULT '支付宝',  -- "支付宝" / "微信"
    payment_password            TEXT DEFAULT '',         -- AES-256-CBC 加密
    small_amount_password_free   INTEGER DEFAULT 0,      -- 0=关闭 / 1=开启
    small_amount_limit          TEXT DEFAULT '',         -- 免密额度
    FOREIGN KEY (user_id) REFERENCES user_profile(id)
)
```

#### 10.1.4 `country_region` — 国家与地区表

```sql
CREATE TABLE country_region (
    user_id         TEXT PRIMARY KEY,
    country_region  TEXT DEFAULT '中国',
    FOREIGN KEY (user_id) REFERENCES user_profile(id)
)
```

#### 10.1.5 `order_records` — 订单记录表

```sql
CREATE TABLE order_records (
    user_id     TEXT NOT NULL,
    order_id    INTEGER PRIMARY KEY AUTOINCREMENT,
    order_body  TEXT DEFAULT '',
    created_at  INTEGER NOT NULL,
    status      TEXT DEFAULT '待付款',  -- 待付款/待发货/待收货/待评价/已完成/已取消
    FOREIGN KEY (user_id) REFERENCES user_profile(id)
)
CREATE INDEX idx_order_user ON order_records(user_id, created_at DESC)
```

#### 10.1.6 `cart_items` — 新增 user_id 列

购物车表新增 `user_id TEXT DEFAULT ''` 列，关联 `user_profile.id`，支持多用户购物车隔离。`onCreate` 中新表直接包含该列，旧表通过 `ALTER TABLE` 升级。

---

### 10.2 CryptoUtil — 密码加密工具

新建 `CryptoUtil.kt`，提供 AES-256-CBC + PBKDF2 密钥派生加密方案：

| 方法 | 说明 |
|---|---|
| `encrypt(plainText)` | 加密明文 → Base64(salt + iv + ciphertext) |
| `decrypt(encryptedText)` | 解密 Base64 密文 → 明文 |

- 每次加密使用随机 salt 和 IV，相同明文产生不同密文
- PBKDF2WithHmacSHA256 密钥派生，10,000 次迭代
- 加密失败返回空字符串，不会崩溃

---

### 10.3 UserRepository 新增方法

#### 10.3.1 登录凭证

| 方法 | 说明 |
|---|---|
| `saveCredentials(loginMethod, encryptedPassword)` | 保存/更新凭证 |
| `getCredentials()` | 获取凭证 Map（login_method, password），null=游客 |
| `isGuestLogin()` | 判断是否为游客登录 |
| `deleteCredentials()` | 删除凭证并清除 user_profile（游客登出） |

#### 10.3.2 收货地址

| 方法 | 说明 |
|---|---|
| `addShippingAddress(address)` | 新增地址，自动处理默认地址唯一性 |
| `getShippingAddresses()` | 获取所有地址，默认地址排最前 |
| `updateShippingAddress(address)` | 更新地址信息 |
| `setDefaultShippingAddress(addressId)` | 设为默认地址（清除其他默认标记） |
| `deleteShippingAddress(addressId)` | 删除地址 |
| `ShippingAddress` | 数据类（addressId, userId, isDefault, phone, recipientName, addressDetail, addressType） |

#### 10.3.3 支付设置

| 方法 | 说明 |
|---|---|
| `getPaymentSettings()` | 获取支付设置，未设置返回默认值 |
| `savePaymentSettings(settings)` | 保存/更新支付设置 |
| `PaymentSettings` | 数据类（userId, defaultPaymentMethod, paymentPassword, smallAmountPasswordFree, smallAmountLimit） |

#### 10.3.4 国家与地区

| 方法 | 说明 |
|---|---|
| `getCountryRegion()` | 获取当前地区，默认 "中国" |
| `saveCountryRegion(region)` | 保存地区设置 |

#### 10.3.5 订单记录

| 方法 | 说明 |
|---|---|
| `addOrderRecord(orderBody, status)` | 新增订单，返回 orderId |
| `getOrderRecords(statusFilter?, limit)` | 获取订单列表（支持按状态过滤） |
| `updateOrderStatus(orderId, newStatus)` | 更新订单状态 |
| `OrderStatus` | 订单状态常量（待付款/待发货/待收货/待评价/已完成/已取消） |
| `OrderRecord` | 数据类（orderId, userId, orderBody, createdAt, status） |

#### 10.3.6 购物车用户关联扩展

| 方法 | 说明 |
|---|---|
| `getCartItemsForCurrentUser(sessionId)` | 优先按 user_id 查询，游客回退 session_id |
| `saveCartItemForCurrentUser(product, sessionId, quantity)` | 保存时同时写入 user_id |

---

### 10.4 ShippingAddressScreen — 收货地址管理

新建 `ShippingAddressScreen` Composable，包含以下模块：

#### 页面结构

- **顶部栏**: 返回按钮 + "收货地址" 标题 + "新增" 按钮
- **空状态**: 定位图标 + "暂无收货地址" + "添加新地址" 按钮
- **地址列表**: `LazyColumn`，默认地址置顶，每张卡片包含：
  - 收货人姓名（加粗）+ 联系电话
  - 默认地址标签（蓝色 `FilterChip`）
  - 地址类型标签（家=橙色 / 公司=蓝色 / 学校=绿色）
  - 详细地址
  - 操作按钮行：设为默认 / 编辑 / 删除

#### 新增/编辑弹窗

`ModalBottomSheet` 表单：
- 收货人姓名 `OutlinedTextField`
- 联系电话 `OutlinedTextField`（数字键盘）
- 详细地址 `OutlinedTextField`
- 地址类型：FilterChip 选择（无/家/公司/学校）
- 设为默认地址：Checkbox
- 保存按钮：必填项为空时禁用

#### 数据同步

所有 CRUD 操作通过 `UserRepository` 直接写入 SQLite，关闭弹窗后自动刷新列表。

---

### 10.5 PaymentSettingsScreen — 支付设置

新建 `PaymentSettingsScreen` Composable，包含以下模块：

#### 页面结构

三个 `Card` 分组：

1. **支付方式**: RadioButton 选择（支付宝 / 微信支付），点击即保存
2. **安全设置**: "支付密码" 行，显示已设置/未设置状态，点击弹出密码设置弹窗
3. **小额免密支付**: 
   - Switch 开关，开启后显示免密额度输入框
   - 额度输入框（数字键盘）+ "保存额度" 按钮

#### 密码设置弹窗

`ModalBottomSheet`：
- 修改密码模式：需输入当前密码 + 新密码 + 确认密码
- 首次设置模式：仅需新密码 + 确认密码
- 密码使用 `PasswordVisualTransformation` 隐藏显示
- 校验：长度 ≥ 6 位数字、两次输入一致
- 保存前通过 `CryptoUtil.encrypt` 加密

---

### 10.6 CountryRegionScreen — 国家与地区

新建 `CountryRegionScreen` Composable：

- **顶部栏**: 返回按钮 + "国家与地区" 标题
- **列表**: 30 个常用国家/地区（含区号参考），每行包含：
  - `RadioButton`（选中项高亮）
  - 地区名称
  - 国际区号（灰色小字）
  - 右箭头
- **交互**: 点击即选中并同步到 SQLite，Toast 提示 "地区已更新为：XX"

---

### 10.7 导航对接

#### 新路由

| Route | Screen | 入口 |
|---|---|---|
| `shipping_address` | `ShippingAddressScreen` | 设置页 → 收货地址 |
| `payment_settings` | `PaymentSettingsScreen` | 设置页 → 支付设置 |
| `country_region` | `CountryRegionScreen` | 设置页 → 国家与地区 |

#### SettingsScreen 变更

- 函数签名新增 `onNavigateToShippingAddress`、`onNavigateToPaymentSettings`、`onNavigateToCountryRegion` 三个回调参数
- "收货地址" 行 `onClick` → `onNavigateToShippingAddress`
- "支付设置" 行 `onClick` → `onNavigateToPaymentSettings`
- "国家与地区" 行 `onClick` → `onNavigateToCountryRegion`

#### NavGraph 变更

- `settings` 路由传入三个新导航回调
- 新增 3 条 `composable` 路由，均使用 `navController.popBackStack()` 作为返回

---

### 10.8 技术要点

- **密码加密**: AES-256-CBC + PBKDF2WithHmacSHA256（10,000 迭代），随机 salt + IV，Base64 编码存储
- **默认地址唯一性**: 保存/更新时先执行 `UPDATE SET is_default=0 WHERE user_id=? AND is_default=1`，再写入新记录
- **购物车用户关联**: 优先按 `user_id` 查询，游客回退 `session_id`；写入时同时记录 `user_id`
- **暗色模式**: 全部新页面使用 `MaterialTheme.colorScheme` 动态色值，自动适配深色主题
- **Compose UI**: 全部使用 Jetpack Compose + Material 3 + `ModalBottomSheet`
- **数据库**: 原生 Android SQLite（SQLiteOpenHelper），协程 `Dispatchers.IO` 异步读写
- **游客模式**: `user_credentials` 表为空时视为游客，登出时清除 `user_profile` 记录

---

## 11. 数据库 v5 升级 — 游客标记 + 登录注册体系

### 涉及文件

| 文件 | 操作 | 说明 |
|---|---|---|
| `apps/android/app/src/main/java/com/shopping/agent/data/local/LocalDatabase.kt` | 修改 | Schema v4→v5，`user_profile` 新增 `is_guest` 列 |
| `apps/android/app/src/main/java/com/shopping/agent/data/local/UserRepository.kt` | 修改 | 新增 `isGuestUser()`、`markAsLoggedIn()`、`createUserProfile()` 方法 |
| `apps/android/app/src/main/java/com/shopping/agent/ui/screens/LoginScreen.kt` | 新建 | 登录页面（手机号/邮箱/游客） |
| `apps/android/app/src/main/java/com/shopping/agent/ui/screens/RegisterScreen.kt` | 新建 | 注册页面（手机号/邮箱+验证码） |
| `apps/android/app/src/main/java/com/shopping/agent/ui/screens/ForgotPasswordScreen.kt` | 新建 | 忘记密码页面 |
| `apps/android/app/src/main/java/com/shopping/agent/ui/screens/PasswordResetScreen.kt` | 新建 | 密码重置页面 |
| `apps/android/app/src/main/java/com/shopping/agent/ui/screens/SettingsScreen.kt` | 修改 | 新增 `onSwitchAccount`、`onLogout` 回调 |
| `apps/android/app/src/main/java/com/shopping/agent/ui/navigation/NavGraph.kt` | 修改 | 新增 4 条认证路由 + 游客守卫逻辑 |
| `apps/android/app/src/main/java/com/shopping/agent/ui/navigation/Screen.kt` | 修改 | 新增 4 个认证路由常量 |

### 11.1 数据库升级 v4 → v5

`DATABASE_VERSION` 从 `4` 递增至 `5`。

`user_profile` 表新增 `is_guest INTEGER DEFAULT 1` 列：
- `1` = 游客登录（默认值）
- `0` = 非游客登录（已通过手机号/邮箱登录）

升级迁移（v4→v5）: `ALTER TABLE user_profile ADD COLUMN is_guest INTEGER DEFAULT 1`

### 11.2 游客守卫机制

#### 页面访问控制

设置页中"收货地址"、"支付设置"、"国家与地区"三个入口添加了游客守卫（`guardedNavigate`）：

```
点击入口 → isGuestUser()? → YES → Toast "请先登录" → 跳转登录页
                          → NO  → 正常跳转目标页面
```

#### 数据库操作控制

- 收货地址、支付设置、国家与地区、订单记录等表的 CRUD 操作通过 `getUserId()` 获取用户 ID
- 游客登出时 `deleteCredentials()` 将用户标记回游客状态（`is_guest=1`）
- 登录/注册成功后 `markAsLoggedIn()` 将用户标记为非游客（`is_guest=0`）

### 11.3 UserRepository 新增方法（v5）

| 方法 | 说明 |
|---|---|
| `isGuestUser()` | 检查当前用户是否为游客（`is_guest=1`） |
| `markAsLoggedIn()` | 标记当前用户为非游客登录 |
| `createUserProfile()` | 创建新用户画像（删除旧游客画像，新建 is_guest=0） |
| `deleteCredentials()` | 更新：清除凭证后标记为游客状态（`is_guest=1`） |

### 11.4 LoginScreen — 登录页面

新建 `LoginScreen` Composable：

#### 页面结构

- **顶部栏**: 返回按钮 + "登录" 标题
- **Tab 切换**: 手机号登录 / 邮箱登录
- **账号输入**: `OutlinedTextField`，图标随 Tab 切换（Phone/Email）
- **密码输入**: 支持可见性切换（`Visibility`/`VisibilityOff` 按钮）
- **验证码登录**: 点击"使用验证码登录"切换模式
  - 获取验证码按钮（60 秒倒计时）
  - 验证码模拟：生成 6 位随机数，Toast 显示（生产环境应通过 SMS/邮件发送）
- **登录按钮**: 密码模式校验密码 → 验证码模式校验验证码 → 标记为非游客 → 返回上一页
- **辅助链接**: 注册账号 / 忘记密码
- **分割线 "或"**
- **游客登录按钮**: 仅在非游客状态下显示，清除凭证后进入主页面
  - 从"切换账号"进入 → 用户非游客 → 显示游客登录入口
  - 从游客守卫进入（如访问地址页）→ 用户已是游客 → 隐藏游客登录，强制登录/注册

### 11.5 RegisterScreen — 注册页面

新建 `RegisterScreen` Composable：

- **Tab 切换**: 手机号注册 / 邮箱注册
- **验证码获取**: 60 秒倒计时，模拟 6 位验证码
- **密码 + 确认密码**: 均支持可见性切换，实时校验一致性
- **注册按钮**: 验证码校验 → 密码校验（≥6 位、两次一致）→ 创建新用户画像 → 保存凭证 → 返回登录页
- 注册成功后 `createUserProfile()` 会删除旧游客画像并创建新非游客画像

### 11.6 ForgotPasswordScreen — 忘记密码

新建 `ForgotPasswordScreen` Composable：

- **Tab 切换**: 手机号找回 / 邮箱找回
- **账号输入**: 与 Tab 类型联动
- **验证码**: 60 秒倒计时，获取前检查账号是否已注册
- **下一步按钮**: 验证码校验通过后跳转密码重置页面（携带账号参数）

### 11.7 PasswordResetScreen — 密码重置

新建 `PasswordResetScreen` Composable：

- **接收参数**: 从忘记密码页传入的 `account`
- **新密码 + 确认密码**: 均支持可见性切换
- **校验规则**:
  - 密码长度 ≥ 6 位
  - 两次输入一致（实时显示错误提示）
  - 新密码不能与旧密码相同（通过 `CryptoUtil.decrypt` 对比）
- **重置按钮**: 加密新密码 → 更新 `user_credentials` → 标记为非游客 → 跳转登录页

### 11.8 导航对接

#### 新路由

| Route | Screen | 参数 | 入口 |
|---|---|---|---|
| `login` | `LoginScreen` | - | 设置页→切换账号/游客守卫 |
| `register` | `RegisterScreen` | - | 登录页→注册账号 |
| `forgot_password` | `ForgotPasswordScreen` | - | 登录页→忘记密码 |
| `password_reset/{account}` | `PasswordResetScreen` | `account: String` | 忘记密码→下一步 |

#### SettingsScreen 变更

- 函数签名新增 `onSwitchAccount`、`onLogout` 两个回调参数
- "切换账号" 按钮 → `onSwitchAccount`（清除凭证 + 跳转登录页）
- "退出登录" 按钮 → `onLogout`（清除凭证 + 回到首页）

#### 完整认证流程

```
SettingsScreen
  ├─ 切换账号 → LoginScreen
  ├─ 退出登录 → (logout) → HomePage
  ├─ 收货地址 → [游客守卫] → 游客?→LoginScreen : →ShippingAddress
  ├─ 支付设置 → [游客守卫] → 游客?→LoginScreen : →PaymentSettings
  └─ 国家与地区 → [游客守卫] → 游客?→LoginScreen : →CountryRegion

LoginScreen
  ├─ 登录成功 → popBackStack
  ├─ 注册账号 → RegisterScreen → 注册成功 → popBackStack → LoginScreen
  ├─ 忘记密码 → ForgotPasswordScreen
  │                └─ 下一步 → PasswordResetScreen(account)
  │                                └─ 重置成功 → LoginScreen
  └─ 游客登录 → popBackStack
```

### 11.9 技术要点

- **验证码模拟**: 生成 6 位随机数字，通过 Toast 显示；生产环境应接入短信/邮件服务
- **密码可见性**: 使用 `PasswordVisualTransformation` / `VisualTransformation.None` 切换（按钮点击方式，非长按）
- **倒计时**: `LaunchedEffect` + 每秒递减，按钮倒计时期间禁用
- **游客守卫**: 在 NavGraph 层通过 `guardedNavigate()` 拦截导航，检查 `isGuestUser()` 后决定跳转
- **游客登录按钮智能显隐**: LoginScreen 内部调用 `isGuestUser()` 判断当前状态，已是游客时隐藏"游客登录"按钮，避免"游客→强制登录→再选游客"的死循环
- **注册画像切换**: `createUserProfile()` 先删除旧游客画像再创建新用户，避免 `getUserId()` 返回无效 ID
- **所有新页面遵循**: 暗色模式自适应、GradientTopBar 统一顶栏、Material 3 组件规范

---

## 12. 数据库 v6 升级 — 登录状态持久化 + 启动路由

### 涉及文件

| 文件 | 操作 | 说明 |
|---|---|---|
| `apps/android/app/src/main/java/com/shopping/agent/data/local/LocalDatabase.kt` | 修改 | Schema v5→v6，新增 `login_state` 表 |
| `apps/android/app/src/main/java/com/shopping/agent/data/local/UserRepository.kt` | 修改 | 新增 `LoginState` 数据类 + 登录状态 CRUD |
| `apps/android/app/src/main/java/com/shopping/agent/MainActivity.kt` | 修改 | 启动时检查登录状态，动态决定 `startDestination` |
| `apps/android/app/src/main/java/com/shopping/agent/ui/navigation/NavGraph.kt` | 修改 | `AppNavGraph` 新增 `startDestination` 参数；登录成功/游客登录/退出登录导航逻辑 |
| `apps/android/app/src/main/java/com/shopping/agent/ui/screens/LoginScreen.kt` | 修改 | 登录/游客成功后调用 `saveLoginState()` |

### 12.1 数据库升级 v5 → v6

`DATABASE_VERSION` 从 `5` 递增至 `6`。

#### `login_state` — 登录状态表

```sql
CREATE TABLE login_state (
    user_id      TEXT PRIMARY KEY,
    login_status INTEGER DEFAULT 0,     -- 0=未登录, 1=已登录
    login_type   TEXT DEFAULT '',        -- "" / "guest" / "non_guest"
    FOREIGN KEY (user_id) REFERENCES user_profile(id)
)
```

- `login_status`: `0` = 未登录（需展示登录页），`1` = 已登录（可跳过登录页）
- `login_type`: `"guest"` = 游客确认登录，`"non_guest"` = 账号密码登录

### 12.2 UserRepository 新增方法（v6）

| 方法 | 说明 |
|---|---|
| `saveLoginState(loginType)` | 保存登录状态（loginType: "guest"/"non_guest"） |
| `getLoginState()` | 获取登录状态 `LoginState` 对象 |
| `isLoggedIn()` | 快捷判断：是否已完成登录（含游客确认登录） |
| `clearLoginState()` | 清除登录状态 |
| `LoginState` | 数据类（userId, loginStatus, loginType） |
| `deleteCredentials()` | 更新：同时清除登录状态表和凭证 |

### 12.3 启动路由逻辑

#### MainActivity 变更

启动流程：开屏 1.5s → 检查 `isLoggedIn()` → 决定 `startDestination`：

| `isLoggedIn()` 结果 | `startDestination` | 效果 |
|---|---|---|
| `true`（已登录/已确认游客） | `"home"` | 跳过登录页，直接进入主页 |
| `false`（首次启动/上次退出了登录） | `"login"` | 展示登录页，用户需登录或注册 |

#### NavGraph 变更

- `AppNavGraph` 新增 `startDestination: String` 参数
- `NavHost` 的 `startDestination` 使用参数值替代硬编码 `"home"`
- 登录成功导航：`navigate("home") { popUpTo("login") { inclusive = true } }` — 清除登录页避免回退
- 游客登录导航：同上，确保游客确认后不可回退到登录页
- 退出登录导航：清除凭证+登录状态 → `navigate("login") { popUpTo("home") { inclusive = true } }` — 强制重新登录

### 12.4 登录状态生命周期

```
首次安装启动
  └─ login_state 表为空 → isLoggedIn()=false → 展示 LoginScreen
       ├─ 用户登录 → saveLoginState("non_guest") → 跳转 home
       ├─ 游客登录 → saveLoginState("guest") → 跳转 home
       └─ 用户注册 → 回到登录页 → 用户登录 → saveLoginState("non_guest")

后续启动
  └─ login_state.login_status=1 → isLoggedIn()=true → 直接进 home

退出登录 / 切换账号
  └─ deleteCredentials() → 清除凭证 + clearLoginState → 跳转 login
       └─ 下次启动 isLoggedIn()=false → 展示 LoginScreen
```

### 12.5 技术要点

- **启动性能**: 登录状态检查在 `LaunchedEffect(Unit)` 中异步执行，不阻塞主线程；加载期间展示白屏占位
- **返回栈管理**: 登录完成后 `popUpTo("login") { inclusive = true }` 确保登录页不在返回栈中，用户无法回退到登录页
- **退出登录**: 清除凭证 + 登录状态，跳转登录页并清除整个返回栈（`popUpTo("home") { inclusive = true }`）
- **首次启动保护**: `onBack` 检查 `previousBackStackEntry != null`，若登录页为启动目标则禁用返回键
