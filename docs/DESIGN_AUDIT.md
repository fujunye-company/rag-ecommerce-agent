# Design Audit Report — RAG E-Commerce Agent

> 审计时间: 2026-05-31  
> 参考库: awesome-design-md (Shopify / Apple / Nike / Stripe)  
> 审计范围: Android Compose UI 设计系统 + 组件实现

---

## 总体评估: 72/100

| 维度 | 得分 | 说明 |
|------|:----:|------|
| 色彩系统 | 65 | 品牌渐变色与参考系统冲突，语义色过饱和 |
| 字体排版 | 60 | 缺轻字重层级，display 对比度不够，body 偏小 |
| 按钮/形状 | 68 | 圆形不一致，部分交互元素用矩形圆角 |
| 阴影/层级 | 75 | 整体克制，但细节页低层级阴影过多 |
| 摄影/图片 | 88 | 边缘到边缘已实现，细节良好 |
| 间距/栅格 | 85 | 4pt 栅格规范，语义令牌齐全 |
| 暗色模式 | 70 | 预留但未完全实现 |

---

## 1. 色彩系统 (65/100)

### 1.1 品牌渐变色 — 高优先级

**当前**: `GradientStart(#4A90D9) → GradientEnd(#E8917E)` 蓝粉渐变广泛用于顶栏、个人页、详情页背景

**参考原则 (Apple)**:
> "Pick ONE accent color. Never use colors decoratively. A gradient is a decorative treatment — use it only when it MEANS something."

**参考原则 (Shopify)**:
> "Color is a tool, not decoration. Use brand color sparingly on interactive elements; everything else stays neutral."

**参考原则 (Nike)**:
> "Monochrome base palette. Black/white dominate. Accent colors appear only as deliberate brand moments."

**问题**: 蓝→粉渐变是纯粹装饰性的。"蓝"代表品牌色，"粉"代表辅助色，将两者混合成渐变没有明确的语义含义。

