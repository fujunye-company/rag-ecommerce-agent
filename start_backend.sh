#!/bin/bash
# RAG E-Commerce Agent — 后端启动脚本
# 用法: source ~/.hermes-venv/bin/activate && bash start_backend.sh

set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR/apps/backend"

# 检查 Embedding 模型是否已缓存，未缓存时允许自动下载
MODEL_CACHE="$HOME/.cache/huggingface/hub/models--BAAI--bge-large-zh-v1.5"
if [ -d "$MODEL_CACHE" ]; then
    export HF_HUB_OFFLINE=1
    export TRANSFORMERS_OFFLINE=1
else
    echo "[WARN] Embedding model not cached at $MODEL_CACHE"
    echo "       Allowing download (first boot may take 5-15 min)."
    echo "       Pre-download: see docs/standards/SETUP.md Section 5"
fi

uvicorn app.main:app --host 0.0.0.0 --port 8080
