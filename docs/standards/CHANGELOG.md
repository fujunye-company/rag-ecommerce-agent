# 变更记录

## 2026-05-27 PM — 3商品输出 + 交错格式修复

### 后端
- cache.py: 新增 CACHE_VERSION=2 版本校验，`get()` 检测版本不一致自动失效旧缓存
- cache.py: `set()` 写入 `_v` 字段标记版本
- main.py: 启动 lifespan 中 `await cache.clear()` + 新增 `POST /api/cache/clear` 端点
- agent.py: `_build_generation_prompt` 改为结构化输出 `[SUMMARY]`/`[PRODUCT_N]`/`[CLOSING]`，每商品第1行=商品名，第2行=匹配度
- agent.py: 新增 `_parse_response_text()` 解析 LLM 结构化输出
- agent.py: 新增 `_emit_interleaved()` 交错发送 SSE 事件（摘要 → 文本+卡片 × N → 结语）
- agent.py: 缓存命中路径 `cards[:3]` 安全阀截断 + 走 `_emit_interleaved` 统一格式
- agent.py: 旧格式缓存降级：无 `[PRODUCT_` 标记时回退到全文→全卡片顺序

### Android
- ChatViewModel: `ProductCard` 事件处理改为立即提交当前累积文本+此卡片为独立 ChatMessage
- ChatViewModel: `Done` 事件处理改为仅提交剩余文本（结语），AI 消息增量持久化
- 每条 AI 回复现为多个 ChatMessage：N 条（文本+单张卡片）+ 可选结语

### 基础设施
- 修复 Windows 原生 postgres 进程与 Docker shopping-pg 端口冲突 (5432)

## 2026-05-27 — 审计修复批处理

### 后端
- agent.py: 管道去重（4 共享辅助函数）、静默异常→logger.warning、`__import__`→顶层 import
- reranker.py: 线程安全修复（env setdefault + threading.Lock）
- Dockerfile: 端口 8000→8080 对齐 docker-compose
- .env 文件 git untrack（安全）

### Android
- ProductCard/ProductCardHorizontal: 死代码 `price > price` 条件移除
- ChatInputBar: 麦克风按钮禁用（语音预留）
- ChatViewModel: 竞态守卫 `if (isStreaming) return`
- CartViewModel: 乐观更新失败回滚 + 错误提示
- 删除 10 个未使用文件/类

### 文档
- CHANGELOG.md 更新至 v0.3.0
- DEV-GUIDE.md 模块完成度刷新
- CLAUDE.md 场景状态 9/9 ✅
- ARCHITECTURE.md v5.0

## 2026-05-25

### Sprint 1 — HomeScreen 重构
- Agent 发送问候 (ChatMessage, 非页面组件)
- 渐变压缩为薄条 (仅状态栏区域)
- 菜单/电话/通知图标仅主页
- 删除所有副标题
- ChatViewModel.sendDailyGreeting()
- 用户气泡浅蓝白 #E3F0FD, 最大280dp
- AgentHintBadge 浅粉胶囊
- ProductCard: 到手价+原价划线+来源标签+右箭头
- ChatInputBar: 相机+文本+语音+发送

### Sprint 2 — CompareScreen 重建
- 搜索栏: 相机+placeholder+红色按钮
- 分类标签: M3默认指示器(选中变大+蓝色)
- 统一2列商品网格(删除双卡区+更多列表)
- 挂画式价格跟踪面板(无下滑条)
- 价格趋势卡片+简易趋势条

### 渐变条统一
- 所有页面从0px开始, 高度=statusBar×0.75
- HomeScreen: 含图标(33dp)
- Compare/Explore/Profile: 空白无内容
- 图标大小: 20dp→22dp→30dp→33dp (多次调整)

### 文件变更
- 修改: HomeScreen.kt ×5, ChatViewModel.kt, MessageBubble.kt, ProductCard.kt ×2, GradientScreenBackground.kt ×4, CompareScreen.kt ×5, NavGraph.kt ×2, ExploreScreen.kt, ProfileScreen.kt, MainBottomNavBar.kt, Color.kt
- 删除: ChatGuideScreen.kt (合并入HomeScreen)
- 文档: DESIGN.md v1→v5, DEV_PLAN.md, CHANGELOG.md
