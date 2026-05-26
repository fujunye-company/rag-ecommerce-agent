# DESIGN.md — 电商智能导购 Agent 设计系统 v2.0

> 适用范围: Android Compose / React / Tailwind / 所有前端 Agent
> 约束对象: Claude Code, Codex, 人工开发者
> 最后更新: 2026-05-25
> 基于: 原型图分析 + REQS-竞赛核心需求 + DEV-GUIDE v3.1

---

## 一、项目设计原则

| # | 原则 | 解释 | 反例 |
|:--:|------|------|------|
| 1 | **信任优先** | 每一像素都要传递可靠感。用户在做购买决策，界面不能有一丝廉价感 | 花哨动画、卡通插画、过度圆角 |
| 2 | **信息克制** | 一个屏幕只做一件事。信息密度向 Apple 设计看齐，不向淘宝首页看齐 | 商品卡片塞6行文字、弹窗套弹窗 |
| 3 | **价格即王** | 价格是电商第一决策因子。视觉权重：价格 > 商品名 > 来源 > 其他 | 价格用灰色、与描述同级 |
| 4 | **AI 隐形** | AI 能力体现在"懂你"，不体现在炫技 UI。不加"AI"角标，不加机器人头像 | 紫色渐变AI按钮、🤖 emoji、浮夸粒子 |
| 5 | **移动优先** | 所有设计以 375pt 宽度为基准，向上适配。不先做桌面版再缩放 | 固定px宽度、桌面端hover交互 |
| 6 | **组件原子化** | 每个组件独立、可测试、可替换。React/Vue/Compose 映射一致 | 页面级内联样式、跨组件 CSS 依赖 |
| 7 | **状态全覆盖** | loading / empty / error / content / streaming 五态必现 | 只画"理想状态" |
| 8 | **触控可达** | 所有交互元素 ≥ 44×44pt，间距 ≥ 8pt | 密集按钮、小字号链接 |

---

## 二、品牌关键词

```
可信 · 清爽 · 专业 · 温润 · 克制 · 高效
```

品牌人格: 像一个**懂行的买手朋友**——不喧哗、不推销、给专业建议、尊重你的判断。

情绪板参考:
- Apple Store 的商品页 (干净、留白、大图)
- 豆包的对话流 (轻盈、呼吸感、逐字节奏)
- Linear 的界面 (精准对齐、克制用色)
- 得物的商品卡 (信息层级清晰、价格突出)

---

## 三、色彩 Token

### 3.1 主色系 — 珊瑚红

```
品牌主色: #FF5C5C (珊瑚红)
  - 来源: 温暖不刺眼的红色，比正红柔和，比粉色专业
  - 用途: CTA按钮、价格、选中态、品牌标识
  - 暗色模式: #FF7B7B (提亮10%)
  - 按压态:   #E04848 (加深10%)

品牌主色浅:
  - #FFF0F0  最浅背景 (卡片hover、标签背景)
  - #FFE0E0  浅背景 (推荐区块背景)
  - #FFB0B0  中等 (进度条、滑块)
```

### 3.2 辅助色系

```
成功/正向:
  - #2ECC71  主绿 (价格下降、优惠标签)
  - #E8F8EF  浅绿背景

警告/提醒:
  - #F39C12  主橙 (库存紧张、限时)
  - #FFF8E8  浅橙背景

信息/链接:
  - #4A90D9  主蓝 (链接、来源可点击)
  - #EBF3FC  浅蓝背景

错误/负向:
  - #E74C3C  主红 (删除、错误提示)
  - #FDEDEC  浅红背景
```

### 3.3 中性色系

```
--color-neutral-0:   #FFFFFF   (卡片白、页面底)
--color-neutral-50:  #F8F9FA   (浅灰底、分组背景)
--color-neutral-100: #F0F0F0   (输入框背景、分割线)
--color-neutral-200: #E0E0E0   (禁用态、骨架屏)
--color-neutral-300: #CCCCCC   (边框、占位图)
--color-neutral-400: #AAAAAA   (占位文字)
--color-neutral-500: #888888   (辅助文字、来源标签)
--color-neutral-600: #666666   (次要正文)
--color-neutral-700: #444444   (正文)
--color-neutral-800: #222222   (标题、高强调)
--color-neutral-900: #111111   (最高强调、价格)
```

### 3.4 完整 Token 表

```css
:root {
  /* ===== 主色 ===== */
  --color-primary:          #FF5C5C;
  --color-primary-light:    #FFF0F0;
  --color-primary-dark:     #E04848;
  --color-primary-gradient: linear-gradient(135deg, #FF5C5C 0%, #FF8E8E 100%);

  /* ===== 辅助色 ===== */
  --color-success:          #2ECC71;
  --color-success-light:    #E8F8EF;
  --color-warning:          #F39C12;
  --color-warning-light:    #FFF8E8;
  --color-info:             #4A90D9;
  --color-info-light:       #EBF3FC;
  --color-error:            #E74C3C;
  --color-error-light:      #FDEDEC;

  /* ===== 中性色 ===== */
  --color-bg-page:          #F8F9FA;
  --color-bg-card:          #FFFFFF;
  --color-bg-input:         #F0F0F0;
  --color-border:           #E0E0E0;
  --color-divider:          #F0F0F0;

  /* ===== 文字色 ===== */
  --color-text-primary:     #111111;
  --color-text-secondary:   #444444;
  --color-text-tertiary:    #888888;
  --color-text-placeholder: #AAAAAA;
  --color-text-on-primary:  #FFFFFF;
  --color-text-price:       #FF5C5C;

  /* ===== 功能色 ===== */
  --color-source-tag:       #4A90D9;
  --color-rating-star:      #FFB800;
  --color-skeleton:         #E0E0E0;
  --color-overlay:          rgba(0,0,0,0.4);
}
```

