# 变更日志

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
