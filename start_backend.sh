#!/bin/bash
# RAG E-Commerce Agent — 后端启动脚本
# 用法: source ~/.hermes-venv/bin/activate && bash start_backend.sh

set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR/apps/backend"

export HF_HUB_OFFLINE=1
export TRANSFORMERS_OFFLINE=1

uvicorn app.main:app --host 0.0.0.0 --port 8080