### 3.5 使用规则

```
禁止:
  ✗ 紫色渐变按钮 (品牌色是珊瑚红，不是紫色)
  ✗ 彩虹色商品标签 (统一用 neutral-100 背景 + text-secondary)
  ✗ 超过 3 种彩色同时出现 (主色 + 最多1个辅助色)
  ✗ 黑色背景大面积使用

许可:
  ✓ 价格固定红色 (--color-text-price)
  ✓ 成功/错误状态用对应辅助色
  ✓ 来源标签用蓝色 (可点击暗示)
  ✓ 评分星用金色 (--color-rating-star)
```

---

## 四、字体 Token

### 4.1 字体系列

```css
:root {
  /* Android: 系统默认中文字体 */
  --font-family-base:     -apple-system, BlinkMacSystemFont, "Segoe UI",
                          "PingFang SC", "Hiragino Sans GB", "Microsoft YaHei",
                          "Helvetica Neue", Arial, sans-serif;

  /* 数字/价格专用 - 等宽数字，便于比价 */
  --font-family-number:   "SF Mono", "Menlo", "Consolas", monospace;

  /* 品牌标题 - 可选英文字体 */
  --font-family-display:  "Inter", -apple-system, sans-serif;
}
```

### 4.2 字体粗细

```
--font-weight-regular:  400   (正文、辅助信息)
--font-weight-medium:   500   (列表标题、按钮)
--font-weight-semibold: 600   (卡片标题、对话标题)
--font-weight-bold:     700   (价格、页面标题、强调数字)
```

### 4.3 使用规则

```
禁止:
  ✗ 在正文中使用自定义艺术字体
  ✗ 中英文混排使用不同字体族
  ✗ Light weight (300) 在移动端正文中 (可读性差)

许可:
  ✓ 价格数字使用 --font-family-number (等宽对齐)
  ✓ 英文品牌名使用 --font-family-display
```

---

## 五、字号层级

```css
:root {
  /* 8pt 栅格字号系统 */
  --font-size-display:   28px;  /* 页面大标题 (探索/设置) */
  --font-size-headline:  22px;  /* 卡片标题、对话首句 */
  --font-size-title:     18px;  /* 商品名、消息标题 */
  --font-size-body:      16px;  /* 正文、对话内容 */
  --font-size-caption:   14px;  /* 辅助信息、时间戳 */
  --font-size-small:     12px;  /* 标签、付款人数、来源 */
  --font-size-price-lg:  24px;  /* 大价格 (商品详情) */
  --font-size-price-md:  20px;  /* 中价格 (卡片) */
  --font-size-price-sm:  16px;  /* 小价格 (对话嵌入) */

  /* 行高 */
  --line-height-tight:   1.2;   /* 标题、价格 */
  --line-height-normal:  1.5;   /* 正文 */
  --line-height-relaxed: 1.7;   /* 长文本、推荐理由 */
}
```

### 层级映射表

| Token | 大小 | 粗细 | 行高 | 用途 |
|--------|:---:|:---:|:---:|------|
| display | 28px | Bold | 1.2 | 页面主标题 |
| headline | 22px | SemiBold | 1.3 | Section标题 |
| title | 18px | Medium | 1.3 | 商品名/卡片标题 |
| body | 16px | Regular | 1.5 | 对话正文 |
| caption | 14px | Regular | 1.5 | 辅助信息/时间 |
| small | 12px | Regular | 1.4 | 标签/销量/来源 |
| price-lg | 24px | Bold | 1.2 | 商品详情大价格 |
| price-md | 20px | Bold | 1.2 | 商品卡片价格 |
| price-sm | 16px | Bold | 1.2 | 对话嵌入价格 |

---

## 六、间距系统

### 6.1 基于 4pt 栅格

```css
:root {
  --space-0:   0;
  --space-1:   4px;
  --space-2:   8px;
  --space-3:   12px;
  --space-4:   16px;
  --space-5:   20px;
  --space-6:   24px;
  --space-8:   32px;
  --space-10:  40px;
  --space-12:  48px;
  --space-16:  64px;
}
```

### 6.2 语义间距

```
--space-page-horizontal:  var(--space-4);    /* 页面左右边距 16px */
--space-page-vertical:    var(--space-4);    /* 页面上下边距 */
--space-card-padding:     var(--space-4);    /* 卡片内边距 */
--space-card-gap:         var(--space-3);    /* 卡片间距 12px */
--space-section-gap:      var(--space-6);    /* Section间距 24px */
--space-list-item-gap:    var(--space-2);    /* 列表项间距 8px */
--space-input-padding:    var(--space-3);    /* 输入框内边距 */
--space-chip-gap:         var(--space-2);    /* Chip标签间距 */
--space-icon-hit-area:    44px;              /* 最小触控区域 */
```

### 6.3 使用规则

```
禁止:
  ✗ 奇数像素间距 (破坏 4pt 栅格)
  ✗ 负 margin 实现布局 (用 gap/padding)
  ✗ 百分比 margin 做间距

许可:
  ✓ 所有间距值必须是 4 的倍数
  ✓ 组件间间距用 gap, 不用 margin
  ✓ 页面级间距用 section-gap
```

