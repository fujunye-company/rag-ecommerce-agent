# 电商RAG导购Agent — 优先级优化清单（现状 vs 业界最佳实践）

> 生成时间：2026-05-19
> 对比依据：docs/background/电商RAG导购Agent案例分析.md（文档第7章 + 第3-6章大厂案例）
> 对比基线：apps/backend/app/services/（agent / rag / retriever / intent / embedding / llm_client 等 16 个模块）

---

## 快速对照：当前实现 vs 文档建议

| 模块 | 当前状态 | 文档建议 | 完成度 |
|------|---------|---------|--------|
| agent.py | StateGraph 骨架，仅返回 greeting | 5 个核心节点 (classify→extract→retrieve→rank→generate) + clarify 追问 | 5% |
| intent.py | 关键词规则 5 类 + slot 返回空 dict | LLM prompt-based 6 类 intent + structured slot schema | 15% |
| retriever.py | Qdrant 向量检索 + metadata 过滤 | 向量 + BM25 双路 + RRF 融合 + Cross-Encoder 重排 | 40% |
| rag.py | embed → search → sort by score | 完整 pipeline：intent→rewrite→retrieve→rerank→generate | 30% |
| reranker.py | 无操作，直通 | BGE-Reranker-v2-m3 cross-encoder | 0% |
| product_ranker.py | 仅透传 score | 多维加权：semantic×0.4 + price×0.2 + rating×0.15 + sales×0.15 + diversity×0.10 | 10% |
| state_manager.py | 内存 dict | LangGraph Checkpointer (SQLite/PostgreSQL) | 30% |
| embedding.py | BGE-large-zh-v1.5，单例懒加载 | 已符合建议（需加缓存） | 90% |
| llm_client.py | DeepSeek AsyncOpenAI + stream | 已符合建议 | 95% |
| evaluator.py | 空壳 | RAGAS 6 指标 + 5 电商专属指标 + 20+ test cases | 0% |
| comparator.py | 空壳 | LLM 多维度对比表 + 总结 | 0% |
| image_parser.py | 空壳 | Chinese-CLIP + VLM (DeepSeek-VL2) | 0% |
| Qdrant Collection | 单一 text vector | 多向量 (text_embedding + image_embedding) + RRF | 20% |

---

## P0 — 核心链路必做（阻塞 MVP 演示，预计 3-5 天）

### P0-1: agent.py — 实现完整 LangGraph 工作流

现状：只有 StateGraph 骨架 + 硬编码 greeting，没有实际节点。
目标：文档 §7.4 Phase 1（线性 Pipeline）→ Phase 2（StateGraph）
影响范围：agent.py, intent.py, rag.py, product_ranker.py

建议实施：

  a) 在 agent.py 中实现 5 个节点函数：
     - classify_intent_node: 调用 intent.classify_intent（当前已是关键词版，P0 可接受）
     - extract_slots_node: 调用 intent.extract_slots（需从空 dict 升级为 LLM structured output）
     - retrieve_node: 调用 rag.retrieve（当前已有基础实现，直接对接）
     - rank_node: 调用 product_ranker.rank_products
     - generate_node: 调用 llm_client.chat_completion (stream=True)

  b) 构建 StateGraph：
     START → classify_intent → extract_slots → retrieve → rank → generate → END

  c) 条件路由（低优先级但结构预留）：
     在 extract_slots 后检查 slots.missing，非空时走 clarify 分支

  d) SSE 流式输出：
     generate_node 中使用 llm_client.chat_completion(stream=True)
     逐 token yield text_delta 事件
     文本结束后 yield product_cards 事件（包含 ranked_products Top5）

参考：文档 §5.1 完整 Pipeline 架构图 + §5.2 LangGraph 代码实现要点
工期：2-3 天

### P0-2: intent.py — LLM-based slot filling 替换关键词规则

现状：extract_slots 返回全 None 的空 dict，classify_intent 仅有 5 个关键词
目标：文档 §5.1 模块2 Slot Filling schema

建议实施：

  a) 新增 IntentSlots Pydantic model：
     category: str | None
     price_min: float | None
     price_max: float | None
     attributes: dict[str, str]  # {"降噪": "好", "颜色": "黑色"}
     brand_preference: str | None
     missing_slots: list[str]

  b) extract_slots 改为 LLM call（用 existing llm_client + structured output prompt）：
     - System prompt 中定义 slot schema
     - 要求 LLM 以 JSON 格式返回
     - 如果 LLM 判断信息不足，在 missing_slots 中列出

  c) classify_intent 升级为 LLM prompt-based（支持 6 类 intent）：
     commodity_recommend / commodity_compare / commodity_detail /
     scenario_shopping / after_sales / chitchat

  d) rewrite_query 实际实现：
     query + slots → 生成 2-3 个检索变体（文档 §5.1 查询改写）

参考：文档 §5.1 模块1+2 详细 schema 定义
工期：1 天

### P0-3: chat API — 完整 SSE 事件链路

现状：仅返回 greeting text_delta + done
目标：文档 §4.1 豆包流式 UI 的完整 SSE 事件类型

