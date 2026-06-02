"""应用配置 — pydantic-settings, 12-Factor App 合规

配置加载优先级（由高到低）:
1. 系统环境变量 (生产部署)
2. .env 文件 (本地开发, gitignored)
3. 代码默认值 (仅非敏感项)

.env 文件从 backend/.env.example 复制后填入真实值。
"""
import os
from pathlib import Path
from pydantic import field_validator
from pydantic_settings import BaseSettings

CONFIG_DIR = Path(__file__).resolve().parents[2]  # backend/

_HF_EMBEDDING = "BAAI/bge-large-zh-v1.5"
_HF_RERANKER = "BAAI/bge-reranker-v2-m3"


def resolve_model_path(local_path: str, hf_name: str) -> str:
    """解析模型路径：本地目录存在则返回本地路径，否则降级为 HF 模型名"""
    if os.path.isdir(local_path):
        return local_path
    return hf_name


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
    # 默认: <backend>/data/models/bge-large-zh-v1.5
    # 不存在时自动降级为 BAAI/bge-large-zh-v1.5 (HF)
    EMBEDDING_MODEL: str = str(Path(__file__).resolve().parents[2] / "data" / "models" / "bge-large-zh-v1.5")

    # ── Reranker ──
    # 默认: <backend>/data/models/bge-reranker-v2-m3
    # 不存在时自动降级为 BAAI/bge-reranker-v2-m3 (HF)
    RERANKER_MODEL: str = str(Path(__file__).resolve().parents[2] / "data" / "models" / "bge-reranker-v2-m3")

    # ── 数据导入 ──
    AUTO_IMPORT_DATA: bool = True

    # ── 服务 ──
    LOG_LEVEL: str = "INFO"
    APP_HOST: str = "0.0.0.0"
    APP_PORT: int = 8080
    CORS_ORIGINS: list[str] = ["*"]
    MAX_UPLOAD_SIZE_MB: int = 10

    @field_validator("EMBEDDING_MODEL", mode="after")
    @classmethod
    def _resolve_embedding(cls, v: str) -> str:
        return resolve_model_path(v, _HF_EMBEDDING)

    @field_validator("RERANKER_MODEL", mode="after")
    @classmethod
    def _resolve_reranker(cls, v: str) -> str:
        return resolve_model_path(v, _HF_RERANKER)

    model_config = {
        "env_file": str(CONFIG_DIR / ".env"),
        "env_file_encoding": "utf-8",
        "extra": "ignore",
    }


settings = Settings()
