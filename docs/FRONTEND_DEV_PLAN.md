# Android 前端开发规划 — 7 页面 UI 差距分析与实施计划

> **⚠️ 历史文档** — 本文档为 2026-05-24 规划阶段的快照，所有 Phase 0-5 任务已完成。
> 当前实际状态见 `README.md`（36+ kt 文件，9/9 场景全栈就绪）和 `docs/progress/开发进度控制表.md`。
>
> 生成日期: 2026-05-24 | 最后更新: 2026-06-07
> 项目: RAG多模态电商AI导购 Agent (04-rag-ecommerce)
> 参赛者: fujunye (大学生)
> 目标: 10 天内完成 7 个页面的可演示 UI — ✅ 已达成

---

## 一、现有代码清单 (33 个 .kt 文件)

### 1.1 已实现的文件（可复用）

| 文件 | 行数 | 状态 | 说明 |
|------|:---:|:----:|------|
| `data/model/ChatMessage.kt` | 12 | ✅ 完成 | role/content/products/timestamp/isStreaming |
| `data/model/Product.kt` | 20 | ✅ 完成 | id/name/price/imageUrl/highlights/matchScore/rating/brand/category |
| `data/model/SSEEvent.kt` | 50 | ✅ 完成 | TextDelta/ProductCard/Done/Error/Unknown + fromJson 解析 |
| `data/model/Feedback.kt` | 11 | ✅ 完成 | sessionId/rating/productId/reason |
| `data/remote/SseClient.kt` | 81 | ✅ 完成 | OkHttp SSE + Flow + 自动重连, baseUrl=10.0.2.2:8000 |
| `data/remote/ApiClient.kt` | 16 | ✅ 完成 | OkHttp 客户端配置 |
| `data/remote/ApiService.kt` | 19 | ⚠️ 接口 | 定义了 getProducts/getProductDetail/submitFeedback 签名, 无实现 |
| `data/repository/ChatRepository.kt` | 36 | ✅ 完成 | SSE 消息发送 + 本地缓存 |
| `data/repository/ProductRepository.kt` | 15 | ⚠️ 空壳 | 只有内存 map, 无 API 调用 |
| `data/repository/FeedbackRepository.kt` | 12 | ⚠️ TODO | submit() 空函数 |
| `viewmodel/ChatViewModel.kt` | 107 | ✅ 完成 | StateFlow 消息流, SSE 流式更新, 错误处理 |
| `viewmodel/ProductViewModel.kt` | 18 | ⚠️ 空壳 | StateFlow 声明, 无加载/筛选逻辑 |
| `viewmodel/CompareViewModel.kt` | 11 | ❌ 占位 | mutableListOf (非响应式), 无功能 |
| `ui/chat/ChatScreen.kt` | 185 | ✅ 完成 | 消息列表 + 输入栏 + SSE 流式 + MessageBubble 内联 |
| `ui/components/ProductCard.kt` | 111 | ✅ 完成 | 竖版卡片: 图片/名称/价格/评分/匹配度/推荐理由 |
| `ui/components/LoadingStates.kt` | 38 | ✅ 完成 | LoadingState/EmptyState/ErrorState 三个通用组件 |
| `ui/components/FeedbackWidget.kt` | 25 | ✅ 完成 | 点赞/点踩按钮(emoji) |
| `ui/components/MessageBubble.kt` | 35 | ⚠️ 冗余 | 旧版独立组件, ChatScreen 已内联, 可废弃 |
| `ui/components/CompareTable.kt` | 17 | ❌ 占位 | 空 TODO 函数体 |
| `ui/components/ImagePicker.kt` | 14 | ❌ 占位 | 空 TODO 函数体 |
| `ui/screens/HomeScreen.kt` | 24 | ❌ 占位 | 仅欢迎文字 |
| `ui/screens/CompareScreen.kt` | 22 | ❌ 占位 | 仅标题+"全量阶段开发" |
| `ui/screens/HistoryScreen.kt` | 22 | ❌ 占位 | 仅标题+"全量阶段开发" |
| `ui/screens/ProductDetailScreen.kt` | 23 | ❌ 占位 | 仅标题+productId 文本 |
| `ui/navigation/NavGraph.kt` | 12 | ⚠️ 半成品 | 只有 sealed class 路由定义, 无 NavHost |
| `ui/theme/Color.kt` | 7 | ❌ 不足 | 仅 3 色(Primary=蓝/Secondary=绿/Background=灰) |
| `ui/theme/Theme.kt` | 9 | ❌ 不足 | 裸 MaterialTheme 包装, 无品牌主题 |
| `ui/theme/Type.kt` | 13 | ⚠️ 基础 | 4 种字号, 需扩展 |
| `MainActivity.kt` | 29 | ❌ 关键缺陷 | TODO 注释, 未挂载 NavGraph, APP 启动白屏 |
| `ShoppingApp.kt` | 13 | ⚠️ 预留 | onCreate 空, Hilt TODO |
| `build.gradle.kts` | 65 | ✅ 完整 | Compose BOM/OkHttp/Coil/Navigation/Coroutines 已配齐 |

