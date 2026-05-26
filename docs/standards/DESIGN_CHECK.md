# 前端设计检查机制 v2.0

> Sprint 4 更新: 审计完成 + DESIGN.md库优化 (2026-05-26)

## 触发规则

```
开发到 HomeScreen          → 对照「主页面每日推荐对话」+「主页面对话页」
开发到 CompareScreen       → 对照「比价页」+「比价详情挂画页」
开发到 ExploreScreen       → 对照「探索页」
开发到 ProfileScreen       → 对照「我的页」
开发到 HistoryDrawer       → 对照「历史对话挂画式侧边栏」
开发到 SettingsScreen      → 对照「设置页」
```

## Sprint 4 审计结果 (2026-05-26)

### P0 修复 (全部完成)

| 修复项 | 文件 | 变更摘要 |
|--------|------|---------|
| ProfileHeader 组件 | ProfileScreen.kt | 新增: 蓝粉渐变头部 + 头像 + fujunye + 客服 + 设置 |
| CartPreviewSection 组件化 | ProfileScreen.kt | 硬编码文本 → CartProductMiniCard(商品图+标题+价格) |
| CouponCenter 粉色券面 | ProfileScreen.kt | PrimaryLight→BrandPink 浅粉券面色调 |
| FeatureGrid 矢量图标 | ProfileScreen.kt | Emoji → Material Icons.Default |
| HistoryDrawer 挂画式 | HistoryDrawer.kt | ModalDrawerSheet → Custom Overlay(大圆角+Scrim+74%宽) |
| SettingsScreen 新建 | SettingsScreen.kt | 3分组+法律链接+双按钮, 完全对齐设计说明书 |
| PriceTrendChart Canvas | CompareScreen.kt | 柱状图 → Canvas折线图(红色+浅粉面积+空心终点) |
| CompareDetailPullHandle | CompareScreen.kt | 新增48dp×5dp灰色拖拽把手 |

### P1 遗留项

| 项 | 状态 | 建议 |
|----|:--:|------|
| ChatScreen.kt 与 HomeScreen 职责边界 | ⚠️ | 确认是否保留独立路由 |
| ProductCardHorizontal image_urls 字段 | ⚠️ | 验证 SSE 数据到组件的字段映射 |
| Mock 数据一致性 | ⚠️ | Profile 的 mock 数据需对齐新组件接口 |

### DESIGN.md 库优化 (已应用于代码)

| 优化项 | 源设计系统 | 应用位置 |
|--------|-----------|---------|
| 多层阴影 (1dp+2dp+8dp+) | Airbnb | Card elevation 分级 |
| 圆角语义化 (4/8/14/20/32dp) | Airbnb | RadiusSm/Md/Lg/XL |
| Warm White 背景 | Notion | Neutral50 已接近 |
| Whisper Border | Notion | 待应用到卡片边框 |
| 粉色券面 + 红色金额 | Airbnb | CouponCenterSection |
| Canvas 折线 + 面积填充 | Stripe | PriceTrendChart |
| 矢量图标替代 | Stripe | ProfileFeatureGrid |
| 保守按钮圆角 (4-8dp) | Stripe | RadiusMd/Lg |

## 检查流程

### Step 1: 加载设计文档
```bash
# 以主页为例
cat docs/standards/pages/主页面每日推荐对话.md
cat docs/standards/pages/主页面对话页.md
```

### Step 2: 逐项对照检查

| 检查项 | 文档要求 | 当前实现 | 状态 |
|--------|---------|---------|:--:|
| 页面归属 | 属于 HomeScreen 内部状态 | HomeScreen ChatMessageList | ✅ |
| 问候语 | "fujunye，早上好" | ViewModel sendDailyGreeting | ✅ |
| 渐变 | 蓝→白→粉 水平渐变，仅顶部 | GradientTopBar(statusBar×0.75) | ✅ |
| 商品卡片 | 2×2 网格，含标题/价格/来源 | ProductCard 组件 | ✅ |
| 底部导航 | 首页/比价/探索/我的 | MainBottomNavBar | ✅ |
| 输入栏 | 相机+文本+语音+发送 | ChatInputBar(Unified) | ✅ |
| 价格颜色 | 珊瑚红 #FF5C5C | TextPrice | ✅ |
| 卡片圆角 | 12dp | RadiusLg | ✅ |

### Step 3: 问题分级

| 级别 | 说明 | 处理 |
|:--:|------|------|
| P0 | 与文档设计根本性冲突 | 立即修复 ✅ (Sprint 4 已完成 8 项 P0) |
| P1 | 细节偏差 (间距/字号/颜色) | 本次/下次修复 |
| P2 | 优化建议 | 记录不阻塞 |

## 各页面核心约束

### HomeScreen (主页)
```
- 三态合一: Normal / DailyRecommend / Chat
- 不是独立页面，不是独立路由
- 对话态时底部导航不消失
- 用户名: fujunye
```

### CompareScreen (比价页)
```
- 顶部搜索栏 + 分类标签
- 商品双卡对比区
- 挂画式价格跟踪面板 (expandable)
- ⚠️ 拖拽手势待实现 (当前: 点击PullHandle收起)
- 价格趋势卡片含平台/走势/最低价
- ✅ PriceTrendChart: Canvas曲线图(红色折线+浅粉面积+空心圆点)
```

### ExploreScreen (探索页)
```
- 商品地图概念 (非瀑布流)
- 等尺寸卡片
- 分类胶囊驱动
- 搜索栏 + 相机图标
- ✅ 9×9螺旋算法已实现
```

### ProfileScreen (我的页)
```
- ✅ 用户头部: 头像 + fujunye + 客服 + 设置
- ✅ 购物车预览: 横向商品列表 (CartProductMiniCard)
- ✅ 订单状态: 5图标行
- ✅ 常用功能: 2×3 网格 (Material Icons)
- ✅ 领券中心: 4券卡片 (粉色券面)
```

### HistoryDrawer (历史侧边栏)
```
- ✅ 挂画式: 白色大圆角面板(右上/右下32dp)
- ✅ 左侧滑出: 占74%屏宽, 右侧主页面半露出
- ✅ Scrim遮罩层: 暗色半透明可点击关闭
- ✅ 搜索栏 + 新建对话
- ✅ 月份分组 + 会话列表
- ✅ 用户底部信息: fujunye + 退出登录
```

### SettingsScreen (设置页)
```
- ✅ 分组卡片: 账号与安全 / 功能 / 关于
- ✅ 顶部渐变+返回+标题"设置"+通知角标
- ✅ 底部: 个人信息清单 + 切换账号/退出登录
- ✅ 模式切换: 标准/长辈/未成年人模式
- ✅ 版本更新: "有新版本可升级" + 橙色提示点
- ⚠️ Icon 使用通用占位图标 (待替换为设计对应的专用icon)
```
