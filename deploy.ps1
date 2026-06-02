# RAG E-Commerce Agent — 一键部署脚本 (Windows PowerShell)
# 用法: .\deploy.ps1           # 启动全部服务
#       .\deploy.ps1 -Stop     # 停止全部服务

param([switch]$Stop)

$ErrorActionPreference = "Stop"
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $ScriptDir

$ComposeFile = "infrastructure/docker-compose.yml"
$EnvExample = "infrastructure/env/.env.docker.example"
$EnvFile = "infrastructure/env/.env.docker"

function Write-Step { param($msg) Write-Host "[DEPLOY] $msg" -ForegroundColor Green }
function Write-Warn  { param($msg) Write-Host "[WARN]  $msg" -ForegroundColor Yellow }
function Write-Err   { param($msg) Write-Host "[ERROR] $msg" -ForegroundColor Red }

# ── Stop mode ──────────────────────────────────────────────
if ($Stop) {
    Write-Step "Stopping all services..."
    docker compose -f $ComposeFile down
    Write-Step "All services stopped."
    exit 0
}

# ── Phase 0: Preflight checks ─────────────────────────────
Write-Step "Phase 0: Preflight checks"

# Docker daemon
$dockerInfo = docker info 2>&1
if ($LASTEXITCODE -ne 0) {
    Write-Err "Docker is not running. Please start Docker Desktop first."
    exit 1
}
Write-Host "       Docker: running"

# Env file — auto-copy from example if missing
if (-not (Test-Path $EnvFile)) {
    if (Test-Path $EnvExample) {
        Write-Step "Creating $EnvFile from template..."
        Copy-Item $EnvExample $EnvFile
        Write-Warn "Edit $EnvFile to set DOUBAO_API_KEY before using LLM features."
    } else {
        Write-Err "Env template not found: $EnvExample"
        exit 1
    }
}

# Check DOUBAO_API_KEY
$envContent = Get-Content $EnvFile -Raw
if ($envContent -notmatch 'DOUBAO_API_KEY=(?!your_doubao_key_here|$)\S+') {
    Write-Warn "DOUBAO_API_KEY is not configured in $EnvFile"
    Write-Warn "LLM calls will fail until you set a valid API key."
}

# Secret scan
if (Test-Path "scripts/secret_scan.py") {
    Write-Step "Running secret scan..."
    python scripts/secret_scan.py 2>&1 | Out-Host
}

# ── Phase 1: Start infrastructure ────────────────────────
Write-Step "Phase 1: Starting infrastructure"
docker compose -f $ComposeFile up -d --build

# ── Phase 2: Wait for ready ──────────────────────────────
Write-Step "Phase 2: Waiting for backend to be ready..."
$maxWait = 600
$elapsed = 0
$interval = 3

while ($elapsed -lt $maxWait) {
    try {
        $response = Invoke-WebRequest -Uri "http://localhost:8080/ready" -TimeoutSec 3 -UseBasicParsing
        $readyJson = $response.Content | ConvertFrom-Json
        $statusMsg = "[$elapsed" + "s] phase=$($readyJson.status) items=$($readyJson.progress.qdrant_item_count)"
        Write-Host "`r       $statusMsg" -NoNewline
        if ($readyJson.status -eq "ready") {
            Write-Host ""
            break
        }
    } catch {
        Write-Host "`r       [$elapsed" + "s] waiting for backend to start..." -NoNewline
    }
    Start-Sleep -Seconds $interval
    $elapsed += $interval
}

if ($elapsed -ge $maxWait) {
    Write-Err "Backend did not become ready within ${maxWait}s. Check logs:"
    Write-Err "  docker compose -f $ComposeFile logs backend"
    exit 1
}

# ── Phase 3: Readiness report ────────────────────────────
Write-Step "Phase 3: Readiness report"

try {
    $healthJson = (Invoke-WebRequest -Uri "http://localhost:8080/health" -TimeoutSec 5 -UseBasicParsing).Content | ConvertFrom-Json
    $readyJson = (Invoke-WebRequest -Uri "http://localhost:8080/ready" -TimeoutSec 5 -UseBasicParsing).Content | ConvertFrom-Json
} catch {
    Write-Err "Cannot reach backend health endpoint"
    exit 1
}

Write-Host ""
Write-Host "============================================================"
Write-Host "  DEPLOY READINESS REPORT"
Write-Host "============================================================"
Write-Host "  Backend:      running (port 8080)"
Write-Host "  PostgreSQL:   $($healthJson.database)"
Write-Host "  Qdrant:       $($healthJson.qdrant) ($($healthJson.collection), $($readyJson.progress.qdrant_item_count) vectors, $($healthJson.vector_size)-dim)"
Write-Host "  LLM:          Doubao-Seed-2.0-lite"
Write-Host ""
Write-Host "  API Docs:     http://localhost:8080/docs"
Write-Host "  Health:       http://localhost:8080/health"
Write-Host "  Ready:        http://localhost:8080/ready"
Write-Host "  Android APK:  cd apps/android; .\gradlew.bat assembleDebug -PapiUrl=http://<YOUR-IP>:8080"
Write-Host ""
Write-Host "  Demo script:  docs/submission/DEMO_RUNBOOK.md"
Write-Host "============================================================"

Write-Step "Deploy complete. System is ready for demo."