### 1.2 测试文件 (3 个)

| 文件 | 状态 |
|------|:----:|
| `test/.../ProductDtoTest.kt` | ✅ |
| `test/.../SseEventTest.kt` | ✅ |
| `test/.../ChatViewModelTest.kt` | ✅ |

---

## 二、7 页面差距矩阵

### P01 — 首页推荐页

| UI 要素 | 说明书要求 | 当前状态 | 差距 |
|---------|----------|:--------:|------|
| 蓝粉渐变背景 | 蓝→粉渐变全屏背景 | ❌ 未实现 | Color.kt 无粉色, Theme.kt 无渐变 |
| 2×2 商品推荐卡片 | 4 张推荐商品卡片网格 | ❌ 未实现 | HomeScreen 只有欢迎文字 |
| 搜索栏 | 顶部搜索入口 | ❌ 未实现 | 无搜索组件 |
| 快捷操作区 | 底部快捷入口按钮 | ❌ 未实现 | 无 |
| 4 Tab 底部导航 | 首页/比价/探索/我的 | ❌ 未实现 | NavGraph 未挂载 |
| **整体评级** | | **❌ 未实现** | 整个页面需从零构建 |

### P02 — 运动商品列表页

| UI 要素 | 说明书要求 | 当前状态 | 差距 |
|---------|----------|:--------:|------|
| 搜索栏 | 顶部搜索 | ❌ 未实现 | 无文件 |
| 分类 Tab | 运动类目切换 (跑步/篮球/健身等) | ❌ 未实现 | 无文件 |
| 2 列商品网格 | 商品缩略图+名称+价格 | ❌ 未实现 | 无文件 |
| **整体评级** | | **❌ 未实现** | 文件完全不存在 |

> 注: 此页可能是首页点击推荐后的跳转页, 非底部 Tab。确认后决定是否独立建文件。

### P03 — 比价页

| UI 要素 | 说明书要求 | 当前状态 | 差距 |
|---------|----------|:--------:|------|
| 商品主卡 | 选中商品的大卡片展示 | ❌ 未实现 | CompareScreen 占位 |
| 价格趋势卡片列表 | 多平台价格对比卡片 | ❌ 未实现 | CompareTable TODO |
| Canvas 价格曲线 | 历史价格走势图 | ❌ 未实现 | 无 Canvas 组件 |
| ViewModel 响应式 | StateFlow 状态管理 | ❌ 未实现 | CompareViewModel 用 mutableListOf 非响应式 |
| **整体评级** | | **❌ 占位空壳** | 页面/组件/VM 全是 TODO |

### P04 — 探索页

