# 拾物 — 基于 RAG 的多模态电商智能导购 AI Agent

> **AI 全栈挑战赛 (第3届)  · 2026**  
> 基于 RAG + LangGraph + Doubao 的智能导购系统  
> 最高优先级需求：`docs/background/REQS-竞赛核心需求.md

## 队员

| 队员 | 职责 |
|------|------|
| 傅钧烨 | Agent 框架设计、主线模块实现 |
| 唐荣炜 | 真机测试反馈、外包模块、演示视频录制 |
| 周芯仪 | 市场与需求分析 |

## 项目概述

"意图理解 → 智能咨询 → 决策辅助 → 交易执行" 全链路闭环。
覆盖 9 级导购场景（推荐→筛选→追问→对比→反问→反选→场景化→购物车→拍照找货）。

## 技术栈

| 层 | 技术 |
|----|------|
| Agent 编排 | LangGraph StateGraph (10 节点) |
| RAG 检索引擎 | LlamaIndex + Qdrant (1024-dim) |
| 后端服务 | FastAPI (SSE 流式, 8 事件类型) |
| 数据库 | PostgreSQL + pgvector |
| LLM | Doubao-Seed-2.0-lite |
| Embedding | BGE-large-zh-v1.5 |
| 前端 | Kotlin + Jetpack Compose (72 .kt) |

## 项目结构

```
rag-ecommerce-agent/
├── apps/
│   ├── backend/          FastAPI 后端 (62 .py, 29 modules)
│   └── android/          Kotlin Compose Android (72 .kt)
├── docs/                 项目文档 (26 .md)
├── infrastructure/       docker-compose + env
└── README.md
```

## 里程碑 (M0-M10)

```
M0 ✅  M1 ✅  M2 ✅  M3 ✅  M4 ✅  M5 ✅  M6 ✅  M7 ✅  M8 ✅  M9 ✅  M10 ✅
```

详见 `docs/progress/M0-M10-全项目规划.md`

## 9 场景完成度

| 级 | 场景 | 后端 | Android | 联调 |
|:--:|------|:--:|:--:|:--:|
| 1 | 单轮推荐 | ✅ | ✅ | ✅ |
| 2 | 条件筛选 | ✅ | ✅ | ✅ |
| 3 | 多轮追问 | ✅ | ✅ | ✅ |
| 4 | 对比决策 | ✅ | ✅ | ✅ |
| 5 | Agent 主动反问 | ✅ | ✅ | ✅ |
| 6 | 反选排除 | ✅ | ✅ | ✅ |
| 7 | 场景化组合 | ✅ | ✅ | ✅ |
| 8 | 购物车下单 | ✅ | ✅ | ✅ |
| 9 | 拍照找货 | ✅ | ✅ | ✅ |

## 快速开始

> **新人从零搭建** → 完整指南：`docs/standards/SETUP.md`

```bash
# 已配置好环境？三步跑起来：
docker compose -f infrastructure/docker-compose.yml up -d postgres qdrant   # 启动基础设施
cd apps/backend && python -c "from app.startup import ensure_qdrant_data; import asyncio; asyncio.run(ensure_qdrant_data())"  # 数据入库
cd apps/backend && uvicorn app.main:app --reload --host 0.0.0.0 --port 8080  # 启动后端
```

## 相关文档

### 竞赛核心

- [竞赛核心需求](docs/background/REQS-竞赛核心需求.md) — 最高优先级，评分权重与交付标准
- [比赛题目要求](docs/background/核心要求-比赛题目.md)
- [PRD 产品需求文档](docs/background/PRD-电商AI导购Agent-V1.0.md)
- [课题说明会纪要](docs/standards/requirements-based-rag-multimodal-ecommerce-ai-agent.md)

### 架构与设计

- [系统架构](docs/ARCHITECTURE.md)
- [API 文档](docs/API.md)
- [项目结构说明](docs/architecture/项目结构说明.md)
- [核心机制](docs/standards/MECHANISM.md)
- [数据契约](docs/standards/DATA-CONTRACT.md)
- [UI 设计规范](docs/standards/DESIGN.md)

### 开发与部署

- [从零搭建指南](docs/standards/SETUP.md)
- [开发总纲](docs/standards/DEV-GUIDE.md)
- [开发规约](docs/standards/开发规约-v2.md)

### 评测与性能

- [评测报告](docs/EVALUATION.md)
- [性能基准](docs/notes/PERFORMANCE.md)
- [TTFT 延迟基准](docs/optimization/TTFT_BENCHMARK.md)

### 答辩与演示

- [演示脚本](docs/DEMO_SCRIPT.md)
- [提交演示手册](docs/submission/DEMO_RUNBOOK.md)
- [答辩 PPT 大纲](docs/notes/PPT-OUTLINE.md)

### 调研与加分项

- [Agent 框架架构分析](docs/background/Agent框架架构分析.md)
- [竞品案例分析](docs/background/电商RAG导购Agent案例分析.md)
- [学术文献补充](docs/background/PRD-背景资料-学术文献补充.md)
- [创新研究](docs/optimization/INNOVATION-RESEARCH.md)

### 项目管理

- [M0-M10 全项目规划](docs/progress/M0-M10-全项目规划.md)
- [开发进度控制表](docs/progress/开发进度控制表.md)
- [变更日志](docs/CHANGELOG.md)
