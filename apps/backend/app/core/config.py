"""应用配置 — pydantic-settings, 12-Factor App 合规

配置加载优先级（由高到低）:
1. 系统环境变量 (生产部署)
2. .env 文件 (本地开发, gitignored)
3. 代码默认值 (仅非敏感项)

.env 文件从 backend/.env.example 复制后填入真实值。
"""
from pathlib import Path
from pydantic_settings import BaseSettings

CONFIG_DIR = Path(__file__).resolve().parents[2]  # backend/


class Settings(BaseSettings):
    """应用配置"""

    # ── 环境标识 ──
    APP_ENV: str = "development"

    # ── 数据库 (必填，无默认值) ──
    DATABASE_URL: str = ""

    # ── Qdrant ──
    QDRANT_URL: str = "http://localhost:6333"
    QDRANT_COLLECTION: str = "products"

    # ── LLM (必填) ──
    # Doubao (豆包) — 比赛指定，优先使用
    DOUBAO_API_KEY: str = ""
    DOUBAO_BASE_URL: str = "https://ark.cn-beijing.volces.com/api/v3/"
    LLM_MODEL: str = "ep-20260514111645-lmgt2"
    # DeepSeek — 降级回退
    DEEPSEEK_API_KEY: str = ""
    DEEPSEEK_BASE_URL: str = "https://api.deepseek.com/v1"
    DEEPSEEK_MODEL: str = "deepseek-chat"

    # ── Embedding ──
    EMBEDDING_MODEL: str = "BAAI/bge-large-zh-v1.5"

    # ── 服务 ──
    LOG_LEVEL: str = "INFO"
    APP_HOST: str = "0.0.0.0"
    APP_PORT: int = 8080
    CORS_ORIGINS: list[str] = ["*"]
    MAX_UPLOAD_SIZE_MB: int = 10

    model_config = {
        "env_file": str(CONFIG_DIR / ".env"),
        "env_file_encoding": "utf-8",
        "extra": "ignore",
    }


settings = Settings()