| UI 要素 | 说明书要求 | 当前状态 | 差距 |
|---------|----------|:--------:|------|
| 商品地图 | 地图背景+商品标记 | ❌ 未实现 | 无文件 |
| 3 列错位网格 | 瀑布流布局 | ❌ 未实现 | 无文件 |
| 分类胶囊 | 可滚动分类标签 | ❌ 未实现 | 无文件 |
| **整体评级** | | **❌ 未实现** | 文件完全不存在 |

### P05 — 我的页

| UI 要素 | 说明书要求 | 当前状态 | 差距 |
|---------|----------|:--------:|------|
| 用户名 fujunye | 顶部用户信息 | ❌ 未实现 | 无文件 |
| 购物车预览 | 购物车商品缩略 | ❌ 未实现 | 无文件 |
| 订单入口 | 订单列表入口 | ❌ 未实现 | 无文件 |
| 领券中心 | 优惠券入口 | ❌ 未实现 | 无文件 |
| **整体评级** | | **❌ 未实现** | 文件完全不存在 |

### P06 — 历史侧边栏

| UI 要素 | 说明书要求 | 当前状态 | 差距 |
|---------|----------|:--------:|------|
| 左侧滑出抽屉 | DrawerLayout 左侧滑出 | ❌ 未实现 | HistoryScreen 占位 |
| 月份分组 | 按月份折叠分组 | ❌ 未实现 | 无 |
| 搜索 | 历史记录搜索 | ❌ 未实现 | 无 |
| **整体评级** | | **❌ 占位空壳** | 只有标题文字 |

### P07 — 对话推荐页

| UI 要素 | 说明书要求 | 当前状态 | 差距 |
|---------|----------|:--------:|------|
| 用户气泡 | 右对齐用户消息 | ✅ 已实现 | ChatScreen.MessageBubble |
| AI 推荐气泡 | 左对齐 AI 回复 | ✅ 已实现 | ChatScreen.MessageBubble + 流式光标 |
| 横向商品卡 | 横向滚动商品推荐 | ⚠️ 部分 | 当前是竖版 ProductCard 纵向堆叠 |
| 输入栏 | 底部输入框+发送按钮 | ✅ 已实现 | OutlinedTextField + Button |
| SSE 流式 | 实时流式文字 | ✅ 已实现 | SseClient → ChatViewModel → UI |
| Mock 数据回退 | 无后端时显示假数据 | ❌ 未实现 | 依赖后端 SSE 连接 |
| **整体评级** | | **⚠️ 80% 完成** | 核心流程跑通, 缺横向卡 + Mock |

---

## 三、差距汇总统计

```
页面    状态            完成度
─────────────────────────────
P07 对话 ✅ 核心完成      80%
P01 首页  ❌ 未实现        5%
P02 列表  ❌ 未实现        0%
P03 比价  ❌ 占位空壳      5%
P04 探索  ❌ 未实现        0%
P05 我的  ❌ 未实现        0%
P06 历史  ❌ 占位空壳      5%

全局缺陷:
- MainActivity 未挂载 NavHost → 启动白屏
- 无底部导航栏
- 无双色渐变主题
- 无 Mock 数据层
- 无搜索栏复用组件
```

---

## 四、优先开发顺序（比赛优先级排序）

比赛权重: 对话推荐(40%) > 首页(20%) > 比价(15%) > 我的(10%) > 探索(10%) > 历史(5%)

```
Phase 0: 基础设施 (先修, 所有页面依赖)  — 第 1 天
Phase 1: P07 对话完善                   — 第 2-3 天
Phase 2: P01 首页 + 导航框架             — 第 4-5 天
Phase 3: P03 比价                       — 第 6 天
Phase 4: P05 我的 + P04 探索             — 第 7-8 天
Phase 5: P06 历史 + P02 列表 + 收尾     — 第 9-10 天
```

---

## 五、详细文件清单

### Phase 0: 基础设施 (Day 1, ~6h)

