# 拾物 App — 待做事项 & 工作留痕

> 更新: 2026-05-26 | 来源: 赛题发布会要求审查 + CC审计

---

## 一、已完成 (本次会话)

| # | 任务 | 文件 | 验证 |
|---|------|------|:--:|
| P0-1/2 | SSE协议统一 + 真实SseClient | SseClient.kt, SSEEvent.kt, ChatViewModel.kt | ✓ curl |
| P0-3 | 后端SSE全链路 | chat.py, agent.py | ✓ 5事件 |
| 数据契约 | ProductRecord全栈统一 | 15+ files | ✓ compile |
| P1-1 | 拍照找货闭环 | SseClient, ChatInputBar, ChatViewModel | ✓ VLM→8商品 |
| P1-2 | 多轮对话 | state_manager, agent merge, rag fallback | ✓ R1+R2 |
| P1-3 | Android README | apps/android/README.md | ✓ |
| P1-4 | 品类结构 | seed_products.json 94品类 | ✓ |
| P2-1 | Reranker启用 | main.py lifespan, agent.py, reranker.py | ✓ ~11s |
| P2-2 | 购物车界面 | CartViewModel, CartScreen, MainBottomNavBar | ✓ compile |
| P2-3 | 本地记忆数据库 | LocalDatabase, UserRepository, ChatViewModel | ✓ compile |
| P2-4 | 反选前端交互 | ChatViewModel exclude chips | ✓ curl |
| P2-5 | vision-search image_urls | retriever.py | ✓ curl |
| P2-6 | Gradle缓存污染 | gradle.properties, README | ✓ |
| P2-7 | Backend Dockerfile | Dockerfile, docker-compose.yml | ✓ |
| P2-3 | CompareScreen AI对比 | CompareRepository, CompareScreen | ✓ compile |
| 会话复盘 | 问题+优化+2新Skill | SESSION-RETRO, batch-rename, preflight | ✓ |

---

## 二、待完成 (P0 — 阻塞评审)

| # | 问题 | 影响 | 建议 |
|---|------|------|------|
| P0-1 | **豆包 Key 验证** | 比赛指定模型，DeepSeek降级可能扣分 | 验证 ep-20260514111645-lmgt2 可用性 |
| P0-2 | **Qdrant 数据完整性** | 190 vectors, hash碰撞丢失60条 | 改用 UUID-int 映射或 product_id 直接作 point id |

---

## 三、待完成 (P1 — 影响得分)

| # | 问题 | 当前状态 | 修复方案 |
|---|------|---------|---------|
| P1-1 | CompareScreen 用 Mock 数据 | AI对比按钮已加, 后端 /api/products/compare 未联调 | curl 验证后端 → 前端接入 |
| P1-2 | 缺 RAGAS 评测 | 无 faithfulness/context_recall 指标 | 搭建 RAGAS pipeline |
| P1-3 | 语音图标空实现 | Mic 按钮 onClick={} | 标注"开发中" 或 移除 |
| P1-4 | 图片 URL 为 placeholder | 所有商品图用 placehold.co | 替换为真实商品图或保留（赛题允许） |
| P1-5 | 端到端测试缺失 | 无自动化测试 | 补充 curl 测试脚本 |

---

## 四、待完成 (P2 — 文档对齐)

| # | 问题 | 修复 |
|---|------|------|
| P2-1 | DATA-CONTRACT 检查清单 | 跑完 checklist 中 15 项 |
| P2-2 | docker-compose 缺豆包环境变量 | 添加 DOUBAO_API_KEY / DOUBAO_BASE_URL |
| P2-3 | 缺性能数据 | 记录 TTFT / 端到端延迟到性能文档 |
| P2-4 | 缺 Demo 演示脚本 | 写 3-5 分钟演示流程 |

---

## 五、Git 工作留痕

### 提交历史

```
Session 2026-05-26:
  feat: SSE协议统一 + 真实SseClient (P0)
  feat: 全栈数据契约 ProductRecord v1.0
  feat: 拍照找货前后端闭环 (P1-1)
  fix: 多轮对话上下文继承 (P1-2)
  docs: Android README (P1-3)
  feat: Reranker启用 lifespan预热 (P2-1)
  feat: 购物车界面 + 底部导航 (P2-2)
  feat: 本地SQLite记忆数据库 (P2-3)
  feat: 反选排除品牌chips (P2-4)
  fix: vision-search image_urls字段 (P2-5)
  fix: Gradle缓存污染 workaround (P2-6)
  feat: Backend Dockerfile + docker-compose (P2-7)
  feat: CompareScreen AI对比按钮 (P2-3)
  docs: DATA-CONTRACT.md / LOCAL-MEMORY.md
  chore: 会话复盘 + batch-rename/preflight skills
```

### 提交规范

```
格式: <type>: <简短描述>

type:
  feat    新功能
  fix     修复bug
  docs    文档
  chore   工程/配置
  refactor 重构

示例:
  feat: 拍照找货前后端闭环
  fix: 多轮对话UUID复用导致上下文丢失
  docs: 统一数据契约 DATA-CONTRACT.md
```

---

## 六、演示准备清单

- [ ] 3-5 分钟 Demo 脚本
- [ ] 场景1: AI导购 → 流式回复 → 商品卡片
- [ ] 场景2: 拍照找货 → VLM识别 → 相似商品
- [ ] 场景3: 多轮对话 → 反选排除
- [ ] 场景4: 购物车操作
- [ ] 场景5: AI多商品对比
- [ ] 技术讲解: RAG链路 + Agent编排 + 防幻觉