---

## 七、圆角系统

```css
:root {
  --radius-none:    0;
  --radius-sm:      4px;    /* 小标签、角标 */
  --radius-md:      8px;    /* 图片、缩略图 */
  --radius-lg:      12px;   /* 卡片、气泡 */
  --radius-xl:      16px;   /* 大圆角卡片 (今日推荐) */
  --radius-full:    9999px; /* 胶囊 (按钮、输入框、Chip) */
}
```

### 使用规则

| 元素 | 圆角 | 说明 |
|------|:---:|------|
| 商品卡片 | 12px (--radius-lg) | 统一卡片感知 |
| 对话气泡 | 16px, 对应角4px | 用户角在右下, AI角在左上 |
| 输入框 | --radius-full | 胶囊形 |
| 按钮(CTA) | --radius-full | 胶囊形 |
| 按钮(次要) | 8px (--radius-md) | 文字按钮 |
| 图片 | 8px (--radius-md) | 商品图 |
| Chip标签 | --radius-full | 分类/追问Chip |

---

## 八、阴影系统

```css
:root {
  --shadow-none:    none;

  /* 卡片 - 轻微浮起 */
  --shadow-card:    0 1px 3px rgba(0,0,0,0.06),
                    0 1px 2px rgba(0,0,0,0.04);

  /* 卡片 hover/按压 */
  --shadow-card-raised: 0 4px 12px rgba(0,0,0,0.08),
                        0 2px 4px rgba(0,0,0,0.04);

  /* 浮层/Drawer/Modal */
  --shadow-overlay: 0 8px 30px rgba(0,0,0,0.12),
                    0 4px 8px rgba(0,0,0,0.06);

  /* 底部导航栏 */
  --shadow-bottom-nav: 0 -1px 3px rgba(0,0,0,0.04),
                       0 -1px 2px rgba(0,0,0,0.02);

  /* 按钮 */
  --shadow-button:  0 2px 8px rgba(255,92,92,0.25);
}
```

### 使用规则

```
禁止:
  ✗ 多层叠加的彩色阴影
  ✗ 阴影+边框同时使用 (二选一)
  ✗ 在暗色模式下使用黑色阴影

许可:
  ✓ 最多 2 层阴影叠加
  ✓ 阴影用黑色 + 透明度 (不用彩色)
  ✓ 暗色模式用更浅的阴影或不使用
```

---

## 九、布局栅格

### 9.1 页面栅格

```
视口宽度: 375pt (基准)
列数: 4列
列宽: (375 - 16*2 - 12*3) / 4 = 76.75pt
水槽: 12pt
页边距: 16pt
```

### 9.2 商品网格

```
2列 Grid (推荐):
  卡片宽度 = (375 - 16*2 - 12) / 2 = 165.5pt ≈ 166pt
  图片高度 = 166pt (1:1)

3列 Grid (探索页):
  卡片宽度 = (375 - 16*2 - 12*2) / 3 ≈ 106pt
  仅显示图片+价格，无商品名 (过窄)
  建议: 改为2列，除非只展示图+价

横滑列表:
  卡片宽度 = 140pt
  间距 = 12pt
  首尾留白 = 16pt
```

### 9.3 对话布局

```
消息列表 (LazyColumn/FlatList):
  左右边距: 16pt
  消息间距: 12pt
  气泡最大宽度: 80% 屏幕宽
  用户气泡右对齐，AI气泡左对齐

输入栏:
  固定在底部
  高度: 48pt (单行) → 104pt (多行最大)
  水平边距: 12pt
  内边距: 12pt 水平, 8pt 垂直
```

---

## 十、卡片规则

### 10.1 通用卡片

```
所有卡片遵循:
  - 背景: --color-bg-card (#FFFFFF)
  - 圆角: --radius-lg (12px)
  - 阴影: --shadow-card
  - 内边距: --space-card-padding (16px)
  - 边框: 无 (用阴影区分，不用边框)

卡片内 Section 间距:
  - 标题 → 内容: 12pt
  - 内容 → 操作: 16pt
  - 卡片内嵌卡片: 8pt 间距 + 4px 圆角 + 无阴影
```

### 10.2 卡片状态

```
常态:   --shadow-card + 白色背景
按压态: scale(0.98) + --color-bg-page 背景
禁用态: opacity(0.5) + 不可点击
选中态: 1px solid --color-primary 边框 (替代阴影)
```

---

## 十一、商品卡片规则

### 11.1 标准商品卡片 (Grid用)

```
┌─────────────────────┐
│                     │
│    [商品图片]        │  1:1 比例
│    圆角 8px          │
│                     │
├─────────────────────┤
│ 安踏 轻盈透气运动鞋   │  --font-size-title (18px)
│                      │  Medium, 最多2行截断
│                      │  --color-text-primary
│ ¥319  ¥499           │  现价 --font-size-price-md (20px) Bold 红色
│                      │  原价 --font-size-small (12px) 灰色+删除线
│ 21万人付款            │  --font-size-small, --color-text-tertiary
│                      │
│ [天猫旗舰店]          │  来源标签 (见§18)
└─────────────────────┘

宽: 166pt (2列) / 106pt (3列仅图+价)
高: 自适应内容
图片: 1:1, object-fit: cover, 圆角8px
```

### 11.2 横向商品卡片 (对话嵌入)

