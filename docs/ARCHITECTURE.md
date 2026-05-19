# 系统架构说明

## 架构图

```
┌─────────────────────────────────────────────────┐
│                   Android 客户端                  │
│  Kotlin + Jetpack Compose + OkHttp SSE          │
└──────────────────┬──────────────────────────────┘
                   │ HTTP / SSE
┌──────────────────▼──────────────────────────────┐
│              FastAPI 后端 (:8000)                 │
│  ┌─────────┐ ┌──────────┐ ┌──────────────────┐  │
│  │ API 层   │ │ Agent 层  │ │ RAG 层           │  │
│  │ chat.py  │ │ LangGraph │ │ LlamaIndex       │  │
│  │ products │ │ intent    │ │ retriever        │  │
│  │ feedback │ │ rank      │ │ embedding        │  │
│  └─────────┘ └──────────┘ └──────────────────┘  │
└──────┬──────────────┬───────────────────────────┘
       │              │
┌──────▼──────┐ ┌─────▼──────┐ ┌──────────────────┐
│ PostgreSQL  │ │  Qdrant    │ │  DeepSeek API    │
│ + pgvector  │ │  :6333     │ │  (LLM)           │
│ :5432       │ │            │ │                  │
└─────────────┘ └────────────┘ └──────────────────┘
```

## 数据流

```
用户文字输入 → intent(意图识别) → rewrite(查询改写)
→ retriever(向量+关键词混合检索) → rank(商品排序)
→ LLM(生成回答) → SSE stream → Android 渲染
```

## 技术选型

| 组件 | 技术 | 原因 |
|------|------|------|
| 后端框架 | FastAPI | 异步原生, SSE 支持, Pydantic 生态 |
| Agent | LangGraph | 状态图编排, 可扩展节点 |
| RAG | LlamaIndex + Qdrant | 混合检索, 企业级向量库 |
| LLM | DeepSeek | 中文理解强, 性价比高 |
| 数据库 | PostgreSQL + pgvector | 结构化+向量混合查询 |
| 前端 | Kotlin Compose | 原生 Android, 声明式 UI |
