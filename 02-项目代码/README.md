# AI全栈挑战赛 — 电商AI导购系统

## 技术栈

| 层 | 技术 |
|----|------|
| 客户端 | Android Kotlin + Jetpack Compose |
| 后端API | Python FastAPI + SSE流式 |
| Agent编排 | LangGraph StateGraph |
| RAG检索 | LlamaIndex + Qdrant |
| 向量数据库 | Qdrant (Docker) |
| 关系数据库 | PostgreSQL 16 + pgvector |
| LLM | DeepSeek v4-pro / v4-flash |

## 项目结构

```
02-项目代码/
├── backend/          # FastAPI后端
│   ├── app/
│   │   ├── main.py          # 应用入口
│   │   ├── api/             # 路由(chat/products/upload/evaluation)
│   │   ├── schemas/         # Pydantic模型
│   │   ├── core/            # 配置/数据库/中间件
│   │   ├── models/          # SQLAlchemy ORM
│   │   └── services/        # Agent/RAG/Embedding服务
│   ├── requirements.txt
│   └── alembic/             # 数据库迁移
├── android/          # Android客户端
│   └── app/src/main/java/com/shopping/agent/
│       ├── ui/chat/         # 聊天界面
│       ├── ui/components/   # ProductCard等组件
│       ├── data/            # Repository+Model
│       └── viewmodel/       # ChatViewModel
└── scripts/          # 工具脚本
```

## 环境依赖

- Python 3.11.15 venv: `~/.hermes-venv/`
- Docker Desktop: PostgreSQL 16 + Qdrant
- DeepSeek API Key: 已配置(.env)
- Claude Code v2.1.144: 已配置(DeepSeek后端)

## 快速启动

```bash
# 1. 启动数据库
docker compose up -d

# 2. 启动后端
cd backend && uvicorn app.main:app --reload --port 8000

# 3. Android客户端在Android Studio中打开android/目录编译运行
```