```
┌────────────────────────────────────┐
│ ┌────────┐                         │
│ │        │ 轻盈透气运动鞋            │  18px SemiBold
│ │ 商品图  │ ¥319  ¥499               │  价格行
│ │ 100×100│ 透气网面 · 减震回弹        │  14px, 2属性
│ │        │ 🏷 天猫旗舰店              │  来源
│ │        │ ✓ 透气网面设计，适合夏季    │  匹配理由 12px 绿色
│ └────────┘                         │
└────────────────────────────────────┘

宽: 屏幕宽 - 32pt (全宽)
高: 120pt 固定
图片: 100×100pt, 圆角8px, 左侧
布局: Row (图左 + 信息右)
图片右边距: 12pt
```

### 11.3 商品卡片禁止事项

```
禁止:
  ✗ 卡片内超过 2 种字号颜色
  ✗ 价格不用红色
  ✗ 商品名超过 2 行不截断
  ✗ 图片没有 1:1 固定比例 (导致布局跳动)
  ✗ 在一张卡片里塞 评分+销量+优惠券+倒计时+直播
  ✗ 用户头像/昵称出现在商品卡片中
```

---

## 十二、AI 聊天气泡规则

### 12.1 用户气泡

```
┌──────────────────────────┐
│ 我想买一双运动鞋，预算      │
│ 300左右，有推荐的吗         │
└──────────────────────────┘
                           ◥  (尖角在右下)

样式:
  - 背景: --color-primary (#FF5C5C)
  - 文字: --color-text-on-primary (#FFFFFF)
  - 圆角: 16px (左上、左下、右上), 4px (右下, 产生尖角)
  - 最大宽度: 80%
  - 右对齐
  - 字体: --font-size-body (16px), Regular
  - 内边距: 12px 水平, 10px 垂直
```

### 12.2 AI 气泡

```
◤  (尖角在左上)
┌──────────────────────────┐
│ 以下是为您推荐的结果：      │
│                          │
│ ┌──横向商品卡片──────────┐ │
│ │ ...                   │ │
│ └──────────────────────┘ │
└──────────────────────────┘

样式:
  - 背景: --color-bg-card (#FFFFFF)
  - 文字: --color-text-primary (#111111)
  - 圆角: 4px (左上), 16px (其余三角)
  - 最大宽度: 80%
  - 左对齐
  - 阴影: --shadow-card (微弱)
  - 内边距: 12px 水平, 10px 垂直
```

### 12.3 流式气泡 (AI回复中)

```
◤
┌──────────────────────────┐
│ 为您推荐轻盈透气运动鞋，  │
│ 这款鞋采用透气网面材质▌   │  ← 闪烁光标在末尾
└──────────────────────────┘

规则:
  - 文本末追加闪烁光标 "▌"
  - 光标颜色: --color-primary
  - 光标动画: opacity 0→1, 500ms 循环
  - 新文本到达时平滑追加 (不是整段替换)
  - 搜索状态: 气泡上方显示 "正在搜索中…" 12px + 左侧旋转圆环
```

### 12.4 消息间距

```
连续同角色消息: 4pt 间距
角色切换:        12pt 间距
消息+商品卡片:   8pt 间距
系统消息(时间戳): 居中, 单独一行, 12px 灰色
```

---

## 十三、输入框规则

### 13.1 主输入框 (ChatInputBar)

```
┌──────────────────────────────────────┐
│ 📷  点击发送或长按说话…          ➤    │
└──────────────────────────────────────┘

样式:
  - 高度: 48pt (单行), 最大 104pt (4行)
  - 圆角: --radius-full (胶囊形)
  - 背景: --color-bg-input (#F0F0F0)
  - 边框: 无, 聚焦态 1.5px solid --color-primary
  - 文字: --font-size-body (16px)
  - 占位符: --color-text-placeholder (#AAAAAA)
  - 左侧图标: 相机 📷 (28×28pt, --color-text-tertiary)
  - 右侧图标: 发送 ➤ (28×28pt, --color-primary, 输入为空时灰色)
  - 内边距: 12pt 水平
  - 悬浮在键盘上方, 有安全区间距

状态:
  - 空白: 发送按钮灰色, 不可点击
  - 有文字: 发送按钮变主色, 可点击
  - 录音中: 替换为波形动画 + "松开发送"提示
  - 上传中: 缩略图 + 进度条
```

### 13.2 搜索框 (SearchBar)

```
┌──────────────────────────────────────┐
│ 🔍  搜索想探索的好物            [搜索] │
└──────────────────────────────────────┘

样式:
  - 高度: 44pt
  - 圆角: --radius-full
  - 背景: --color-bg-input
  - 右侧搜索按钮: --color-primary 背景, 白色文字, 圆角胶囊
  - 左侧图标: 可选相机图标 (比价页) 或搜索图标 (探索页)
```

---

## 十四、拍照搜索按钮规则

```
📷 相机按钮

样式:
  - 尺寸: 28×28pt (输入栏内), 44×44pt (独立悬浮)
  - 颜色: --color-text-tertiary (常态), --color-primary (有照片)
  - 点击: scale(0.9) → scale(1.0) 弹性动画
  - 长按: 无特殊行为

交互:
  点击 → 底部弹出 ActionSheet:
    ┌──────────────────────┐
    │  📸  拍照             │
    │  🖼  从相册选择         │
    │  ✕   取消             │
    └──────────────────────┘

选择图片后:
  - 缩略图替换相机图标 (28×28pt, 圆角4px)
  - 右侧出现 ✕ 删除按钮 (16×16pt)
  - 点击缩略图 → 全屏预览
  - 点击 ✕ → 清除图片

上传中:
  - 缩略图叠加半透明遮罩 + 进度环
  - 不可发送消息直到上传完成
```

