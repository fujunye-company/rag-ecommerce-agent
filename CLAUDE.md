# 电商AI导购系统 — 项目上下文

## 技术栈

| 层 | 技术 |
|----|------|
| 客户端 | Android Kotlin + Jetpack Compose |
| 后端API | Python FastAPI + SSE 流式 (sse-starlette) |
| Agent编排 | LangGraph StateGraph |
| RAG检索 | LlamaIndex + Qdrant |
| 向量数据库 | Qdrant (Docker, :6333) |
| 关系数据库 | PostgreSQL 16 + pgvector (Docker, :5432) |
| LLM | DeepSeek v4-pro / v4-flash |
| Python环境 | ~/.hermes-venv |
| 依赖管理 | uv (pyproject.toml) |

## 项目结构

```
02-source-code/
├── backend/          Python FastAPI 后端
│   ├── app/
│   │   ├── main.py          FastAPI 入口, lifespan, /health, CORS
│   │   ├── api/             chat, products, upload, evaluation
│   │   ├── schemas/         Pydantic: chat.py, product.py
│   │   ├── core/            config.py, database.py
│   │   ├── models/          SQLAlchemy ORM
│   │   └── services/        Agent/RAG/LLM 业务层
│   ├── alembic/             数据库迁移
│   └── requirements.txt
├── android/           Kotlin Compose 前端
│   └── app/src/main/java/com/shopping/agent/
│       ├── ui/chat/         聊天界面
│       ├── ui/components/   ProductCard 组件
│       ├── data/            Repository + Model
│       └── viewmodel/       ChatViewModel
└── scripts/           工具脚本
```

## 关键命令

```bash
# 启动 Docker 服务 (PostgreSQL + Qdrant)
docker compose up -d

# 启动后端
cd 02-source-code/backend && uvicorn app.main:app --reload --port 8000

# 安装依赖
cd 02-source-code/backend && uv pip install -r requirements.txt

# 数据库迁移
cd 02-source-code/backend && alembic upgrade head
```

## API 接口

```
GET  /health                   健康检查（含数据库连通性）
POST /api/chat                 SSE 流式问答
POST /api/chat/stream          SSE 流式问答（备选路径）
GET  /api/products             商品列表 (?category=&min_price=&max_price=)
GET  /api/products/{id}        商品详情
POST /api/feedback             用户反馈 ({session_id, product_id, rating, reason})
POST /api/upload/image         图片上传（预留）
POST /api/evaluation/run       评测（预留）
```

## SSE 事件类型

```
text_delta    → {"type": "text_delta", "content": "..."}
product_cards → {"type": "product_cards", "products": [...]}
done          → {"type": "done"}
error         → {"type": "error", "message": "...", "code": "..."}
```

## Hermes ↔ Claude Code 协作模式

Hermes 作为项目总控，Claude Code 作为自主执行器：
- Hermes 分发任务 → Claude Code 自主处理 → 返回结构化报告
- Claude Code 可以 Read/Write/Edit/Bash/WebSearch
- 任务完成后输出报告格式：任务结论 → 修改文件列表 → 验证结果 → 下一步建议
