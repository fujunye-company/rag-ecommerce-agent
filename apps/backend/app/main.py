"""
FastAPI应用入口 — 电商AI导购系统
"""
import asyncio
import sys

if sys.platform == "win32":
    asyncio.set_event_loop_policy(asyncio.WindowsSelectorEventLoopPolicy())

import logging
from contextlib import asynccontextmanager

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import RedirectResponse
from sqlalchemy import text

from app.api import chat, products, upload, evaluation, feedback, compare, knowledge, cart
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
    """应用生命周期：启动时建表+预热模型，关闭时断开数据库"""
    # startup
    if settings.DATABASE_URL:
        try:
            async with engine.begin() as conn:
                await conn.run_sync(Base.metadata.create_all)
            logger.info("数据库表创建/验证完成")
        except Exception as exc:
            logger.error("数据库初始化失败: %s", exc)
            raise
    else:
        logger.info("DATABASE_URL 未配置，跳过数据库初始化（内存模式）")

    # 预热 Reranker 模型（同步，阻塞启动直到就绪）
    try:
        from app.services.reranker import _get_model
        import asyncio as _asyncio
        await _asyncio.to_thread(_get_model)
        logger.info("Reranker model warmed up")
    except Exception as e:
        logger.warning("Reranker warmup skipped: %s", e)

    # 清空旧缓存 — 确保 top_k 等参数变更后不返回过期数据
    from app.services import cache
    await cache.clear()
    logger.info("Query cache cleared on startup")

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


# ── 异常处理器 ──────────────────────────────────────────────
from app.core.exceptions import AppException
from app.schemas.common import ApiResponse
from fastapi import Request
from fastapi.responses import JSONResponse

@app.exception_handler(AppException)
async def app_exception_handler(request: Request, exc: AppException):
    """统一应用异常 → {code, message, data}"""
    return JSONResponse(
        status_code=exc.status_code,
        content=ApiResponse(
            code=exc.detail["code"] if isinstance(exc.detail, dict) else 5000,
            message=exc.detail["message"] if isinstance(exc.detail, dict) else str(exc.detail),
            data=None,
        ).model_dump(),
    )

@app.exception_handler(Exception)
async def unhandled_exception_handler(request: Request, exc: Exception):
    """兜底异常处理器 — 不暴露堆栈"""
    import logging
    logger = logging.getLogger("main")
    logger.error("Unhandled exception: %s", exc, exc_info=True)
    return JSONResponse(
        status_code=500,
        content=ApiResponse(code=5000, message="服务器内部错误", data=None).model_dump(),
    )

# 路由注册 — /api/v1 为主要版本，/api 保留向后兼容
# TODO: 全量阶段废弃 /api 前缀，仅保留 /api/v1
for prefix in ["/api/v1", "/api"]:
    app.include_router(chat.router, prefix=prefix, tags=["chat"])
    app.include_router(products.router, prefix=prefix, tags=["products"])
    app.include_router(upload.router, prefix=prefix, tags=["upload"])
    app.include_router(evaluation.router, prefix=prefix, tags=["evaluation"])
    app.include_router(feedback.router, prefix=prefix, tags=["feedback"])
    app.include_router(compare.router, prefix=prefix, tags=["compare"])
    app.include_router(knowledge.router, prefix=prefix, tags=["knowledge"])
    app.include_router(cart.router, prefix=prefix, tags=["cart"])


# ── 健康检查 ──────────────────────────────────────────────
@app.get("/health")
async def health():
    """健康检查：验证应用 + 数据库 + Qdrant 连通性"""
    import httpx
    
    db_status = "connected"
    try:
        async with engine.connect() as conn:
            await conn.execute(text("SELECT 1"))
    except Exception:
        db_status = "unavailable"
    
    # Qdrant 连通性检查
    qdrant_status = "unknown"
    collection_name = ""
    vector_size = 0
    try:
        async with httpx.AsyncClient(timeout=3.0) as client:
            r = await client.get(f"{settings.QDRANT_URL}/collections/{settings.QDRANT_COLLECTION}")
            if r.status_code == 200:
                data = r.json()
                qdrant_status = "ok"
                collection_name = settings.QDRANT_COLLECTION
                vector_size = data.get("result", {}).get("config", {}).get("params", {}).get("vectors", {}).get("size", 0)
            else:
                qdrant_status = f"error: HTTP {r.status_code}"
    except Exception as e:
        qdrant_status = f"unavailable: {str(e)[:50]}"
    
    healthy = db_status == "connected"
    return {
        "status": "ok" if healthy else "degraded",
        "version": "0.1.0",
        "database": db_status,
        "qdrant": qdrant_status,
        "collection": collection_name,
        "vector_size": vector_size,
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
            "llm": "Doubao-Seed-2.0-lite",
        },
    }


# ── 缓存管理 ──────────────────────────────────────────────
@app.post("/api/cache/clear")
async def clear_cache():
    """清空查询缓存 — 参数变更后使用"""
    from app.services import cache
    st = await cache.stats()
    await cache.clear()
    logger.info("Cache cleared via API (was %d entries)", st["size"])
    return {"status": "ok", "cleared": st["size"]}


# ── 根路径 ────────────────────────────────────────────────
@app.get("/")
async def root():
    """根路径：重定向到 /docs 便于开发调试"""
    return RedirectResponse(url="/docs")
 