---

## 十五、语音输入按钮规则

```
🎤 语音按钮

样式:
  - 尺寸: 28×28pt (输入栏内)
  - 颜色: --color-text-tertiary (常态), --color-primary (录音中)
  - 位置: 输入栏内, 发送按钮左侧

交互:
  长按 (>200ms):
    - 按钮放大 1.2×
    - 出现波形动画
    - 顶部浮现 "松开发送, 上滑取消"
    - 振动反馈 (Android)

  松手:
    - 触发 ASR 语音识别
    - 识别结果填入输入框
    - 如果输入框已有文字, 追加而非替换

  上滑 (>50pt 垂直位移):
    - 按钮变红
    - 提示变为 "松开取消"
    - 松手 → 取消录音

状态:
  - 不支持语音: 按钮不显示 (降级)
  - 识别失败: Toast "未识别到语音, 请重试"
```

---

## 十六、分类标签/滑动条规则

### 16.1 分类标签行 (CategoryTabs)

```
[推荐] [运动鞋] [袜子] [包袋] [数码] [随机] →

样式:
  - 高度: 36pt
  - 间距: 8pt
  - 标签内边距: 水平12pt, 垂直6pt
  - 常态: --color-bg-input 背景, --color-text-secondary 文字
  - 选中: --color-primary 背景, --color-text-on-primary 文字
  - 首尾各 16pt 间距 (可滑出视觉暗示)
  - 选中指示: 无下划线, 用背景色区分 (填充式)

交互:
  - 左右滑动: 滚动标签
  - 点击标签: 切换分类 + 刷新内容
  - 选中动画: 背景色过渡 200ms ease-out
```

### 16.2 追问 Chip (ClarifyChip)

```
[拍照性能] [续航时间] [性价比] [外观设计]

样式:
  - 同CategoryTabs, 但常态用 --color-primary-light 背景
  - 选中态: --color-primary 背景
  - 位置: AI回复末尾, 商品卡片下方
  - 点击: 自动填入输入框 + 发送
```

---

## 十七、价格展示规则

### 17.1 价格视觉规范

```
现价 (主价格):
  - 字体: --font-family-number (等宽数字)
  - 字号: --font-size-price-md (20px 卡片) / --font-size-price-sm (16px 对话)
  - 颜色: --color-text-price (#FF5C5C)
  - 粗细: Bold
  - 符号: ¥ (人民币, 12px, 上标偏移-2px)

原价 (划线价):
  - 字体: 同系列 Regular
  - 字号: 比现价小 4px
  - 颜色: --color-text-tertiary (#888888)
  - 装饰: line-through
  - 位置: 现价右侧, 间距 8px

价格范围: "¥135-156"
  - 同现价规则
  - 连接符 "-" 不参与等宽

价格降幅: "↓12%"
  - 颜色: --color-success (#2ECC71)
  - 字号: --font-size-small (12px)
  - 位置: 现价右侧
```

### 17.2 价格布局

```
┌─ 商品卡片 ──────────────────────┐
│ ¥319  ¥499  ↓36%               │
│  (主)  (原)  (降)               │
└────────────────────────────────┘

间距:
  现价-原价: 8pt
  原价-降幅: 8pt
  价格行-下一行: 8pt

对齐:
  所有卡片价格左对齐 (同一垂直列)
  不同长度的价格通过等宽字体自然对齐
```

---

## 十八、商品来源展示规则

```
[天猫旗舰店]  [抖音商城]  [京东自营]

样式:
  - 高度: 22pt
  - 圆角: --radius-sm (4px)
  - 背景: --color-info-light (#EBF3FC)
  - 文字: --color-info (#4A90D9)
  - 字号: --font-size-small (12px)
  - 内边距: 水平6pt, 垂直2pt
  - 图标: 16×16pt 平台logo (可选)

位置:
  - 商品卡片内部, 价格下方
  - 对话商品卡: 属性标签行末尾

交互:
  - 可点击: 点击跳转商品详情页
  - 不可点击: 纯展示
  - 点击态: 背景加深至 --color-info + 10% alpha
```

---

## 十九、推荐理由展示规则

```
✓ 透气网面设计，适合夏季运动，价格在预算范围内

样式:
  - 字号: --font-size-small (12px)
  - 颜色: --color-success (#2ECC71)
  - 前缀: "✓" 或 "💡" 图标 (14×14pt)
  - 最大宽度: 100% 气泡宽
  - 行数: 最多 2 行, 超出截断+省略号
  - 位置: 对话嵌入商品卡的最底部

格式:
  "① 匹配依据 + ② 品质亮点 + ③ 适用场景"
  例: "预算300元内，这款299元 ✓ 透气网面，减震回弹 ✓ 适合日常跑步"

规则:
  - 必须真实 (来自 retrieval 结果, 禁止编造)
  - 不超过 40 个字
  - 正面描述为主, 不加负面评价
  - 不自称 "AI推荐" 或 "智能推荐"
```

---

## 二十、状态规则

### 20.1 加载态 (Loading)

