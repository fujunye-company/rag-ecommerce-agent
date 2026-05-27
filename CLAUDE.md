# RAG E-Commerce Agent — Claude Code 上下文 v4.0

> 项目根: `04-rag-ecommerce/`  
> GitHub: `git@github.com:fujunye-company/rag-ecommerce-agent.git`  
> 比赛: AI 全栈挑战赛 (第3届) — **最高优先级：`docs/background/REQS-竞赛核心需求.md`**

## 技术栈

| 层 | 技术 |
|----|------|
| 后端 | FastAPI + LangGraph + LlamaIndex |
| 向量库 | Qdrant (1024-dim, bge-large-v1.5) |
| 数据库 | PostgreSQL + pgvector (async SQLAlchemy) |
| LLM | **Doubao-Seed-2.0-lite** ⚠️ Key待验证 (当前 DeepSeek 降级) |
| Embedding | BGE-large-zh-v1.5 |
| 前端 | Kotlin + Jetpack Compose (Android 原生, 33 kt) |
| Python | 3.11 @ ~/.hermes-venv |

### Doubao API
```
Base: https://ark.cn-beijing.volces.com/api/v3/
Model: ep-20260514111645-lmgt2
Key:  见 apps/backend/.env (DOUBAO_API_KEY)
```

## 关键命令

```bash
cd apps/backend && uvicorn app.main:app --reload --host 0.0.0.0 --port 8000
docker compose -f infrastructure/docker-compose.yml up -d
cd apps/backend/data/qdrant && python ingest_to_qdrant.py
cd apps/backend/data/qdrant && python retrieve_from_qdrant.py --test
```

## 当前里程碑

```
M1 ✅  M2 ✅  M3 ✅  M4 ✅  M5 ⚠️  M6 ✅  M7 ⚠️  M8 ⚠️  M9 ✅  M10 🔜
```

**M10 冲刺重点**：
- P0: 豆包Key验证 → Android编译 → 前后端联调 (4.5h)
- P1: clarify追问节点 → VLM拍照找货 → RAGAS评测 (5h)
- P2: 答辩PPT → 演示视频 → 打包交付 (4.5h)

## 场景完成度 (9场景 × 后端)

| 1 | 2 | 3 | 4 | 5 | 6 | 7 | 8 | 9 |
|:-:|:-:|:-:|:-:|:-:|:-:|:-:|:-:|:-:|
| ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |

## 比赛评分权重

| 维度 | 权重 | 当前估计 |
|------|:---:|:--:|
| 基础功能完整性 | 35% | ~20% |
| 工程质量 | 25% | ~18% |
| 效果与可靠性 | 20% | ~5% |
| 加分项深度 | 20% | ~10% |
| **合计** | **100%** | **~53%** |

## 严禁项
- 编造不存在的优惠券/功能/价格
- 用纯 Web/H5 客户端
- 泄露 API Key
- 忽略否定语义
