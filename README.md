# RAG 电商 AI 导购 Agent

> **AI 全栈挑战赛 (第3届) — 华南理工大学 2026**  
> 基于 RAG + LangGraph + Doubao 的智能导购系统  
> 最高优先级需求：`docs/background/REQS-竞赛核心需求.md`

## 项目概述

以"意图理解 → 智能咨询 → 决策辅助 → 交易执行"全链路闭环为核心，覆盖 7 级导购场景（推荐→追问→对比→反选→场景化→购物车→拍照找货）。

## 技术栈

| 层 | 技术 |
|----|------|
| Agent 编排 | LangGraph StateGraph |
| RAG 检索引擎 | LlamaIndex + Qdrant 向量库 |
| 后端服务 | FastAPI (SSE 流式输出) |
| 数据库 | PostgreSQL + pgvector |
| LLM | Doubao-Seed-2.0-lite |
| Embedding | BGE-large-zh-v1.5 |
| 前端 | Kotlin + Jetpack Compose (Android 原生) |

## 项目结构

```
04-rag-ecommerce/
├── apps/
│   ├── backend/          FastAPI 后端服务
│   └── android/          Kotlin Compose Android 客户端
├── docs/
│   ├── background/       REQS 竞赛需求 + PRD + 调研
│   ├── architecture/     架构设计 + UI 方案
│   ├── optimization/     创新研究 + 优先级清单
│   ├── progress/         进度报告 + 任务分配
│   ├── standards/        开发规约
│   └── workflow/         工作流文档
├── infrastructure/       docker-compose + env
└── README.md

> **比赛提交时**：`ln -s apps/backend server && ln -s apps/android client` 映射为 `/server` `/client` `/docs`
```

## 7 级场景

| 级 | 场景 | 状态 |
|:--:|------|:---:|
| 1 | 单轮推荐 | ✅ |
| 2 | 多轮追问细化 | ✅ |
| 3 | 对比决策 | ✅ |
| 4 | 反选/排除约束 | ❌ |
| 5 | 场景化组合推荐 | ⚠️ |
| 6 | 购物车与下单 | ❌ |
| 7 | 拍照找货 | ❌ |

## 快速开始

```bash
make install        # 安装后端依赖
make docker-up      # 启动 PostgreSQL + Qdrant
make seed           # 导入种子商品数据
make dev            # 启动开发服务器 (localhost:8000)
make test           # 运行测试
```

## 相关文档

- [竞赛核心需求](docs/background/REQS-竞赛核心需求.md) — **最高优先级**
- [开发总纲](docs/standards/DEV-GUIDE.md)
- [API 文档](docs/API.md)
- [演示脚本](docs/DEMO_SCRIPT.md)
- [进度控制表](docs/progress/开发进度控制表.md)