```
骨架屏 (Skeleton):
  - 颜色: --color-skeleton (#E0E0E0)
  - 动画: shimmer (从左到右光泽扫过, 1.5s)
  - 形状: 与内容相同的圆角矩形占位

商品卡片骨架:
  ┌─────────────────────┐
  │ ████████████████████ │  图片占位 (1:1)
  │ ██████████████       │  标题占位 (80%宽)
  │ ██████               │  价格占位 (40%宽)
  │ ████                 │  来源占位 (25%宽)
  └─────────────────────┘

对话加载:
  - AI气泡内显示 3 个跳动的点 "..." (bounce动画)
  - 不显示骨架屏
```

### 20.2 空状态 (Empty)

```
无搜索结果:
  ┌──────────────────────────┐
  │                          │
  │      [空状态插图]          │  简洁线稿, 不用卡通
  │                          │
  │   未找到相关商品            │  18px SemiBold
  │   试试其他关键词或分类筛选   │  14px 灰色
  │                          │
  │      [换个关键词]          │  次要按钮
  └──────────────────────────┘

插图风格: 线性图标, --color-border 颜色, 120×120pt

无对话历史:
  ┌──────────────────────────┐
  │                          │
  │    👋 早上好              │  22px SemiBold
  │    想买什么？我帮你找       │  16px 灰色
  │                          │
  │  [推荐跑鞋] [200元耳机]    │  示例问题Chip
  └──────────────────────────┘
```

### 20.3 错误状态 (Error)

```
网络错误:
  ┌──────────────────────────┐
  │                          │
  │      [🌐 断连图标]        │
  │                          │
  │   网络连接已断开            │
  │   请检查网络后重试          │
  │                          │
  │      [重试]               │  主色按钮
  └──────────────────────────┘

发送失败:
  - 消息气泡右侧显示红色感叹号 ⚠ (16×16pt)
  - 点击感叹号 → 重发
  - 不自动重发

服务端错误:
  - Toast: "服务繁忙，请稍后重试" (2s自动消失)
  - 不阻塞用户继续浏览
```

### 20.4 流式输出态 (Streaming)

```
触发: SSE text_delta 事件到达
表现:
  - 新 AI 气泡从底部出现 (fadeIn + slideUp, 200ms)
  - 文字逐字追加 (非逐token, 可按词/字追加)
  - 末尾闪烁光标 "▌" (opacity 0↔1, 500ms 循环)
  - 光标颜色: --color-primary
  - 自动滚动到底部 (smooth scroll)
  - 搜索状态: 气泡上方固定12px高提示条 "正在搜索中… ◎"

text_delta 完成 → product_cards 到达:
  - 文字区下方 8pt 间距
  - 商品卡片从底部渐入 (fadeIn + slideUp, 300ms, stagger each)
  - 最多 3 张横向商品卡

done 事件:
  - 光标消失 (fadeOut 200ms)
  - 消息固化到列表
  - 反馈按钮出现 (有帮助/无帮助, fadeIn 300ms)
```

---

## 二十一、移动端适配规则

### 21.1 断点

```
基准宽度: 375pt (iPhone 12/13)
最小宽度: 320pt (iPhone SE)
最大宽度: 428pt (iPhone 14 Pro Max)
平板:    768pt+ (2列 → 3列商品Grid)
```

### 21.2 适配策略

```
所有尺寸使用相对单位:
  - 宽度: 基于屏幕百分比 (不用固定px)
  - 字体: pt/sp (iOS/Android原生)
  - 间距: 固定pt (不随屏幕缩放)

商品 Grid:
  375pt:  2列 (165pt/列)
  428pt:  2列 (190pt/列) ← 卡片更宽, 不变列数
  768pt+: 3列 (236pt/列)

对话气泡:
  最大宽度 = min(屏幕宽×0.8, 400pt)

底部安全区:
  - iPhone X+: 底部 +34pt padding
  - Android: 系统自动处理 (WindowInsets)
```

### 21.3 横屏

```
横屏: 不强制竖屏, 但推荐竖屏
横屏对话页: 气泡最大宽度 = 60% (更窄)
商品Grid: 3-4列
```

---

## 二十二、可访问性规则

### 22.1 对比度 (WCAG AA)

```
正文文字: ≥ 4.5:1
  #111111 on #FFFFFF = 17.1:1 ✅
  #444444 on #FFFFFF = 9.4:1  ✅

大文字 (≥18px Bold): ≥ 3:1
  #FF5C5C on #FFFFFF = 4.0:1  ✅ (价格)
  #888888 on #FFFFFF = 4.5:1  ✅

按钮文字:
  #FFFFFF on #FF5C5C = 4.6:1  ✅
```

### 22.2 触控区域

```
所有交互元素 ≥ 44×44pt
列表项高度 ≥ 48pt
关闭按钮 ≥ 44×44pt (即使图标是24×24)
相邻可点击元素间距 ≥ 8pt
```

### 22.3 其他

```
- 所有图片必须设置 contentDescription
- 输入框必须设置 imeAction + keyboardType
- 状态变化通过 animateItemPlacement 平滑过渡
- 不使用纯颜色传递信息 (如"红色=错误", 需同时显示文字)
- 支持系统字体缩放 (最多 1.5×)
```

---

## 二十三、禁止事项

### P0 红线 — 违反即不合格

```
✗ 紫色渐变作为主色调 (品牌主色是 #FF5C5C 珊瑚红)
✗ 价格用黑色/灰色 (必须红色)
✗ 商品卡片无 1:1 固定图片比例 (导致布局跳动)
✗ 对话页>3种气泡颜色
✗ AI 回复中编造不存在的商品/优惠券/价格
✗ 输入框不显示发送按钮
✗ 缺少 Loading/Empty/Error 任一状态
```