```
新建文件:
  core/common/MockData.kt                 — Mock 商品数据(20条+), 用户数据, 历史数据
  ui/theme/Color.kt                       — 重写: 蓝粉渐变色板 (Blue400→Pink300)
  ui/theme/Theme.kt                       — 重写: GradientBackground + 圆角卡片主题
  ui/theme/Type.kt                        — 扩展: 价格字号(红色粗体)
  ui/navigation/NavGraph.kt               — 重写: NavHost + bottomBar + 路由跳转
  ui/components/SearchBar.kt              — 可复用搜索栏组件
  ui/components/GradientBackground.kt     — 蓝粉渐变背景组件
  ui/components/CategoryTabs.kt           — 分类 Tab 组件

修改文件:
  MainActivity.kt                         — 挂载 NavGraph (关键修复!)
  build.gradle.kts                        — 如需 Canvas 图表库或地图库, 此处添加依赖
  AndroidManifest.xml                     — 如需地图权限或拍照权限
```

### Phase 1: P07 对话完善 (Day 2-3, ~8h)

```
新建文件:
  ui/components/HorizontalProductCard.kt  — 横向滚动商品卡片 (用于 AI 推荐)
  ui/components/QuickReplyChips.kt        — 快捷回复/引导问题胶囊

修改文件:
  ui/chat/ChatScreen.kt                   — 集成 HorizontalProductCard, 加入欢迎引导
  viewmodel/ChatViewModel.kt              — 添加 Mock 模式 fallback (后端不可用时)
  data/repository/ChatRepository.kt       — 添加 MockSseClient (返回假 SSE 事件)
```

### Phase 2: P01 首页 + 导航框架 (Day 4-5, ~8h)

```
新建文件:
  ui/screens/HomeScreen.kt                — 重写: 蓝粉渐变 + 2x2 推荐网格 + 搜索 + 快捷操作
  ui/components/RecommendationGrid.kt     — 2x2 商品推荐卡片网格
  ui/components/QuickActions.kt           — 底部快捷操作按钮组
  viewmodel/HomeViewModel.kt              — 首页数据加载 + Mock 商品

修改文件:
  ui/navigation/NavGraph.kt               — 加入底部导航 4 Tab (首页/比价/探索/我的)
  MainActivity.kt                         — 确认 NavHost 挂载
```

### Phase 3: P03 比价 (Day 6, ~5h)

```
新建文件:
  ui/screens/CompareScreen.kt             — 重写: 商品主卡 + 价格趋势列表 + Canvas 曲线
  ui/components/PriceTrendCard.kt         — 单平台价格卡片
  ui/components/PriceChart.kt             — Canvas 绘制价格趋势曲线
  viewmodel/CompareViewModel.kt           — 重写: StateFlow 响应式 + Mock 比价数据

修改文件:
  ui/components/CompareTable.kt           — 实现多列对比表 (或废弃, 改用卡片列表)
```

### Phase 4: P05 我的 + P04 探索 (Day 7-8, ~7h)

```
新建文件:
  ui/screens/ProfileScreen.kt             — P05 我的: 用户信息 fujunye + 购物车 + 订单 + 领券
  ui/screens/ExploreScreen.kt             — P04 探索: 3 列错位网格 + 分类胶囊
  ui/components/StaggeredGrid.kt          — 瀑布流错位网格布局
  viewmodel/ProfileViewModel.kt           — 用户数据 + Mock
  viewmodel/ExploreViewModel.kt           — 探索数据 + Mock

修改文件: (无, 主要是新建)
```

### Phase 5: 历史 + 列表 + 收尾 (Day 9-10, ~6h)

