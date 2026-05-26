#!/bin/bash
# RAG E-Commerce Agent — 后端启动脚本
# 设置 HF 离线模式，避免 reranker 每次 70s 超时

cd /mnt/c/Users/fujunye/Desktop/Hermes/04-rag-ecommerce/apps/backend

export HF_HUB_OFFLINE=1
export TRANSFORMERS_OFFLINE=1

~/.hermes-venv/bin/uvicorn app.main:app --host 0.0.0.0 --port 8000