### P1 警告 — 影响品质评分

```
✗ 在商品卡片中堆叠超过 3 行辅助信息
✗ 使用 emoji 替代图标 (💯🔥🎉 等)
✗ 卡片同时使用阴影+边框
✗ AI 气泡加"AI"角标
✗ 骨架屏使用纯色无动画
✗ 字体大小不遵循 4pt 栅格 (用了 15px/17px 等)
✗ Toast 超过 2 秒 (用户来不及看清)
✗ 按钮圆角不一 (有的 24px, 有的 20px)
```

### P2 建议 — 优化方向

```
✗ 品牌色大面积作背景 (刺眼, 仅用作强调)
✗ 灰色文字超过 3 种不同值
✗ 对话页顶部导航颜色与底部不一致
✗ 缺少按压反馈 (scale/opacity)
```

---

## 二十四、代码示例

### 24.1 Tailwind Theme Token

```js
// tailwind.config.js
module.exports = {
  theme: {
    extend: {
      colors: {
        primary: {
          DEFAULT: '#FF5C5C',
          light: '#FFF0F0',
          dark: '#E04848',
        },
        success: {
          DEFAULT: '#2ECC71',
          light: '#E8F8EF',
        },
        warning: {
          DEFAULT: '#F39C12',
          light: '#FFF8E8',
        },
        info: {
          DEFAULT: '#4A90D9',
          light: '#EBF3FC',
        },
        neutral: {
          0: '#FFFFFF',
          50: '#F8F9FA',
          100: '#F0F0F0',
          200: '#E0E0E0',
          300: '#CCCCCC',
          400: '#AAAAAA',
          500: '#888888',
          600: '#666666',
          700: '#444444',
          800: '#222222',
          900: '#111111',
        },
      },
      fontSize: {
        'display': ['28px', { lineHeight: '1.2', fontWeight: '700' }],
        'headline': ['22px', { lineHeight: '1.3', fontWeight: '600' }],
        'title': ['18px', { lineHeight: '1.3', fontWeight: '500' }],
        'body': ['16px', { lineHeight: '1.5', fontWeight: '400' }],
        'caption': ['14px', { lineHeight: '1.5', fontWeight: '400' }],
        'small': ['12px', { lineHeight: '1.4', fontWeight: '400' }],
        'price-lg': ['24px', { lineHeight: '1.2', fontWeight: '700' }],
        'price-md': ['20px', { lineHeight: '1.2', fontWeight: '700' }],
        'price-sm': ['16px', { lineHeight: '1.2', fontWeight: '700' }],
      },
      borderRadius: {
        'sm': '4px',
        'md': '8px',
        'lg': '12px',
        'xl': '16px',
        'full': '9999px',
      },
      spacing: {
        '0': '0',
        '1': '4px',
        '2': '8px',
        '3': '12px',
        '4': '16px',
        '5': '20px',
        '6': '24px',
        '8': '32px',
        '10': '40px',
        '12': '48px',
      },
      boxShadow: {
        'card': '0 1px 3px rgba(0,0,0,0.06), 0 1px 2px rgba(0,0,0,0.04)',
        'card-raised': '0 4px 12px rgba(0,0,0,0.08), 0 2px 4px rgba(0,0,0,0.04)',
        'overlay': '0 8px 30px rgba(0,0,0,0.12), 0 4px 8px rgba(0,0,0,0.06)',
        'bottom-nav': '0 -1px 3px rgba(0,0,0,0.04), 0 -1px 2px rgba(0,0,0,0.02)',
        'button': '0 2px 8px rgba(255,92,92,0.25)',
      },
      fontFamily: {
        'base': ['-apple-system', 'BlinkMacSystemFont', '"Segoe UI"', '"PingFang SC"', '"Hiragino Sans GB"', '"Microsoft YaHei"', 'sans-serif'],
        'number': ['"SF Mono"', '"Menlo"', '"Consolas"', 'monospace'],
      },
    },
  },
};
```

### 24.2 React 组件示例

