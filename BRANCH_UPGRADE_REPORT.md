# main 分支升级报告

> **对比基准**: `nilesthump` (ca49d75) → `main` (afde65b)
> **生成时间**: 2026-05-31
> **领先 commits**: 5 feature + 2 merge = 7 提交

---

## 一、总体概览

| 维度 | nilesthump | main | 变化 |
|------|-----------|------|------|
| 文件总数 | 196 | 380 | **+184 (+94%)** |
| Python 文件 | 68 | 80 | +12 |
| Android 文件 | 46 | 87 | **+41 (+89%)** |
| 代码增量 | — | — | **+21,102 / -2,077 行** |

---

## 二、nilesthump 分支定位

nilesthump 是早期功能分支，核心贡献：**Qdrant 商品库构建**（2 commits）

| Commit | 内容 |
|--------|------|
| ca49d75 | 50条商品 + 评论 → Qdrant 入库脚本（API 调用方式） |
| 94c5824 | 调库测试 + BGE 模型配置说明 |

**nilesthump 交付物** (16 files, +8,469/-233):
- `seed_products.json` (1,302 行) — 50 条种子商品
- `seed_reviews.json` (6,399 行) — 商品评论数据
- `ingest_to_qdrant.py` (281 行) — Qdrant 入库脚本
- `retrieve_from_qdrant.py` (171 行) — 检索测试脚本
- `qdrant/README.md` (201 行) — 使用说明

**nilesthump 时项目状态**: 后端基本骨架 + Android 基础脚手架（仅 Home/Compare/History/ProductDetail 4 个占位页面），整体处于 Phase 0 早期。

---

## 三、main 分支提交历史

```
afde65b feat: P@3检索重测 + S7场景增强 + 评测体系完善 + 进度报告
32827fc fix: reranker CI 降级 — 模型不可用时回退原始分数排序
43dd68e feat: 交错SSE输出 + 缓存版本校验 + 3商品硬限制 + 文档更新
264997a feat: 全栈赛题对齐 — P0/P1/P2 修复批处理
365c08d feat: Phase 0-4 前端7页面 + 后端P0修复 (compare/clarify/cart/SSE优化/Doubao)
d5d7bda Merge PR #4 ← nilesthump (商品库构建)
f640e35 Merge PR #3 ← feat/docs-and-config-update (文档+配置更新)
```

nilesthump 合并后，main 又经历了 **5 轮功能迭代**。

---

## 四、分领域升级详情

### 4.1 后端服务层 — 从骨架到全栈引擎

| 文件 | 增量 | 关键变更 |
|------|------|----------|
| `services/agent.py` | +1,249 | **核心引擎重构**: clarify追问节点、交错SSE、3商品硬限制、多轮对话 |
| `services/comparator.py` | +322 | 对比逻辑全面重写 |
| `services/intent.py` | +267 | 意图识别增强（槽位填充 + 理由生成） |
| `services/product_ranker.py` | +266 | P@3 排序优化 + 评测集成 |
| `services/image_parser.py` | +232 | VLM 图片解析增强（多模态导购） |
| `services/evaluator.py` | +195 | 评测体系从零搭建 |
| `services/llm_client.py` | +154 | Doubao-Seed-2.0 适配 + DeepSeek 降级 |
| `services/reranker.py` | +150 | CI 降级容错（模型不可用回退原始分） |
| `services/state_manager.py` | +132 | 缓存版本校验 + 会话状态管理 |
| `services/cache.py` | +63 | 缓存策略优化 |
| `services/cart_service.py` | +61 | 购物车服务完善 |
| `services/retriever.py` | +80 | 检索逻辑重构（融合 Qdrant） |

**新增服务/模型**:
- `services/rag.py` — RAG 检索封装层
- `services/order_service.py` + `models/order.py` — 订单系统
- `api/order.py` — 订单 API 端点

**API 层新增/扩展**:
| 端点 | 增量 | 说明 |
|------|------|------|
| `api/upload.py` | +158 | 图片上传 + VLM 解析 |
| `main.py` | +127 | 路由注册 + CORS + 中间件 |
| `api/cart.py` | +104 | 购物车 CRUD |
| `api/evaluation.py` | +42 | 评测 API |
| `api/chat.py` | +32 | SSE 流式对话 |
| `api/compare.py` | +31 | 商品对比 |

### 4.2 Android 前端 — 从 4 页占位到 11 页完整 UI

**nilesthump 时代**: 仅 4 个占位页面（Home、Compare、History、ProductDetail），基础脚手架。

**main 时代**: 完整 11 页面 + 10+ 组件体系。

| 页面 | 行数 | 状态 |
|------|------|------|
| `ProductDetailScreen.kt` | 747 | **重写** 9 组件详情页 |
| `CompareScreen.kt` | 509 | **重写** 后端联调 |
| `ProfileScreen.kt` | 420 | **新增** 个人中心 |
| `CartScreen.kt` | 246 | **新增** 购物车 |
| `HomeScreen.kt` | 180 | **重构** 首页 |
| `ExploreScreen.kt` | 182 | **新增** 探索发现 |
| `SettingsScreen.kt` | 166 | **新增** 设置 |
| `ExploreProductPostScreen.kt` | 103 | **新增** 商品帖子 |
| `CategoryListScreen.kt` | 15 | **新增** 分类列表 |

**新增 UI 组件 (10+)**:
- `ChatInputBar.kt` — 统一聊天输入栏 (277行)
- `HistoryDrawer.kt` — 对话历史抽屉 (285行)
- `ProductCardHorizontal.kt` — 横向商品卡片 (89行)
- `PriceTrendChart.kt` — 价格趋势图 (106行)
- `StreamingBubble.kt` — SSE 流式气泡 (88行)
- `UnifiedSearchBar.kt` — 统一搜索栏 (74行)
- `GradientScreenBackground.kt` — 渐变背景 (55行)
- `MainBottomNavBar.kt` — 底部导航栏 (66行)
- `ExploreProductNodeCard.kt` — 探索节点卡片 (74行)

