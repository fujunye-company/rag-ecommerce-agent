#!/bin/bash
# RAG E-Commerce Agent — 一键部署脚本 (Linux / WSL / macOS)
# 用法: ./deploy.sh          # 启动全部服务
#       ./deploy.sh --stop   # 停止全部服务

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR"

COMPOSE_FILE="infrastructure/docker-compose.yml"
ENV_EXAMPLE="infrastructure/env/.env.docker.example"
ENV_FILE="infrastructure/env/.env.docker"
BACKEND_URL="${BACKEND_URL:-http://localhost:8080}"
BACKEND_PORT="${BACKEND_URL##*:}"
BACKEND_PORT="${BACKEND_PORT%%/*}"
export APP_PORT="${APP_PORT:-$BACKEND_PORT}"
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

log()  { echo -e "${GREEN}[DEPLOY]${NC} $*"; }
warn() { echo -e "${YELLOW}[WARN]${NC}  $*"; }
err()  { echo -e "${RED}[ERROR]${NC} $*"; }
info() { echo -e "${CYAN}       $*${NC}"; }

# ── Stop mode ──────────────────────────────────────────────
if [ "${1:-}" = "--stop" ]; then
    log "Stopping all services..."
    docker compose -f "$COMPOSE_FILE" down
    log "All services stopped."
    exit 0
fi

# ── Phase 0: Preflight checks ─────────────────────────────
log "Phase 0: Preflight checks"

# Docker daemon
if ! docker info >/dev/null 2>&1; then
    err "Docker is not running. Please start Docker Desktop first."
    exit 1
fi
info "Docker: running"

# Model prefetch hint (optional, speeds up first boot)
MODEL_DIR="apps/backend/data/models/bge-large-zh-v1.5"
if [ -d "$MODEL_DIR" ] && [ -f "$MODEL_DIR/config.json" ]; then
    info "Model: pre-downloaded (local), first boot will be fast"
else
    warn "Model not pre-downloaded. First boot will download ~1.3GB."
    info "  Tip: python scripts/prefetch_model.py --all  to pre-download now"
fi

# docker compose plugin
if ! docker compose version >/dev/null 2>&1; then
    err "docker compose plugin not found. Please install Docker Compose v2."
    exit 1
fi

# Port checks
for port in "$BACKEND_PORT" 5433 6333; do
    if command -v ss >/dev/null 2>&1; then
        if ss -tlnp 2>/dev/null | grep -q ":${port} "; then
            warn "Port ${port} is in use — may conflict"
        fi
    elif command -v lsof >/dev/null 2>&1; then
        if lsof -i ":${port}" >/dev/null 2>&1; then
            warn "Port ${port} is in use — may conflict"
        fi
    fi
done

# Env file — auto-copy from example if missing
if [ ! -f "$ENV_FILE" ]; then
    if [ -f "$ENV_EXAMPLE" ]; then
        log "Creating $ENV_FILE from template..."
        cp "$ENV_EXAMPLE" "$ENV_FILE"
        warn "Edit $ENV_FILE to set DOUBAO_API_KEY before using LLM features."
    else
        err "Env template not found: $ENV_EXAMPLE"
        exit 1
    fi
fi

# Check DOUBAO_API_KEY is set
DOUBAO_KEY=$(grep -E '^DOUBAO_API_KEY=' "$ENV_FILE" | sed 's/DOUBAO_API_KEY=//')
if [ -z "$DOUBAO_KEY" ] || [ "$DOUBAO_KEY" = "your_doubao_key_here" ]; then
    warn "DOUBAO_API_KEY is not configured in $ENV_FILE"
    warn "LLM calls will fail until you set a valid API key."
fi

# Secret scan
if [ -f "scripts/secret_scan.py" ]; then
    log "Running secret scan..."
    python scripts/secret_scan.py || true
fi

# ── Phase 1: Start infrastructure ────────────────────────
log "Phase 1: Starting infrastructure"
docker compose -f "$COMPOSE_FILE" up -d --build

# ── Phase 2: Wait for ready ──────────────────────────────
log "Phase 2: Waiting for backend to be ready..."
MAX_WAIT=${MAX_WAIT:-900}  # 15 minutes (first boot may download HF models)
ELAPSED=0
INTERVAL=3
MODEL_SOURCE_SHOWN=""

