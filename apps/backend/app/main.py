     1|"""
     2|FastAPI应用入口 — 电商AI导购系统
     3|"""
     4|import logging
     5|from contextlib import asynccontextmanager
     6|
     7|from fastapi import FastAPI
     8|from fastapi.middleware.cors import CORSMiddleware
     9|from fastapi.responses import RedirectResponse
    10|from sqlalchemy import text
    11|
    12|from app.api import chat, products, upload, evaluation, feedback, compare, knowledge
    13|from app.core.config import settings
    14|from app.core.database import engine, Base
    15|
    16|# ── 日志配置 ──────────────────────────────────────────────
    17|logging.basicConfig(
    18|    level=getattr(logging, settings.LOG_LEVEL, logging.INFO),
    19|    format="%(asctime)s [%(levelname)s] %(name)s: %(message)s",
    20|    datefmt="%Y-%m-%d %H:%M:%S",
    21|)
    22|logger = logging.getLogger("main")
    23|
    24|
    25|# ── Lifespan ──────────────────────────────────────────────
    26|@asynccontextmanager
    27|async def lifespan(app: FastAPI):
    28|    """应用生命周期：启动时建表，关闭时断开数据库"""
    29|    # startup
    30|    try:
    31|        async with engine.begin() as conn:
    32|            await conn.run_sync(Base.metadata.create_all)
    33|        logger.info("数据库表创建/验证完成")
    34|    except Exception as exc:
    35|        logger.error("数据库初始化失败: %s", exc)
    36|        raise
    37|    yield
    38|    # shutdown
    39|    try:
    40|        await engine.dispose()
    41|        logger.info("数据库连接已关闭")
    42|    except Exception as exc:
    43|        logger.error("数据库关闭异常: %s", exc)
    44|
    45|
    46|# ── FastAPI app ───────────────────────────────────────────
    47|app = FastAPI(
    48|    title="电商AI导购系统",
    49|    version="0.1.0",
    50|    lifespan=lifespan,
    51|)
    52|
    53|# CORS — MVP 阶段不传 credentials（避免与 wildcard origin 冲突）
    54|app.add_middleware(
    55|    CORSMiddleware,
    56|    allow_origins=settings.CORS_ORIGINS,
    57|    allow_credentials=False,  # MVP: 移动端 SSE 无需 cookie
    58|    allow_methods=["*"],
    59|    allow_headers=["*"],
    60|)
    61|
    62|
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
    63|# TODO: 全量阶段废弃 /api 前缀，仅保留 /api/v1
    64|for prefix in ["/api/v1", "/api"]:
    65|    app.include_router(chat.router, prefix=prefix, tags=["chat"])
    66|    app.include_router(products.router, prefix=prefix, tags=["products"])
    67|    app.include_router(upload.router, prefix=prefix, tags=["upload"])
    68|    app.include_router(evaluation.router, prefix=prefix, tags=["evaluation"])
    69|    app.include_router(feedback.router, prefix=prefix, tags=["feedback"])
    70|    app.include_router(compare.router, prefix=prefix, tags=["compare"])
    71|    app.include_router(knowledge.router, prefix=prefix, tags=["knowledge"])
    72|
    73|
    74|# ── 健康检查 ──────────────────────────────────────────────
    75|@app.get("/health")
    76|async def health():
    77|    """健康检查：验证应用 + 数据库连通性"""
    78|    db_status = "connected"
    79|    try:
    80|        async with engine.connect() as conn:
    81|            await conn.execute(text("SELECT 1"))
    82|    except Exception:
    83|        db_status = "unavailable"
    84|
    85|    healthy = db_status == "connected"
    86|    return {
    87|        "status": "ok" if healthy else "degraded",
    88|        "version": "0.1.0",
    89|        "database": db_status,
    90|    }, 200 if healthy else 503
    91|
    92|
    93|# ── 版本信息 ──────────────────────────────────────────────
    94|@app.get("/version")
    95|async def version():
    96|    """返回系统版本及技术栈信息"""
    97|    return {
    98|        "name": "电商AI导购系统",
    99|        "version": "0.1.0",
   100|        "tech_stack": {
   101|            "backend": "FastAPI",
   102|            "llm": "DeepSeek",
   103|        },
   104|    }
   105|
   106|
   107|# ── 根路径 ────────────────────────────────────────────────
   108|@app.get("/")
   109|async def root():
   110|    """根路径：重定向到 /docs 便于开发调试"""
   111|    return RedirectResponse(url="/docs")
   112|