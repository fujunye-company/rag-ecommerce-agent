# 电商AI导购Agent — 开发总纲 DEV-GUIDE v3.0

> **最高优先级参考：`docs/background/REQS-竞赛核心需求.md`**  
> 本文档 = PRD + 竞品分析 + 架构分析 + 创新调研 + UI设计 + 比赛需求 的压缩版  
> 每次编码前必读。冲突时以 REQS.md 为准。

---

## 一、系统目标（来自比赛题目）

```
"意图理解 → 智能咨询 → 决策辅助 → 交易执行" 全链路闭环

Android(Kotlin/Compose) ← SSE → FastAPI ← LangGraph → Qdrant/PostgreSQL → Doubao
```

**7 级场景覆盖**：单轮推荐 → 多轮追问 → 对比决策 → 反选排除 → 场景化组合 → 购物车下单 → 拍照找货

---

## 二、技术栈（比赛指定）

| 层 | 技术 | 备注 |
|----|------|------|
| 客户端 | Android Kotlin + Jetpack Compose | 原生，不接受 Web |
| 后端 | FastAPI + Uvicorn | |
| Agent | LangGraph StateGraph | |
| RAG | LlamaIndex + Qdrant | |
| 数据库 | PostgreSQL + pgvector | |
| LLM | **Doubao-Seed-2.0-lite** ⚠️ 待切换 | 比赛提供 Key（当前仍为 DeepSeek） |
| Embedding | BGE-large-zh-v1.5 | |
| 向量库 | Qdrant | |

### Doubao API

```
Base: https://ark.cn-beijing.volces.com/api/v3/
Model: ep-20260514111645-Imgt2
Key:  ark-2af51d30-ed70-4061-a2cd-74f454ccc4e8-2282e
Limit: TPM 807K, RPM 700
```

---

## 三、架构设计

```
LangGraph StateGraph:
  START
    ↓
  classify_intent   → 6类意图 + 否定条件检测
    ↓
  extract_slots     → 结构化槽位 (含否定属性)
    ↓  [missing_slots非空]
  clarify           → 主动追问Chip
    ↓  [slots完整]
  retrieve          → Qdrant向量检索 + metadata过滤 + 否定条件排除
    ↓
  rank              → 向量粗排 → LLM精排 → 动态权重
    ↓
  generate          → SSE stream: text_delta×N → product_cards → done
    ↓
  [cart 分支]       → 购物车CRUD (加购/删除/修改/下单)
    ↓
  [image 分支]      → 图像理解 → 相似商品检索
    ↓
  END
```

---

## 四、代码级创新约束

### 4.1 否定语义处理（比赛核心考察点）
```
必须: intent 检测否定关键词: "不要"、"除了"、"非"、"不含"、"拒绝"
必须: extract_slots 输出 exclude_attributes: ["含酒精", "日系品牌"]
必须: retriever 排除命中 exclude_attributes 的商品
```

### 4.2 购物车 CRUD（场景6 — 比赛加分项，非阻塞）

> ⚠️ 加分项目标，不影响场景 1-3 交付。

```
加分项: 识别 cart_add/cart_remove/cart_modify/cart_checkout 意图
加分项: 维护 session 级购物车状态
加分项: 客户端实时展示购物车数量/状态
```

### 4.3 agent.py → generate_node
```
禁止: "为您推荐以下几款：1. xxx 2. xxx 3. xxx"
必须: 三段式理由 + 自信度 + 引用
禁止: 编造不存在的优惠券/功能/价格（比赛红线）
```

### 4.4 intent.py → classify_intent
```
必须: LLM prompt-based 分类 (6类 + cart + image)
必须: 否定条件检测 ("不要"/"除了"/"非")
必须: 信息不足时主动追问
```

### 4.5 性能要求
```
首 Token < 1s (Prompt压缩 + 流水线并行)
SSE 流式速率 ≥ 20 tokens/s
检索延迟 < 2s
```

---

## 五、评测指标（比赛评分）

| 维度 | 权重 | 指标 |
|------|:---:|------|
| 基础功能完整性 | 35% | 7场景中至少场景1-3跑通 |
| 工程质量 | 25% | 代码结构清晰、错误处理完善、文档齐全 |
| 效果与可靠性 | 20% | 无幻觉、检索准确、流畅美观 |
| 加分项深度 | 20% | 购物车/多模态/性能优化/否定语义 |

---

## 六、绝对禁止（红线）

```
❌ 编造不存在的优惠券/功能/价格（比赛直接扣分）
❌ 用纯 Web/H5 客户端
❌ 泄露 API Key
❌ 忽略否定语义（"不要""除了""非"）
❌ generate_node 输出纯列表
❌ intent 只用品类规则
❌ slots 不全时强行推荐
❌ 一次性生成 >3 个文件的代码
```

---

## 七、模块完成度现状

| 模块 | 完成度 | M5 还需 |
|------|:---:|------|
| agent.py | 85% | 否定语义 + cart 意图分支 |
| intent.py | 80% | 否定条件检测 + exclude_attributes |
| retriever.py | 90% | 否定条件排除过滤 |
| product_service.py | 100% | — |
| state_manager.py | 100% | cart state 扩展 |
| chat.py | 100% | — |
| 购物车模块 | 0% | **全新** |
| 多模态模块 | 0% | **全新** |
| LLM 切换 | 0% | DeepSeek → Doubao |

---

> 详细展开: `docs/background/REQS-竞赛核心需求.md` (最高优先级)  
> 进度跟踪: `docs/progress/开发进度控制表.md`