建议实施：

  a) 确认 chat.py 的 SSE 生成器调用 agent.generate_response（当前已正确委托）
  b) agent.generate_response 需要产出的事件序列：
     text_delta × N (流式文本 token) → product_cards (Top5 商品) → done
  c) product_cards 事件格式严格按 sse_events.py 中的 ProductCardEvent
  d) 错误处理：任何节点异常 → error 事件 → done

参考：文档 §4.1 SSE 事件流格式
工期：0.5 天

---

## P1 — 体验闭环（MVP 演示需要，预计 2-4 天）

### P1-1: retriever.py — BM25 关键词检索 + RRF 融合

现状：仅 Qdrant 向量检索一条路径
目标：文档 §5.1 模块3 三路径混合检索 (向量 + 关键词 + 元数据)

建议实施：

  a) 新增 BM25 关键词检索路径：
     - 使用 rank_bm25 库（轻量，无需额外服务）
     - 或使用 PostgreSQL full-text search (to_tsvector)
     - 对商品名 + 描述做关键词匹配

  b) RRF (Reciprocal Rank Fusion) 融合：
     score_rrf = Σ 1/(k + rank_i)  for each path
     融合向量检索 Top50 + BM25 检索 Top50 → 合并 Top100

  c) 异步并行执行：
     asyncio.gather(text_search, keyword_search, metadata_filter)
     减少端到端延迟

参考：文档 §3.3 混合检索伪代码 + §5.3 LlamaIndex QueryFusionRetriever
工期：1 天

### P1-2: reranker.py — Cross-Encoder 重排序

现状：完全无操作
目标：文档 §5.1 模块4 两阶段重排序

建议实施：

  a) 集成 BGE-Reranker-v2-m3（中文最优 cross-encoder）：
     from FlagEmbedding import FlagReranker
     reranker = FlagReranker('BAAI/bge-reranker-v2-m3', use_fp16=True)

  b) 对融合后的 Top100 → rerank → Top20

  c) 可配置：MVP 阶段通过环境变量开关（避免首次下载延迟）

参考：文档 §5.3 LlamaIndex SentenceTransformerRerank
工期：0.5 天

### P1-3: product_ranker.py — 多维加权排序

现状：仅透传 score
目标：文档 §5.1 模块4 业务规则排序公式

建议实施：

  a) 实现加权公式：
     final_score = 0.40 × semantic_score
                 + 0.20 × price_match(budget)     # 越贴近预算越高
                 + 0.15 × rating_score             # 归一化到 [0,1]
                 + 0.15 × sales_volume_score
                 + 0.10 × diversity_bonus          # 避免同品牌扎堆

  b) diversity_bonus: 对已选中品牌的后续商品降权
  c) 权重配置化：支持通过环境变量 / 配置文件调整权重

参考：文档 §5.1 模块4 业务规则排序
工期：0.5 天

### P1-4: state_manager.py — LangGraph Checkpointer 持久化

现状：内存 dict，进程重启丢失
目标：文档 §7.3 "多轮记忆 → LangGraph Checkpointer (PostgreSQL)"

建议实施：

  a) 在 agent StateGraph 编译时启用 checkpointer：
     from langgraph.checkpoint.postgres import PostgresSaver
     memory = PostgresSaver.from_conn_string(settings.DATABASE_URL)
     return workflow.compile(checkpointer=memory)

  b) 保留当前 state_manager.py 作为轻量级辅助（记录偏好标签/预算/已看商品）
  c) chat API 传递 thread_id（= session_id）给 agent 实现多轮

参考：文档 §5.2 LangGraph 最佳实践 "Checkpointer 做多轮记忆"
工期：0.5 天

### P1-5: Qdrant — 多模态 Collection 设计

现状：单一 products collection，仅文本向量
目标：文档 §3.3 Qdrant 多模态 Collection 设计（多向量 + RRF）

建议实施：

  a) 创建 collection 时定义多向量：
     vectors={
         "text_embedding": {size: 1024, distance: "Cosine"},
         "image_embedding": {size: 512, distance: "Cosine"},  # Chinese-CLIP
     }

  b) 商品入库时同时写 text_embedding 和 image_embedding
  c) 搜索时指定 using="text_embedding"（纯文本）或同时搜索两个向量做 RRF
  d) MVP 阶段：先创建多向量 schema，image_embedding 可为空向量（等 P2-4 以图搜图）

参考：文档 §3.3 完整 Python 伪代码
工期：0.5 天

### P1-6: product_ranker — 追问机制 (Clarification)

现状：无追问能力
目标：文档 §2.1 淘宝问问 "追问式导购" + §5.1 Slot Filling clarify 分支

建议实施：

  a) 在 extract_slots 后检查 slots.missing_slots
  b) 如果 missing_slots 非空（如缺少"佩戴方式偏好"）：
     - LLM 生成自然追问："您平时更喜欢入耳式还是头戴式耳机？"
     - 通过 SSE text_delta 发送追问
     - 不发送 product_cards（信息不足不推荐）
     - yield done，等待用户回复
  c) 用户回复后，state_manager 记录上一轮已填槽位，合并新一轮 extract 结果