```
新建文件:
  ui/screens/HistoryScreen.kt             — 重写: 左侧抽屉 + 月份分组 + 搜索
  ui/components/HistoryDrawer.kt          — 侧边栏抽屉组件
  ui/components/HistoryMonthGroup.kt      — 月份折叠分组
  ui/screens/SportListScreen.kt           — P02 运动商品列表: 搜索 + 分类Tab + 2列网格
  viewmodel/HistoryViewModel.kt           — 历史数据 + Mock

修改文件:
  ui/navigation/NavGraph.kt               — 注册 P02/历史路由
  ui/components/CompareTable.kt           — 最终实现或删除
  ui/components/ImagePicker.kt            — 最终实现或删除 (非 MVP)
```

---

## 六、工时估算 (大学生非全职)

```
阶段         内容                  工时    日历天数
──────────────────────────────────────────────────
Phase 0      基础设施修复           6h     1 天
Phase 1      P07 对话完善           8h     2 天
Phase 2      P01 首页+导航          8h     2 天
Phase 3      P03 比价               5h     1 天
Phase 4      P05+P04 我的+探索      7h     2 天
Phase 5      P06+P02+收尾          6h     2 天
──────────────────────────────────────────────────
合计                                40h    10 天

按每天 3-5h 可用时间 (课业间隙) 估算, 预留 buffer。
```

### 风险与降级策略

| 风险 | 概率 | 降级方案 |
|------|:---:|---------|
| 时间不够 | 中 | P04 探索页降级为 2 列网格 (非错位), P02 列表内嵌到首页 |
| Canvas 曲线难实现 | 中 | 用 Compose Canvas 画简单折线, 不做贝塞尔平滑 |
| 地图集成复杂 | 高 | P04 地图改为静态图片背景 + 商品标记点 (不集成 Map SDK) |
| 横向商品卡交互 | 低 | 用 LazyRow 即可, 无技术风险 |

---

## 七、复用策略

```
✅ 原样复用 (零改动):
  - ProductCard.kt            → 首页/探索/比价/我的 都直接用
  - LoadingStates.kt          → 所有页面通用
  - FeedbackWidget.kt         → 对话页保留
  - ChatViewModel.kt          → 对话页核心逻辑不变
  - SseClient.kt              → SSE 连接不变
  - SSEEvent.kt               → 事件解析不变
  - ChatMessage.kt            → 消息 DTO 不变
  - Product.kt                → 商品 DTO 不变

🔄 修改复用:
  - ChatScreen.kt             → 加横向卡 + Mock 切换
  - NavGraph.kt               → 加 NavHost + BottomBar
  - Theme/Color/Type           → 全面重写
  - MainActivity.kt           → 挂 NavGraph

🗑️ 废弃:
  - MessageBubble.kt          → ChatScreen 已内联, 删除
  - CompareTable.kt           → 若改用卡片列表可删除
  - ImagePicker.kt            → MVP 不实现, 可保留 TODO
```

---

## 八、架构补齐（四层结构）

当前缺少 `core/` 和 `domain/` 层, 需补齐:

```
core/
  common/
    Constants.kt            — App 常量 (baseUrl, 用户名等)
    MockData.kt             — 所有 Mock 数据
  di/                        — 依赖注入 (暂用手动, Hilt 预留)

domain/
  model/                     — (当前 data/model 已是 domain model, 可不动)
  usecase/                   — 可选, MVP 阶段跳过
```

---

## 九、关键里程碑检查点

```
Day 1 结束: APP 启动不再白屏, 能看到导航框架
Day 3 结束: 对话页可展示 Mock 对话 + 横向商品卡
Day 5 结束: 首页展示 4 张推荐卡片 + 底部导航可切换
Day 6 结束: 比价页展示 Mock 价格曲线
Day 8 结束: 我的页 + 探索页完成
Day 10 结束: 所有 7 页面可演示, 交付出包
```

---

## 十、待确认事项

1. P02 运动商品列表页是独立 Tab 还是首页内嵌子页面? (说明书未明确)
2. 探索页地图是否必须用真实 MapView? 可接受静态图+标记吗?
3. Canvas 价格曲线的时间范围是 7 天/30 天/90 天?
4. 是否有 UI 设计稿图片可参考? (当前仅靠文字说明)