```tsx
// ProductCard.tsx — 标准商品卡片
import React from 'react';

interface ProductCardProps {
  product: Product;
  onPress?: () => void;
  variant?: 'grid' | 'horizontal';
}

export const ProductCard: React.FC<ProductCardProps> = ({
  product,
  onPress,
  variant = 'grid',
}) => {
  if (variant === 'horizontal') {
    return (
      <div
        onClick={onPress}
        className="
          flex gap-3 p-4 w-full h-[120px]
          bg-white rounded-lg shadow-card
          active:scale-[0.98] active:bg-neutral-50
          transition-transform duration-200
        "
      >
        {/* 商品图 */}
        <img
          src={product.imageUrl}
          alt={product.name}
          className="w-[100px] h-[100px] rounded-md object-cover flex-shrink-0"
        />
        {/* 信息区 */}
        <div className="flex flex-col justify-between flex-1 min-w-0">
          <h3 className="text-title text-neutral-900 truncate-2">
            {product.name}
          </h3>
          <div className="flex items-baseline gap-2">
            <span className="text-price-sm text-primary font-bold font-number">
              ¥{product.price}
            </span>
            {product.originalPrice && (
              <span className="text-small text-neutral-500 line-through">
                ¥{product.originalPrice}
              </span>
            )}
          </div>
          <div className="flex items-center gap-2">
            <span className="px-1.5 py-0.5 rounded-sm bg-info-light text-info text-small">
              {product.source}
            </span>
          </div>
          {product.matchReason && (
            <p className="text-small text-success truncate">
              ✓ {product.matchReason}
            </p>
          )}
        </div>
      </div>
    );
  }

  // Grid variant
  return (
    <div
      onClick={onPress}
      className="
        flex flex-col bg-white rounded-lg shadow-card overflow-hidden
        active:scale-[0.98] transition-transform duration-200
      "
    >
      <div className="aspect-square overflow-hidden">
        <img
          src={product.imageUrl}
          alt={product.name}
          className="w-full h-full object-cover rounded-md"
        />
      </div>
      <div className="p-4 flex flex-col gap-1">
        <h3 className="text-title text-neutral-900 line-clamp-2 leading-tight">
          {product.name}
        </h3>
        <div className="flex items-baseline gap-2">
          <span className="text-price-md text-primary font-bold font-number">
            ¥{product.price}
          </span>
          {product.originalPrice && (
            <span className="text-small text-neutral-500 line-through">
              ¥{product.originalPrice}
            </span>
          )}
        </div>
        {product.salesCount && (
          <span className="text-small text-neutral-500">
            {product.salesCount > 10000
              ? `${(product.salesCount / 10000).toFixed(1)}万人付款`
              : `${product.salesCount}人付款`}
          </span>
        )}
        <span className="px-1.5 py-0.5 rounded-sm bg-info-light text-info text-small self-start">
          {product.source}
        </span>
      </div>
    </div>
  );
};
```

```tsx
// MessageBubble.tsx — 聊天气泡
import React from 'react';

interface MessageBubbleProps {
  message: Message;
  isStreaming?: boolean;
}

export const MessageBubble: React.FC<MessageBubbleProps> = ({
  message,
  isStreaming,
}) => {
  const isUser = message.role === 'USER';

  return (
    <div className={`flex ${isUser ? 'justify-end' : 'justify-start'} px-4`}>
      <div
        className={`
          max-w-[80%] px-3 py-2.5
          ${isUser
            ? 'bg-primary text-white rounded-[16px_16px_4px_16px]'
            : 'bg-white text-neutral-900 rounded-[4px_16px_16px_16px] shadow-card'
          }
        `}
      >
        <p className="text-body whitespace-pre-wrap break-words">
          {message.content}
          {isStreaming && (
            <span className="inline-block w-[2px] h-[1em] bg-primary align-middle ml-0.5 animate-pulse">
              &nbsp;
            </span>
          )}
        </p>

        {/* 嵌入商品卡片 */}
        {message.productCards?.map((card) => (
          <div key={card.id} className="mt-2">
            <ProductCard product={card} variant="horizontal" />
          </div>
        ))}
      </div>

      {/* 发送失败重试 */}
      {message.status === 'ERROR' && (
        <button
          onClick={() => onRetry?.(message.id)}
          className="ml-2 text-error self-center"
          aria-label="重试发送"
        >
          ⚠
        </button>
      )}
    </div>
  );
};
```

### 24.3 Android Compose Theme

```kotlin
// Color.kt
val Primary = Color(0xFFFF5C5C)
val PrimaryLight = Color(0xFFFFF0F0)
val PrimaryDark = Color(0xFFE04848)

val Success = Color(0xFF2ECC71)
val Warning = Color(0xFFF39C12)
val Info = Color(0xFF4A90D9)
val Error = Color(0xFFE74C3C)

val Neutral0 = Color(0xFFFFFFFF)
val Neutral50 = Color(0xFFF8F9FA)
val Neutral100 = Color(0xFFF0F0F0)
val Neutral200 = Color(0xFFE0E0E0)
val Neutral400 = Color(0xFFAAAAAA)
val Neutral500 = Color(0xFF888888)
val Neutral700 = Color(0xFF444444)
val Neutral900 = Color(0xFF111111)

val TextPrice = Primary  // 价格固定红色
val SourceTag = Info     // 来源蓝色
val RatingStar = Color(0xFFFFB800)  // 评分金色

// Type.kt
val Typography = Typography(
    displayLarge = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.Bold, lineHeight = 34.sp),
    headlineMedium = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.SemiBold, lineHeight = 28.sp),
    titleMedium = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Medium, lineHeight = 24.sp),
    bodyLarge = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Normal, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Normal, lineHeight = 20.sp),
    labelSmall = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Normal, lineHeight = 16.sp),
)

// Shape.kt
val Shapes = Shapes(
    small = RoundedCornerShape(4.dp),
    medium = RoundedCornerShape(8.dp),
    large = RoundedCornerShape(12.dp),
    extraLarge = RoundedCornerShape(16.dp),
)

// Theme.kt
private val LightColorScheme = lightColorScheme(
    primary = Primary,
    onPrimary = Color.White,
    primaryContainer = PrimaryLight,
    secondary = Info,
    background = Neutral50,
    surface = Neutral0,
    onSurface = Neutral900,
    onSurfaceVariant = Neutral500,
    error = Error,
    outline = Neutral200,
)

@Composable
fun ShoppingAgentTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content,
    )
}
```

---

> **本文档为 DESIGN.md v2.0，所有前端 Agent (Claude Code / Codex) 和人工开发者必须遵守。**
> 
> 冲突裁决: DESIGN.md > 原型图 > 个人偏好
> 
> 如需修改设计 token，须更新本文档并通知全队。
> 
> 路径: `04-rag-ecommerce/docs/standards/DESIGN.md`
