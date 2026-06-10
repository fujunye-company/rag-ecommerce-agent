# RAG E-Commerce Agent — 一键部署脚本 (Windows PowerShell)
# 用法: .\deploy.ps1              # 启动全部服务（检测已运行时跳过）
#       .\deploy.ps1 -Build        # 启动 + 强制重建 backend 镜像
#       .\deploy.ps1 -Stop         # 停止全部服务

param([switch]$Stop, [switch]$Build)

$ErrorActionPreference = "Stop"
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $ScriptDir

$ComposeFile = "infrastructure/docker-compose.yml"
$EnvExample = "infrastructure/env/.env.docker.example"
$EnvFile = "infrastructure/env/.env.docker"
$BackendUrl = if ($env:BACKEND_URL) { $env:BACKEND_URL } else { "http://localhost:8080" }
$BackendPort = ([Uri]$BackendUrl).Port
if (-not $env:APP_PORT) { $env:APP_PORT = "$BackendPort" }

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

# Model prefetch hint
$modelDir = "apps\backend\data\models\bge-large-zh-v1.5"
if ((Test-Path $modelDir) -and (Test-Path "$modelDir\config.json")) {
    Write-Host "       Model: pre-downloaded (local), first boot will be fast"
} else {
    Write-Warn "Model not pre-downloaded. First boot will download ~1.3GB."
    Write-Host "       Tip: see docs/standards/SETUP.md Section 5 to pre-download models"
}

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
    Write-Warn "请联系比赛主办方获取 Doubao API Key，然后编辑 $EnvFile 填入。"
}

# ── Phase 0.5: Check if already running healthy ───────────
$runningCount = (docker compose -f $ComposeFile ps --format json 2>$null | ForEach-Object {
    if ($_ -match '"Health":"healthy"') { 1 }
}).Count
$totalSvcs = 3
if ($runningCount -ge $totalSvcs) {
    Write-Step "All $totalSvcs services already running and healthy."
    if ($Build) {
        Write-Warn "-Build flag set: recreating backend image..."
        docker compose -f $ComposeFile build backend
        docker compose -f $ComposeFile up -d backend
    }
    # Skip to Phase 3
} else {

# ── Phase 1: Start infrastructure ────────────────────────
Write-Step "Phase 1: Starting infrastructure"
if ($Build) {
    docker compose -f $ComposeFile up -d --build
} else {
    docker compose -f $ComposeFile up -d
}

# ── Phase 2: Wait for ready ──────────────────────────────
Write-Step "Phase 2: Waiting for backend to be ready..."
$maxWait = if ($env:MAX_WAIT) { [int]$env:MAX_WAIT } else { 900 }
$elapsed = 0
$interval = 3

$modelSourceShown = ""
while ($elapsed -lt $maxWait) {
    try {
        $response = Invoke-WebRequest -Uri "$BackendUrl/ready" -TimeoutSec 3 -UseBasicParsing
        $readyJson = $response.Content | ConvertFrom-Json
        $status = $readyJson.status
        $items = $readyJson.progress.qdrant_item_count
        $modelSrc = $readyJson.progress.model_source
        $modelPct = $readyJson.progress.model_download_pct

        if ($modelSrc -and $modelSrc -ne $modelSourceShown) {
            $modelSourceShown = $modelSrc
            switch ($modelSrc) {
                "local"    { Write-Host "       Model: local path (no download needed)" }
                "cache"    { Write-Host "       Model: HF cache hit (no download needed)" }
                "download" { Write-Host "       Model: downloading from HF (resumable)..." }
            }
        }

        if ($status -eq "downloading_model") {
            Write-Host "`r       [$elapsed" + "s] Downloading model... ${modelPct}%" -NoNewline
        } elseif ($status -eq "seeding") {
            Write-Host "`r       [$elapsed" + "s] Vectorizing products... ${items} done" -NoNewline
        } elseif ($status -eq "warming_reranker") {
            Write-Host "`r       [$elapsed" + "s] Warming reranker..." -NoNewline
        } else {
            Write-Host "`r       [$elapsed" + "s] $($readyJson.message)" -NoNewline
        }

        if ($status -eq "ready") {
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

}  # end of "services not already running" block

# ── Phase 3: Readiness report ────────────────────────────
Write-Step "Phase 3: Readiness report"

try {
    $healthJson = (Invoke-WebRequest -Uri "$BackendUrl/health" -TimeoutSec 5 -UseBasicParsing).Content | ConvertFrom-Json
    $readyJson = (Invoke-WebRequest -Uri "$BackendUrl/ready" -TimeoutSec 5 -UseBasicParsing).Content | ConvertFrom-Json
} catch {
    Write-Err "Cannot reach backend health endpoint"
    exit 1
}

Write-Host ""
Write-Host "============================================================"
Write-Host "  DEPLOY READINESS REPORT"
Write-Host "============================================================"
Write-Host "  Backend:      running ($BackendUrl)"
Write-Host "  PostgreSQL:   $($healthJson.database)"
Write-Host "  Qdrant:       $($healthJson.qdrant) ($($healthJson.collection), $($readyJson.progress.qdrant_item_count) vectors, $($healthJson.vector_size)-dim)"
Write-Host "  LLM:          Doubao-Seed-2.0-lite"
Write-Host ""
Write-Host "  API Docs:     $BackendUrl/docs"
Write-Host "  Health:       $BackendUrl/health"
Write-Host "  Ready:        $BackendUrl/ready"

# Validate data import produced vectors
$itemCount = $readyJson.progress.qdrant_item_count
if ($itemCount -eq 0) {
    Write-Host ""
    Write-Warn "=============================================================="
    Write-Warn "  QDRANT HAS 0 VECTORS -- RAG retrieval will return empty results."
    Write-Warn "  Check: docker compose -f $ComposeFile logs backend | Select-String 'auto-import|error'"
    Write-Warn "=============================================================="
}

Write-Host "  Android APK:  cd apps/android; .\gradlew.bat assembleDebug -PapiUrl=http://<YOUR-IP>:$BackendPort"
Write-Host ""
Write-Host "  Demo script:  docs/submission/DEMO_RUNBOOK.md"
Write-Host "============================================================"

Write-Step "Deploy complete. System is ready for demo."
