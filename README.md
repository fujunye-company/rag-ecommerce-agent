# RAG 电商 AI 导购 Agent

> **AI 全栈挑战赛 (第3届) — 华南理工大学 2026**  
> 基于 RAG + LangGraph + Doubao 的智能导购系统  
> 最高优先级需求：`docs/background/REQS-竞赛核心需求.md`

## 项目概述

"意图理解 → 智能咨询 → 决策辅助 → 交易执行" 全链路闭环。
覆盖 9 级导购场景（推荐→筛选→追问→对比→反问→反选→场景化→购物车→拍照找货）。

## 技术栈

| 层 | 技术 | 状态 |
|----|------|:--:|
| Agent 编排 | LangGraph StateGraph | ✅ |
| RAG 检索引擎 | LlamaIndex + Qdrant (1024-dim) | ✅ |
| 后端服务 | FastAPI (SSE 流式输出) | ✅ |
| 数据库 | PostgreSQL + pgvector | ✅ |
| LLM | Doubao-Seed-2.0-lite | ⚠️ Key待验证 |
| Embedding | BGE-large-zh-v1.5 | ✅ |
| 前端 | Kotlin + Jetpack Compose (33 kt) | ❓ 待联调 |

## 项目结构

```
04-rag-ecommerce/
├── apps/
│   ├── backend/          FastAPI 后端 (62 .py, 29 modules)
│   └── android/          Kotlin Compose Android (33 .kt)
├── docs/                 30 份文档
├── infrastructure/       docker-compose + env
├── client → apps/android
├── server → apps/backend
└── README.md
```

## 里程碑 (M0-M10)

```
M0 ✅  M1 ✅  M2 ✅  M3 ✅  M4 ✅  M5 ⚠️  M6 ✅  M7 ⚠️  M8 ⚠️  M9 ✅  M10 🔜
```

详见 `docs/progress/M0-M10-全项目规划.md`

## 9 场景完成度

| 级 | 场景 | 后端 | Android | 联调 |
|:--:|------|:--:|:--:|:--:|
| 1 | 单轮推荐 | ✅ | ❓ | ❓ |
| 2 | 条件筛选 | ✅ | ❓ | ❓ |
| 3 | 多轮追问 | ✅ | ❓ | ❓ |
| 4 | 对比决策 | ✅ | ❓ | ❓ |
| 5 | Agent 主动反问 | ❌ | ❌ | ❌ |
| 6 | 反选排除 | ✅ | ❓ | ❓ |
| 7 | 场景化组合 | ⚠️ | ❓ | ❓ |
| 8 | 购物车下单 | ⚠️ | ❓ | ❓ |
| 9 | 拍照找货 | ❌ | ⚠️ | ❌ |

## 快速开始

```bash
make docker-up      # 启动 PostgreSQL + Qdrant
cd apps/backend/data/qdrant && python ingest_to_qdrant.py  # 入库
make dev            # 启动开发服务器 (localhost:8000)
```

## 相关文档

- [竞赛核心需求](docs/background/REQS-竞赛核心需求.md) — **最高优先级**
- [M0-M10 全项目规划](docs/progress/M0-M10-全项目规划.md)
- [开发总纲](docs/standards/DEV-GUIDE.md)
- [开发进度控制表](docs/progress/开发进度控制表.md)
- [演示脚本](docs/DEMO_SCRIPT.md)
- [评测报告](docs/EVALUATION.md)
