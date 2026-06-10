# 变更日志

## [0.4.2] - 2026-06-07

### Added
- **品类上下文持久化**：`category_context` 字段常驻 AgentState，品类关键词推理（38 对显式映射：运动鞋→鞋、T恤→衣服等）+ 对话历史品类继承，切换品类时自动重置偏好（price_min/max/brand/attributes/scenario）
- **零食查询品类守卫**：关键词消歧（食品词集 vs 数码词集），防止"好吃不贵的零食"被误判为数码品类
- **多商品批量操作**：购物车支持索引提取（"前两个"、"第1和第3个"），多商品加购互斥去重

### Changed
- `agent.py` 从 2780 行增长至 3054 行（+274 行品类守卫 + 多商品批量逻辑）
- 测试覆盖扩充：新增 `test_state_slots.py`、`test_category_guard.py`，测试 98→ 预计 105+

## [0.4.1] - 2026-06-07

### Added
- **README 最终版**：队员实名（傅钧烨/唐荣炜/周芯仪）、26 篇文档索引按 7 分类组织
- **全项目审计清理**：移除 75+ 非必要文件（死代码 13 Kotlin、过时文档 28、一次性产物 32+）

### Changed
- **data/images/ 移入 apps/backend/data/images/**：393 张商品图片归入 backend 子树，main.py IMAGES_DIR 路径更新
- **数据入库指引**：SETUP/ARCHITECTURE/MECHANISM 中 ingest_to_qdrant.py → startup.ensure_qdrant_data()
- **文档数量修正**：MECHANISM/DATA-CONTRACT/DESIGN/EVALUATION/TTFT_BENCHMARK 中产品数 250/290 → 190，品类 15 → 94
- **LlamaIndex 引用清除**：DEV-GUIDE/PRD/电商RAG案例分析中移除未实际使用的 LlamaIndex 声明
- **VLM 架构更新**：DESIGN/PERFORMANCE 中 Qwen3-VL-2B → Doubao Vision API

### Fixed
- EVALUATION 测试用例数 226 → 286（与实测一致），章节号 2.3 重复修正
- INNOVATION-RESEARCH 附录实现进度 0%/5% → ✅ 已实现
- DEMO_RUNBOOK 死引用 secret_scan.py → pytest 指令
- DEMO_SCRIPT/核心要求 DeepSeek 降级描述更新

## [0.4.0] - 2026-05-28

### Added
- **下单/结算闭环**：Order ORM 模型 + order_service + REST API（POST/GET/cancel），agent.py checkout 调用真实 service，Android CartScreen 结算按钮对接下单 API + 下单成功弹窗
- **clarify SSE 事件类型**：后端 ClarifyEvent + agent.py 反问专用事件，Android SSEEvent.Clarify + HomeScreen 反问气泡 + 选项 chips
- **商品详情页**：ProductDetailScreen（9 组件：HeroGallery/InfoCard/CouponCard/LogisticsCard/SpecGrid/ReviewSection/ShopCard/BottomActionBar），ProductDetailData 数据模型，ProductDetailViewModel（收藏/关注/加购/下单），NavGraph 导航集成 + HomeScreen 商品卡片点击跳转
- **CompareScreen 真实数据联调**：CompareRepository.fetchProducts() 从后端 API 拉取真实商品，mock fallback
- **E2E 测试脚本**：apps/backend/tests/e2e_scenarios.sh 覆盖 9 场景 + 加分项（购物车/反馈/缓存）
- **性能基准文档**：docs/notes/PERFORMANCE.md（延迟分解/场景实测/缓存指标/优化记录）
- **3-5 分钟演示脚本**：docs/DEMO_SCRIPT.md（5 幕演示 + 技术讲解）
- **答辩 PPT 大纲**：docs/notes/PPT-OUTLINE.md（18 页结构化大纲）
- **final_delivery 打包**：APK 预编译提交 + 目录结构整理（apk/source/docs 就绪，MANIFEST.md 待补充）
- **P@3 检索精度重测**：scripts/run_p3_test.py（直连 Qdrant + BGE 嵌入），eval_cases.json ground truth 更新为真实 product_id，286 用例实测 P@3=0.146（商品推荐类 0.213），消除了 UUID5 修复前的 P@3=0 误报
- **S7 场景化购物增强**：intent.py 关键词扩展（6→18），agent.py `_SCENARIO_FALLBACK_MAP`（11 场景），LLM 失败时规则兜底

### Fixed
- **`get_fast_client()` 修复**：改为复用 `create_llm_client()` → Doubao，不再硬编码 DeepSeek
- **Doubao API Key 验证**：官方 Key 实测通过，`ep-20260514111645-lmgt2` 正常响应
- **Qdrant 数据完整性**：md5[:16]%2^63 hash 碰撞 → uuid.uuid5() 确定性 UUID，消除 ~60 条丢失
- **DATA-CONTRACT 补充**：新增 clarify 事件类型 + UUID5 point ID 说明

### Changed
- 意图分类 / 槽位提取 / 反问生成 / 查询扩展 / 否定解析 — 全部从关键词规则恢复为 LLM 调用
- README.md 全矩阵更新（LLM/Android 状态 + 9 场景完成度 + 技术栈版本）

## [0.3.2] - 2026-05-28

### Added
- **TTS 语音播报**：`TtsManager` 封装 Android TextToSpeech 引擎（中文），增量播报流式 SSE 文本（`speakIncremental`），HomeScreen 顶栏扬声器开关按钮，ChatViewModel 集成 TTS 状态管理

### Audited
- **全场景审计**：场景5（主动反问）后端逻辑完整但无专用SSE事件；场景8 购物车CRUD完整但下单仅对话桩代码；场景9（拍照找货）四层完整打通
- **Doubao LLM 审计**：主生成路径已配置Doubao但Key未验证；`get_fast_client()` 硬编码DeepSeek无Key→全部轻量任务降级关键词规则

## [0.3.1] - 2026-05-27

### Fixed
- **缓存旧数据绕过 top_k=3 逻辑**：添加 `CACHE_VERSION=2` 版本校验，旧缓存自动失效；启动时 `await cache.clear()` 清空缓存；新增 `POST /api/cache/clear` 管理端点
- **缓存命中路径安全阀**：`generate_response` 中 `cards[:3]` 截断，防止旧缓存返回 >3 件商品

### Changed
- **SSE 交错格式**：`_emit_interleaved()` 实现 摘要 → (商品文本 + 卡片) × N → 结语 交替推送，替代原有的「全部文本 → 全部卡片」顺序
- **Prompt 输出格式**：LLM 输出改为 `[SUMMARY]` / `[PRODUCT_N]` / `[CLOSING]` 结构化标记；每商品第一行=商品名，第二行=综合匹配度
- **Android ChatViewModel**：收到 `product_cards` 事件时立即提交当前累积文本+卡片为独立 `ChatMessage`，不再等到流结束才合并为单条消息
- **缓存策略增强**：`cache.set()` 写入 `_v` 版本字段，`cache.get()` 校验版本不一致则自动过期

### Added
- `_parse_response_text()` — 按 `[PRODUCT_N]` 分隔符解析 LLM 输出
- `_emit_interleaved()` — 交错产出 SSE 事件的共享生成器（缓存命中 + 新生成共用）
- `POST /api/cache/clear` — 手动清空查询缓存端点

## [0.3.0] - 2026-05-27

### Fixed
- SSE 进度消息解析：文字聊天 progress 事件修复 `product_info` 字段检测
- 前后端商品数量统一为 3 条
- ProductCard/ProductCardHorizontal 死代码 `price > price` 条件移除
- ChatInputBar 麦克风按钮禁用（语音功能预留）
- ChatViewModel 重复发送竞态条件：新增 `isStreaming` 守卫
- agent.py 静默异常捕获替换为 `logger.warning`
- CartViewModel 4 个乐观更新失败回滚 + 错误状态提示
- Dockerfile 端口 8000→8080 对齐 docker-compose
- reranker.py 线程安全：env 移至模块级 + threading.Lock

### Changed
- agent.py 管道去重：提取 `_extract_raw_products`/`_build_user_prefs`/`_assemble_cards`/`_build_generation_prompt` 4 个共享辅助函数
- `__import__("time")` / `__import__("hashlib")` 替换为顶层 import
- 闲聊回复改为引导澄清句式
- 反幻觉 prompt 统一为规范版本（5 条严禁 + 3 条校验 + 引用标注）

### Security
- `apps/backend/.env` 和 `infrastructure/env/.env` 从 git 取消跟踪（含在线 API key）

### Removed
- 10 个未使用文件：CompareViewModel、ProductRepository、ApiService、ApiClient、ChatScreen、ChatGuideScreen、ProductViewModel、CartPreviewSection、ProductTrendCard、MockHomeProducts

## [0.2.0] - 2026-05-25

### Added
- 拍照找货：VLM 图像理解 → Qdrant 相似检索 → SSE 流式返回
- 场景化组合推荐：LLM 场景分解 → 多类目并行检索 → 去重合并
- 购物车 2 步下单确认流程
- 反幻觉校验层 `_validate_ranked_products`（MIN_MATCH_SCORE=0.25）
- Android CompareScreen、CartScreen、ExploreScreen、ProfileScreen

## [0.1.0] - 2026-05-19

### Added
- FastAPI 后端工程骨架 (main.py, config, database)
- Kotlin Compose 前端工程骨架
- 完整项目结构设计 (v3.1, 覆盖 M1-M10)
- 占位文件生成 (99 个新文件)
- API 接口占位 (chat, products, upload, evaluation, feedback, compare, knowledge)
- 数据模型占位 (Product, Session, Message, Feedback, KnowledgeVersion)
- 业务服务占位 (agent, intent, rag, retriever, llm_client, embedding, ingestion 等 15 个)
- 测试框架及占位测试 (pytest + pytest-asyncio)
- 种子商品数据 (5 个测试商品)
- Docker Compose 基础设施配置