while [ $ELAPSED -lt $MAX_WAIT ]; do
    READY_JSON=$(curl -s "$BACKEND_URL/ready" 2>/dev/null)
    HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" "$BACKEND_URL/ready" 2>/dev/null || echo "000")

    if [ "$HTTP_CODE" = "200" ] || [ "$HTTP_CODE" = "503" ]; then
        STATUS=$(echo "$READY_JSON" | python -c "import sys,json; print(json.load(sys.stdin).get('status','?'))" 2>/dev/null || echo "?")
        ITEMS=$(echo "$READY_JSON" | python -c "import sys,json; print(json.load(sys.stdin)['progress']['qdrant_item_count'])" 2>/dev/null || echo "0")
        MSG=$(echo "$READY_JSON" | python -c "import sys,json; print(json.load(sys.stdin).get('message',''))" 2>/dev/null || echo "")
        MODEL_SRC=$(echo "$READY_JSON" | python -c "import sys,json; print(json.load(sys.stdin)['progress'].get('model_source',''))" 2>/dev/null || echo "")
        MODEL_PCT=$(echo "$READY_JSON" | python -c "import sys,json; print(json.load(sys.stdin)['progress'].get('model_download_pct',0))" 2>/dev/null || echo "0")

        # Show model source once
        if [ -n "$MODEL_SRC" ] && [ "$MODEL_SRC" != "$MODEL_SOURCE_SHOWN" ]; then
            MODEL_SOURCE_SHOWN="$MODEL_SRC"
            case "$MODEL_SRC" in
                local)   info "Model: local path (no download needed)" ;;
                cache)   info "Model: HF cache hit (no download needed)" ;;
                download) info "Model: downloading from ${HF_ENDPOINT:-HF} (resumable)..." ;;
            esac
        fi

        # Show progress line
        if [ "$STATUS" = "downloading_model" ]; then
            echo -ne "\r       [$ELAPSED""s] Downloading model... ${MODEL_PCT}%                    "
        elif [ "$STATUS" = "seeding" ]; then
            echo -ne "\r       [$ELAPSED""s] Vectorizing products... ${ITEMS} done                "
        elif [ "$STATUS" = "warming_reranker" ]; then
            echo -ne "\r       [$ELAPSED""s] Warming reranker...                                 "
        else
            echo -ne "\r       [$ELAPSED""s] $MSG                              "
        fi

        if [ "$STATUS" = "ready" ]; then
            echo ""
            break
        fi
    else
        echo -ne "\r       [$ELAPSED""s] waiting for backend to start...                    "
    fi
    sleep $INTERVAL
    ELAPSED=$((ELAPSED + INTERVAL))
done

if [ $ELAPSED -ge $MAX_WAIT ]; then
    err "Backend did not become ready within ${MAX_WAIT}s. Check logs:"
    err "  docker compose -f $COMPOSE_FILE logs backend"
    exit 1
fi

# ── Phase 3: Readiness report ────────────────────────────
log "Phase 3: Readiness report"

HEALTH_JSON=$(curl -s "$BACKEND_URL/health" 2>/dev/null || echo '{"status":"unreachable"}')
READY_JSON=$(curl -s "$BACKEND_URL/ready" 2>/dev/null || echo '{}')

echo ""
echo "============================================================"
echo "  DEPLOY READINESS REPORT"
echo "============================================================"

# Parse health
DB_STATUS=$(echo "$HEALTH_JSON" | python -c "import sys,json; print(json.load(sys.stdin).get('database','?'))" 2>/dev/null || echo "?")
QDRANT_STATUS=$(echo "$HEALTH_JSON" | python -c "import sys,json; print(json.load(sys.stdin).get('qdrant','?'))" 2>/dev/null || echo "?")
COLLECTION=$(echo "$HEALTH_JSON" | python -c "import sys,json; print(json.load(sys.stdin).get('collection','?'))" 2>/dev/null || echo "?")
VEC_SIZE=$(echo "$HEALTH_JSON" | python -c "import sys,json; print(json.load(sys.stdin).get('vector_size',0))" 2>/dev/null || echo "?")
ITEM_COUNT=$(echo "$READY_JSON" | python -c "import sys,json; print(json.load(sys.stdin)['progress']['qdrant_item_count'])" 2>/dev/null || echo "?")

echo "  Backend:      running ($BACKEND_URL)"
echo "  PostgreSQL:   $DB_STATUS"
echo "  Qdrant:       $QDRANT_STATUS ($COLLECTION, $ITEM_COUNT vectors, ${VEC_SIZE}-dim)"
echo "  LLM:          Doubao-Seed-2.0-lite"
echo ""
echo "  API Docs:     $BACKEND_URL/docs"
echo "  Health:       $BACKEND_URL/health"
echo "  Ready:        $BACKEND_URL/ready"

# Validate data import actually produced vectors
if [ "$ITEM_COUNT" = "0" ] || [ "$ITEM_COUNT" = "?" ]; then
    echo ""
    warn "=============================================================="
    warn "  QDRANT HAS 0 VECTORS — RAG retrieval will return empty results."
    warn "  Check: docker compose -f $COMPOSE_FILE logs backend | grep -iE 'auto-import|error'"
    warn "=============================================================="
fi

# Detect LAN IP for Android connection
LAN_IP=$(ip route get 1 2>/dev/null | awk '{print $7; exit}' 2>/dev/null || ifconfig 2>/dev/null | awk '/inet / && !/127.0.0.1/ {print $2; exit}' || hostname -I 2>/dev/null | awk '{print $1}' || echo "")
if [ -n "$LAN_IP" ]; then
    echo "  Android APK:  cd apps/android && ./gradlew assembleDebug -PapiUrl=http://$LAN_IP:$BACKEND_PORT"
else
    echo "  Android APK:  cd apps/android && ./gradlew assembleDebug -PapiUrl=http://<YOUR-IP>:$BACKEND_PORT"
fi
echo ""
echo "  Demo script:  docs/submission/DEMO_RUNBOOK.md"
echo "============================================================"

log "Deploy complete. System is ready for demo."