参考：文档 §5.1 clarify 分支 + §5.2 conditional_edges
工期：1 天

---

## P2 — 全量增强（非阻塞 MVP，按 M6-M10 排期）

### P2-1: 评测体系 — RAGAS + 电商专属指标

现状：evaluator.py 空壳，eval_cases.json 仅 3 条简单用例
目标：文档 §6.1 RAGAS 6 指标 + §6.2 5 电商专属指标

建议实施：

  a) 组装 20+ 评测用例（覆盖 6 类 intent × 3-4 条/类）
  b) 实现自动评测脚本：
     - Context Precision / Recall（需要标注 ground truth product IDs）
     - Faithfulness（逐句检查是否可追溯到商品数据）
     - Answer Relevancy（LLM-as-Judge 打分）
  c) 新增电商专属指标：
     - Recommendation Diversity（品牌/价格熵）
     - Price Sensitivity（超预算比例）
     - Justification Quality（LLM 1-5 打分）
  d) 评测 CLI：python scripts/run_eval.py --dataset data/test_cases/
  e) CI 集成：每次 PR 自动跑评测

参考：文档 §6.1 完整指标表 + §6.2 评测流程
工期：2 天

### P2-2: 多模态 — 以图搜图 + VLM 商品理解

现状：image_parser.py 空壳
目标：文档 §3.2 商品图片理解 pipeline

建议实施：

  a) Phase 1: Chinese-CLIP 以图搜图（轻量本地部署）
     - 商品入库时预计算 image_embedding
     - 用户上传图片 → CLIP encode → Qdrant search(using="image_embedding")
  b) Phase 2: DeepSeek-VL2 商品结构化理解
     - 图片 → VLM → {category, color, style, brand_hint} → 进入意图识别 pipeline

参考：文档 §3.2 VLM 选型对比表
工期：3 天

### P2-3: 商品对比 Agent

现状：comparator.py 空壳
目标：文档 §2.3 Amazon Rufus "比较式对话"

建议实施：

  a) LLM 生成多维度对比表（价格/参数/评分/适用场景）
  b) 前端 CompareScreen 渲染对比卡片
  c) 支持动态维度选择："只看降噪对比"

工期：1.5 天

### P2-4: 知识库 RAG + 评价维度化检索

现状：仅 product collection，无知识库/评价独立检索
目标：文档 §2.2 京东 "评价维度化检索" + §2.3 Amazon "ReviewRAG"

建议实施：

  a) 新增 knowledge Qdrant collection（商品百科/测评文章/FAQ）
  b) 商品数据添加 review_summary 字段（按"音质/佩戴/续航"分段）
  c) retriever 同时检索 products + knowledge 两个 collection
  d) 支持 "这款耳机戴久了耳朵疼吗？" → 检索评价中佩戴舒适度相关片段

工期：2 天

### P2-5: Embedding 缓存 + Qdrant 零停机更新

现状：每次查询重新 encode
目标：文档 §5.3 "产线化注意事项"

建议实施：

  a) 高频 query embedding LRU 缓存（cachetools）
  b) Qdrant collection 别名机制：products_v1 → products（ingest 新数据到 v2，切换别名）

工期：0.5 天

### P2-6: 前端 SSE 流式渲染优化

现状：Android 端待验证
目标：文档 §4.1 豆包流式逐字渲染 + §4.2 商品卡片横向滚动

建议实施：

  a) ChatViewModel._streamingText 增量拼接 + AnimatedString
  b) product_cards 事件 → LazyRow 横向滚动卡片
  c) text_delta 结束后延迟 300ms 再渲染 product_cards（视觉节奏）
  d) 追问 Chip 按钮（suggestions 字段）

工期：2 天

---

## 实施优先级时间线

```
Week 1 (P0): 核心链路打通
  Day 1-2:   P0-2 intent LLM slot filling
  Day 2-4:   P0-1 agent LangGraph 完整工作流
  Day 4-5:   P0-3 SSE 完整事件链路
  里程碑：用户输入 "推荐3000以内降噪耳机" → 流式文本 + Top5 商品卡片

Week 2 (P1): 体验闭环
  Day 1:     P1-1 BM25 + RRF 混合检索
  Day 1-2:   P1-2 BGE-Reranker 重排序
  Day 2:     P1-3 多维加权排序
  Day 3:     P1-5 Qdrant 多模态 Collection
  Day 3-4:   P1-4 Checkpointer 多轮持久化
  Day 4-5:   P1-6 追问机制
  里程碑：多轮对话流畅，追问自然，检索精准度明显提升

Week 3-4 (P2): 全量增强
  P2-1 评测体系 → P2-2 以图搜图 → P2-4 知识库RAG → P2-3 商品对比 → P2-5 缓存 → P2-6 前端优化
```

---

## 产出文件

本清单已保存至:
/mnt/c/Users/fujunye/Desktop/Hermes/04-rag-ecommerce/docs/optimization/PRIORITY_CHECKLIST.md
