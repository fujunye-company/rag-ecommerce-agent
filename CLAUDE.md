# RAG E-Commerce Agent — Claude Code 上下文 v3.0

> 项目根: `04-rag-ecommerce/`  
> GitHub: `git@github.com:fujunye-company/rag-ecommerce-agent.git`  
> 比赛: AI 全栈挑战赛 (第3届) — **最高优先级：`docs/background/REQS-竞赛核心需求.md`**

## 技术栈

| 层 | 技术 |
|----|------|
| 后端 | FastAPI + LangGraph + LlamaIndex |
| 向量库 | Qdrant |
| 数据库 | PostgreSQL + pgvector (async SQLAlchemy) |
| LLM | **Doubao-Seed-2.0-lite** ⚠️ 待切换 (当前 DeepSeek) |
| Embedding | BGE-large-zh-v1.5 |
| 前端 | Kotlin + Jetpack Compose (Android 原生) |
| Python | 3.11 @ ~/.hermes-venv |

### Doubao API
```
Base: https://ark.cn-beijing.volces.com/api/v3/
Model: ep-20260514111645-Imgt2
Key:  见 apps/backend/.env (DOUBAO_API_KEY)
```

## 关键命令

```bash
cd apps/backend && uvicorn app.main:app --reload --host 0.0.0.0 --port 8000
docker compose -f infrastructure/docker-compose.yml up -d
cd apps/backend && python -m pytest tests/ -v
cd apps/backend && python scripts/seed_data.py
```

## 当前里程碑

```
M1 工程启动 ✅  M2 数据基础 ✅  M3 RAG检索 ✅  M4 对话闭环 ✅  M5 MVP验收 🔄
```
**M5 重点**：LLM切换Doubao + 否定语义 + 数据14→50+ + 场景4/5/6(购物车加分)

## 比赛评分权重

| 维度 | 权重 |
|------|:---:|
| 基础功能完整性 | 35% |
| 工程质量 | 25% |
| 效果与可靠性 | 20% |
| 加分项深度 | 20% |

## 严禁项
- 编造不存在的优惠券/功能/价格
- 用纯 Web/H5 客户端
- 泄露 API Key
- 忽略否定语义
