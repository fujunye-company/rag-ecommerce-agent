# 系统架构 — AI 全栈挑战赛

> 参考：`docs/background/REQS-竞赛核心需求.md`

---

## 整体架构

```
┌─────────────────────────────────────────────────────────┐
│  Android (Kotlin + Jetpack Compose)                     │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌───────────┐ │
│  │ ChatScreen│ │ProductCard│ │CartWidget│ │ImagePicker│ │
│  └──────────┘ └──────────┘ └──────────┘ └───────────┘ │
│         │            │            │            │        │
│         └────────────┴─────┬──────┴────────────┘        │
│                     SSE Client (OkHttp)                  │
└──────────────────────────┬──────────────────────────────┘
                           │ SSE (text_delta / product_cards / cart_update / done)
┌──────────────────────────┴──────────────────────────────┐
│  FastAPI Backend                                        │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌───────────┐ │
│  │ /chat    │ │/products │ │/feedback │ │ /cart     │ │
│  └────┬─────┘ └──────────┘ └──────────┘ └───────────┘ │
│       │                                                  │
│  ┌────┴──────────────────────────────────────────────┐  │
│  │ LangGraph Agent (StateGraph)                       │  │
│  │  classify_intent → extract_slots → retrieve        │  │
│  │       → rank → generate → [cart branch]            │  │
│  └────────────────────┬───────────────────────────────┘  │
│                       │                                   │
│  ┌──────────┐ ┌───────┴──────┐ ┌───────────────────┐   │
│  │ Qdrant   │ │ PostgreSQL   │ │ Doubao LLM API    │   │
│  │ (向量库) │ │ (会话/商品)   │ │ (Seed-2.0-lite)   │   │
│  └──────────┘ └──────────────┘ └───────────────────┘   │
└─────────────────────────────────────────────────────────┘
```

---

## 技术选型

| 层 | 选型 | 理由 |
|----|------|------|
| 客户端 | Android Kotlin + Compose | 比赛要求原生 |
| 后端框架 | FastAPI | 异步 + SSE 原生支持 |
| Agent 编排 | LangGraph | StateGraph + conditional edges |
| 向量库 | Qdrant | 高性能 + REST API |
| 数据库 | PostgreSQL + pgvector | 会话持久化 + 向量扩展 |
| LLM | Doubao-Seed-2.0-lite (目标) | 比赛提供 Key，当前 DeepSeek → 待切换 |
| Embedding | BGE-large-zh-v1.5 | 中文电商领域 SOTA |

---

## 数据流

```
用户输入 → SSE → FastAPI
  → intent 分类 (Doubao LLM)
  → slot 提取 (含否定条件)
  → BGE embedding → Qdrant 向量检索 + metadata过滤
  → 重排序 (语义+业务双塔)
  → Doubao LLM 生成推荐理由
  → SSE stream: text_delta → product_cards → done
  → Android 流式渲染 + 商品卡片展示
```

---

## 目录映射（比赛提交要求）

> 比赛要求 `/client`、`/server`、`/docs`。当前工程使用 `apps/` 前缀。
> 提交时通过符号链接映射：`ln -s apps/backend server && ln -s apps/android client`

## 模块分层

```
apps/backend/app/
├── api/          路由层 (chat/products/cart/feedback)
├── core/         基础设施 (config/database/security)
├── models/       SQLAlchemy ORM (Product/Session/Cart)
├── schemas/      Pydantic (ProductCard/ChatRequest)
└── services/     业务逻辑 (agent/intent/retriever/cart)

apps/android/app/src/main/java/com/shopping/agent/
├── ui/           Compose (ChatScreen/ProductCard/CartWidget)
├── data/         Repository + API Client
└── viewmodel/    StateFlow (ChatViewModel/CartViewModel)
```