**建议**:
- **Primary 路径**: 保留 BrandBlue(#4A90D9) 作为唯一的品牌主色，删除 GradientStart/GradientEnd
- **Secondary 路径**: BrandPink(#E8917E) 降级为功能色的弱引用（如促销标签、限时角标），不作为界面装饰色
- **背景**: 顶栏/页面背景统一使用 `Neutral50(#F8F9FA)` 或纯白，移除 `GradientScreenBackground` 组件中的水平三色渐变

### 1.2 语义色过多

**当前**: Success 绿(#2ECC71) + Warning 橙(#F39C12) + Error 红(#E74C3C) + Info 蓝(#4A90D9)

**问题**: 电商场景不需要 Success/Warning/Info/Error 四色完整语义系统。4 种饱和色增加视觉噪音。

**建议**:
- 保留 `TextPrice` 珊瑚红(#FF5C5C) — 电商价格锚点，不可删
- 保留 Error 用于异常状态
- Success/Warning/Info → 降级为 less-saturated variant，亮度减少 30%

### 1.3 价格色

**当前**: `TextPrice = #FF5C5C` 珊瑚红

**评估**: 符合中国电商习惯（淘宝/京东均用红色价格），保留不变。

---

## 2. 字体排版 (60/100)

### 2.1 缺轻字重 — 高优先级

**当前**: Bold / SemiBold / Medium / Normal 四档

**参考原则 (Shopify)**:
> "Display typography uses thin weights (200-300). Never use bold for headings — bold is for prices and interactive labels only."

**参考原则 (Nike)**:
> "Extreme typographic contrast: ultra-bold wordmarks paired with ultra-light body copy."

**问题**:
- `displayLarge`(28sp Bold) 作为页面标题太重，缺乏高端电商的精致感
- 无 Light/Thin 字重，无法营造对比层次
- 全 App 内 FontWeight 使用集中在 Bold(40%+) 和 SemiBold(30%+)

**建议**:
```kotlin
// 新增轻字重显示样式
displayLarge = TextStyle(
    fontSize = 28.sp,
    fontWeight = FontWeight.Light,  // Bold → Light
    lineHeight = 34.sp,
    letterSpacing = (-0.5).sp,      // 负间距增加高端感
)
headlineLarge = TextStyle(
    fontSize = 22.sp,
    fontWeight = FontWeight.Normal,  // SemiBold → Normal
    lineHeight = 28.sp,
)
```

### 2.2 Body 字号

**当前**: `bodyLarge = 16sp`

**参考原则 (Shopify)**: body 17sp

**建议**: 将 `bodyLarge` 从 16sp 提升到 17sp，6% 增幅微小但可感知。`bodyMedium` 保持 14sp。

### 2.3 价格字体

**当前**: `PriceLarge/Medium/Small` 使用 `FontFamily.Monospace` + Bold

**评估**: Monospace 用于价格在电商中不常见（淘宝用系统字体）。建议改为 `FontFamily.Default` + `FontWeight.Bold`，保持数字对齐用 Tabular Lining。

---

## 3. 按钮与形状 (68/100)

### 3.1 按钮圆角不一致 — 中优先级

**实际使用**:
| 位置 | 圆角 | 形状 |
|------|------|------|
| ProductDetail CTA | 22dp | 胶囊 |
| Cart "下单" | 12dp | 圆角矩形 |
| SearchBar | 24dp | 胶囊 |
| ChatInputBar | 默认 | 矩形(?) |

**参考原则 (Shopify)**:
> "Pill-shaped buttons ONLY. Never use square or slightly rounded corners on interactive elements. Buttons are 50% rounded (full pill)."

**问题**: "下单"按钮用 12dp 圆角矩形，搜索栏用 24dp 胶囊，两者都是主要交互元素但形状不同。

**建议**:
- 所有主操作按钮统一为胶囊形 (`RoundedCornerShape(50)` / `RadiusFull`)
- 搜索栏保持胶囊 (24dp ≈ 48dp/2，已符合)
- Chip/Tag 用 `RadiusMd`(8dp)，与按钮形成形状层级
- `RadiusFull` 令牌已定义但未被引用 — 应在关键按钮中使用

### 3.2 气泡形状

**当前**: `UserBubbleShape` / `AgentBubbleShape` 不对称圆角

**评估**: 设计意图清晰，保留不变。符合 Messenger/微信气泡模式。

---

## 4. 阴影与层级 (75/100)

### 4.1 低层级阴影冗余 — 中优先级

**实际使用**:
- ProductDetailScreen 6 个 section 卡片 **全部** `shadowElevation = 1.dp`
- CompareScreen: 1dp, 2dp, 3dp, 8dp 混用
- 底栏: 8dp（两处）
- 搜索栏: 4dp, 输入栏: 3dp

**参考原则 (Apple)**:
> "One shadow only. Use a single, tight shadow (y:2, blur:8) to indicate elevation. Never use shadows decoratively — a card at rest doesn't need a shadow."

**参考原则 (Shopify)**:
> "Subtle shadow or none. Interactive elements should use scale/tint for feedback, not shadow changes."

**问题**: 6 个 section 卡片全部加 1dp 阴影 = 装饰性阴影。Apple 原则明确指出"静止卡片不需要阴影"。

**建议**:
- 同一页面内，只有 **最高的层级元素** (如吸顶底栏) 需要阴影
- Section 卡片移除阴影，改用 `Outline`(Neutral100) 细线分隔
- 底栏阴影从 8dp → 4dp (减弱)
- 输入栏/搜索栏阴影统一: 都使用 2dp 或不使用（二选一）

### 4.2 抽屉阴影

**当前**: `HistoryDrawer shadowElevation = 16.dp`

**建议**: 降低到 8dp。16dp 在 Material3 中对应 FAB/对话框级别，抽屉侧滑应更轻。

---

## 5. 摄影与图片 (88/100)

### 5.1 已做好的

- 商品卡片图片 `fillMaxWidth().aspectRatio(1f)` + `ContentScale.Crop` — 边缘到边缘
- 详情页 Hero 图片移除顶栏，图片紧贴屏幕顶部
- 图片叠加渐变遮罩用于文字可读性

### 5.2 可优化

**参考原则 (Apple)**:
> "Photos should dominate. Remove chrome — no borders, no shadows, no decorative containers around images."

**问题**: `ProductCard` 外层 Card 有 `cardElevation(2.dp)` + `RoundedCornerShape(8.dp)`，相当于给图片加了一个"相框"效果。Apple 原则明确反对。

**建议**:
- 商品卡片降阴影到 0dp（用细线分隔替代）
- 图片圆角减小到 4dp(小) 或 0dp(无)
- 商品网格间距从 12dp → 8dp，让图片占比更大

---

## 6. 间距与栅格 (85/100)

**评估**: 4pt 栅格 + 语义间距令牌体系完整。`pageHorizontal`, `cardPadding`, `sectionGap` 等语义令牌合理。

**建议**: `space1=4dp` 在 Compose 中不应被使用（4dp 是最小视觉可感知单位），从语义令牌中移除或标记为 deprecated。

---

## 7. 暗色模式 (70/100)

**当前**: `DarkColorScheme` 已定义颜色语义但:
- `DarkBackground = #1A1A1A` 不是纯黑，与 Apple/Nike 的 AMOLED 纯黑偏好冲突
- 缺少暗色模式下的卡片颜色、分隔线颜色
- 无暗色模式渐变替代方案

**建议**:
- `DarkBackground` → `#000000`（纯黑，减少 LCD 漏光视觉差异）
- `DarkSurface` → `#121212`（Google Material 暗色规范标准色）

---

## 8. 优先级排序

| 优先级 | 改进项 | 影响范围 | 工作量 |
|--------|--------|----------|:------:|
| **P0** | 移除装饰性渐变色，统一品牌蓝 | Color.kt + 3 个屏幕 | 小 |
| **P0** | 按钮形状统一胶囊 | 4 个屏幕 | 小 |
| **P1** | 字体增加轻字重层级 | Type.kt + 全局感知 | 中 |
| **P1** | ProductDetail section 卡片去阴影 | ProductDetailScreen.kt | 小 |
| **P2** | body 17sp + 暗色模式完善 | Type.kt + Theme.kt | 中 |
| **P2** | 商品卡片去/降阴影 | ProductCard.kt | 小 |
| **P3** | 语义色去饱和 | Color.kt | 小 |

---

## 9. 中国电商 vs 国际电商 — 文化适配说明

中国电商（淘宝、京东、拼多多）的设计惯例与国际高端电商存在根本差异:

| 特征 | 中国电商 | 国际高端电商 (Shopify/Nike/Apple) |
|------|----------|----------------------------------|
| 价格色 | 红色(醒目) | 黑色(融入) |
| 密度 | 高信息密度 | 留白大 |
| 色彩 | 多彩、促销感 | 单色、克制 |
| 按钮 | 红色/"立即购买" | 品牌色/"Add to cart" |
| 图片 | 白底商品图 | 场景摄影 |

**本项目的定位**: AI 导购 Agent，介于两者之间 — 保留中国电商操作惯例（红价、高密度），吸收国际电商视觉克制（减少装饰渐变、轻字重、去阴影）。

以上审计基于 Shopify/Apple/Nike 三个参考系统的 DESIGN.md 文件（from awesome-design-md），对比本项目 `ui/theme/` 下的完整设计令牌和 30 个组件的实际使用情况。
