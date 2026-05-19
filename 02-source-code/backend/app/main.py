"""
FastAPI应用入口 — 电商AI导购系统
"""
import logging
from contextlib import asynccontextmanager

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import RedirectResponse
from sqlalchemy import text

from app.api import chat, products, upload, evaluation
from app.core.config import settings
from app.core.database import engine, Base

# ── 日志配置 ──────────────────────────────────────────────
logging.basicConfig(
    level=getattr(logging, settings.LOG_LEVEL, logging.INFO),
    format="%(asctime)s [%(levelname)s] %(name)s: %(message)s",
    datefmt="%Y-%m-%d %H:%M:%S",
)
logger = logging.getLogger("main")


# ── Lifespan ──────────────────────────────────────────────
@asynccontextmanager
async def lifespan(app: FastAPI):
    """应用生命周期：启动时建表，关闭时断开数据库"""
    # startup
    try:
        async with engine.begin() as conn:
            await conn.run_sync(Base.metadata.create_all)
        logger.info("数据库表创建/验证完成")
    except Exception as exc:
        logger.error("数据库初始化失败: %s", exc)
        raise
    yield
    # shutdown
    try:
        await engine.dispose()
        logger.info("数据库连接已关闭")
    except Exception as exc:
        logger.error("数据库关闭异常: %s", exc)


# ── FastAPI app ───────────────────────────────────────────
app = FastAPI(
    title="电商AI导购系统",
    version="0.1.0",
    lifespan=lifespan,
)

# CORS — MVP 阶段不传 credentials（避免与 wildcard origin 冲突）
app.add_middleware(
    CORSMiddleware,
    allow_origins=settings.CORS_ORIGINS,
    allow_credentials=False,  # MVP: 移动端 SSE 无需 cookie
    allow_methods=["*"],
    allow_headers=["*"],
)

# 路由注册
# TODO: 全量阶段考虑 /api/v1 版本化前缀以支持 breaking change 共存
app.include_router(chat.router, prefix="/api", tags=["chat"])
app.include_router(products.router, prefix="/api", tags=["products"])
app.include_router(upload.router, prefix="/api", tags=["upload"])
app.include_router(evaluation.router, prefix="/api", tags=["evaluation"])


# ── 健康检查 ──────────────────────────────────────────────
@app.get("/health")
async def health():
    """健康检查：验证应用 + 数据库连通性"""
    db_status = "connected"
    try:
        async with engine.connect() as conn:
            await conn.execute(text("SELECT 1"))
    except Exception:
        db_status = "unavailable"

    healthy = db_status == "connected"
    return {
        "status": "ok" if healthy else "degraded",
        "version": "0.1.0",
        "database": db_status,
    }, 200 if healthy else 503


# ── 版本信息 ──────────────────────────────────────────────
@app.get("/version")
async def version():
    """返回系统版本及技术栈信息"""
    return {
        "name": "电商AI导购系统",
        "version": "0.1.0",
        "tech_stack": {
            "backend": "FastAPI",
            "llm": "DeepSeek",
        },
    }


# ── 根路径 ────────────────────────────────────────────────
@app.get("/")
async def root():
    """根路径：重定向到 /docs 便于开发调试"""
    return RedirectResponse(url="/docs")
