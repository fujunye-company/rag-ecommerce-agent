# RAG 电商 AI 导购 Agent

> **AI 全栈挑战赛 (第3届) —  2026**  
> 基于 RAG + LangGraph + Doubao 的智能导购系统  
> 最高优先级需求：`docs/background/REQS-竞赛核心需求.md`

## 项目概述

"意图理解 → 智能咨询 → 决策辅助 → 交易执行" 全链路闭环。
覆盖 9 级导购场景（推荐→筛选→追问→对比→反问→反选→场景化→购物车→拍照找货）。

## 技术栈

| 层 | 技术 | 状态 |
|----|------|:--:|
| Agent 编排 | LangGraph StateGraph (10 节点) | ✅ |
| RAG 检索引擎 | LlamaIndex + Qdrant (1024-dim) | ✅ |
| 后端服务 | FastAPI (SSE 流式, 6 事件类型) | ✅ |
| 数据库 | PostgreSQL + pgvector | ✅ |
| LLM | Doubao-Seed-2.0-lite | ✅ 已验证 |
| Embedding | BGE-large-zh-v1.5 | ✅ |
| 前端 | Kotlin + Jetpack Compose (35+ kt) | ✅ 代码就绪 |

## 项目结构

```
04-rag-ecommerce/
├── apps/
│   ├── backend/          FastAPI 后端 (62 .py, 29 modules)
│   └── android/          Kotlin Compose Android (36 .kt, 7 页面)
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
| 1 | 单轮推荐 | ✅ | ✅ | ❓ |
| 2 | 条件筛选 | ✅ | ✅ | ❓ |
| 3 | 多轮追问 | ✅ | ✅ | ❓ |
| 4 | 对比决策 | ✅ | ✅ | ❓ |
| 5 | Agent 主动反问 | ✅ | ✅ | ❓ |
| 6 | 反选排除 | ✅ | ✅ | ❓ |
| 7 | 场景化组合 | ⚠️ | ✅ | ❓ |
| 8 | 购物车下单 | ✅ | ✅ | ❓ |
| 9 | 拍照找货 | ✅ | ✅ | ❌ |

> 场景 1-6,8-9 全栈代码就绪（7/9），场景 7 后端部分完成。联调待 APK 编译后执行。

## 快速开始

> **新人从零搭建** → 完整指南：`docs/standards/SETUP.md`（克隆→配置→模型→Docker→入库→启动→Android）

```bash
# 已配置好环境？三步跑起来：
docker compose -f infrastructure/docker-compose.yml up -d   # 启动基础设施
cd apps/backend/data/qdrant && python ingest_to_qdrant.py   # 数据入库
cd .. && uvicorn app.main:app --reload --host 0.0.0.0 --port 8080  # 启动后端
```

## 相关文档

- [从零搭建指南](docs/standards/SETUP.md) — **新人必读**
- [竞赛核心需求](docs/background/REQS-竞赛核心需求.md) — **最高优先级**
- [M0-M10 全项目规划](docs/progress/M0-M10-全项目规划.md)
- [开发总纲](docs/standards/DEV-GUIDE.md)
- [开发进度控制表](docs/progress/开发进度控制表.md)
- [演示脚本](docs/DEMO_SCRIPT.md)
- [评测报告](docs/EVALUATION.md)