**数据层新增**:
- Mock 数据体系: 7 个文件 (+2,142 行) — 实现离线开发能力
- `CartViewModel.kt` (+258) — 购物车状态管理
- `ChatViewModel.kt` (+595) — 对话引擎重构
- `ProductDetailViewModel.kt` (+132) — 商品详情状态
- `UserRepository.kt` (+299) — 用户数据仓库
- `LocalDatabase.kt` (+124) — 本地持久化
- `TtsManager.kt` (+77) — TTS 语音播报
- `SSE Client` (+235) — 流式解析重构

### 4.3 数据层 — 从 50 条到评测体系

**商品数据扩展**:
| 文件 | nilesthump | main | 变化 |
|------|-----------|------|------|
| `seed_products.json` | 1,302 行 | 5,532 行 | +4,230 |
| `seed_reviews.json` | 6,399 行 | 5,716 行 | 重构 |
| `qdrant/README.md` | 201 行 | 229 行 | +28 |

**评测体系 (新增)**:
| 文件 | 行数 | 说明 |
|------|------|------|
| `eval_cases.json` | 3,023 | 评测用例库 |
| `eval_results_30.json` | 9,335 | 评测结果报告 |
| `p3_results.json` | 8,315 | P@3 检索评测 |
| `eval_ckpt_0/10/20.json` | 9,365 | 检查点快照 |
| `run_eval.py` | 223 | 评测脚本入口 |

### 4.4 文档体系 — 从简陋到完善

| 类别 | nilesthump | main | 变化 |
|------|-----------|------|------|
| 标准文档 | 7 个 | 20 个 | **+13** |
| 设计页面 | 0 | 8 个 | **+8** |
| 项目记录 | 0 | 8 个 | **+8** |
| 答辩材料 | 0 | 11 个 .docx | **+11** |

**新增核心文档**:
- `ANDROID_COMPOSE_MIGRATION.md` (+1,674) — Compose 迁移指南
- `MECHANISM.md` (+717) — 系统运行机制
- `FRONTEND_DEV_PLAN.md` (+355) — 前端开发计划
- `SETUP.md` (+279) — 环境搭建指南
- `DATA-CONTRACT.md` (+262) — 数据契约
- `ARCHITECTURE.md` (+257) — 重写架构文档
- `EVALUATION.md` (+163) — 评测体系文档

### 4.5 测试体系 — 从 0 到 E2E

**nilesthump**: 无后端测试。

**main 新增**:

| 文件 | 行数 | 说明 |
|------|------|------|
| `e2e_scenarios.sh` | 186 | **E2E 9 场景**端到端脚本 |
| `test_intent.py` | 114 | 意图识别测试 |
| `test_product_ranker.py` | 86 | 排序评测测试 |
| `test_rag.py` | 54 | RAG 检索测试 |
| `test_agent.py` | 50 | Agent 流程测试 |
| `test_sse.py` | 48 | SSE 流式输出测试 |
| `test_reranker.py` | 46 | Reranker 降级测试 |
| `test_products.py` | 36 | 商品 API 测试 |
| `test_feedback.py` | 34 | 反馈机制测试 |

### 4.6 基础设施

- **docker-compose.yml** (+32): Qdrant + PostgreSQL + pgvector 服务完善
- **CLAUDE.md** (+47): v4.0 重写 — 项目上下文、Skill 映射、里程碑
- **README.md** (+84): 项目说明重构
- **build.gradle.kts** (+25): Android 构建配置更新
- **.gitignore** (+4): 补充忽略规则

---

## 五、升级路线图

```
nilesthump (ca49d75) ─── 商品库构建里程碑
  │
  ├─ PR #3 merge ─── 文档 + 配置基线更新
  │
  ├─ Phase 0-4 ─── 前端 7 页面 + 后端 P0 修复
  │   ├─ Compare / Clarify / Cart 场景打通
  │   ├─ SSE 流式优化 + Doubao 适配
  │   └─ Mock 数据体系建立
  │
  ├─ 全栈赛题对齐 ─── P0/P1/P2 修复批处理
  │   ├─ 3 商品硬限制 + clarify 追问节点
  │   └─ ProductDetail 9 组件重写
  │
  ├─ 交错 SSE + 缓存 ─── 性能 & 可靠性
  │   ├─ 检索+LLM 并行流式
  │   └─ 缓存版本校验 + 文档补全
  │
  ├─ Reranker CI 降级 ─── 生产鲁棒性
  │
  └─ P@3 检索重测 + S7 ─── 评测完善
      ├─ 评测体系从 0 到 1
      ├─ S7 场景后端
      └─ 进度报告 + 交付准备
```

---

## 六、当前 main 分支状态

| 指标 | 值 |
|------|-----|
| 场景完成度 | 8/9 全栈打通 |
| APK 大小 | 24.1 MB（编译成功） |
| 比赛评分估计 | ~84% |
| 后端测试覆盖 | E2E + 单元 + 评测 |
| 文档完整度 | 标准 20 篇 + 设计 8 页 + 答辩材料 11 份 |

---

## 七、风险提示

1. `nilesthump` 分支已合并且**本地/远程均已删除**，无法直接 checkout 验证
2. `__pycache__/` 目录被 Git 追踪（nilesthump 时代遗留问题）
3. `seed_products.json.bak` 系列文件体积大（7,583 行），建议清理
4. 多个 `.docx` 答辩文件被纳入版本控制，建议确认是否需要 Git LFS
