@echo off
setlocal EnableExtensions

cd /d "%~dp0"
set "BACKEND_DIR=%cd%"
for %%I in ("%BACKEND_DIR%\..\..") do set "ROOT_DIR=%%~fI"
set "COMPOSE_FILE=%ROOT_DIR%\infrastructure\docker-compose.yml"

if not defined PORT set "PORT=8080"
set "APP_PORT=%PORT%"
set "DATABASE_URL=postgresql+asyncpg://shopping:shopping123@localhost:5433/shopping_agent"
set "QDRANT_URL=http://localhost:6333"
if not defined HF_ENDPOINT set "HF_ENDPOINT=https://hf-mirror.com"

echo ===========================================
echo   RAG E-Commerce Local One-Click Launcher
echo ===========================================
echo Backend dir: %BACKEND_DIR%
echo Port:        %PORT%
echo.

call :find_python
if errorlevel 1 exit /b 1

if "%~1"=="--check" (
    call :check_docker
    if errorlevel 1 exit /b 1
    call :ensure_env
    call :check_python_deps
    if errorlevel 1 exit /b 1
    call :check_models
    echo.
    echo [OK] Local launcher preflight passed.
    exit /b 0
)

call :check_docker
if errorlevel 1 exit /b 1

call :ensure_env

echo [1/6] Pulling database containers if needed...
docker compose -f "%COMPOSE_FILE%" pull postgres qdrant
if errorlevel 1 (
    echo [ERROR] Failed to pull database containers.
    pause
    exit /b 1
)

echo.
echo [2/6] Starting PostgreSQL and Qdrant...
docker compose -f "%COMPOSE_FILE%" up -d postgres qdrant
if errorlevel 1 (
    echo [ERROR] Failed to start database containers.
    pause
    exit /b 1
)

call :wait_container_healthy shopping-pg 90
if errorlevel 1 exit /b 1
call :wait_container_healthy shopping-qdrant 90
if errorlevel 1 exit /b 1

echo.
echo [3/6] Checking backend Python dependencies...
call :check_python_deps
if errorlevel 1 (
    echo [INFO] Installing backend dependencies...
    %PYTHON_CMD% -m pip install -r "%BACKEND_DIR%\requirements.txt"
    if errorlevel 1 (
        echo [ERROR] Failed to install backend dependencies.
        pause
        exit /b 1
    )
)

echo.
echo [4/6] Ensuring local AI models...
call :ensure_models
if errorlevel 1 exit /b 1

echo.
echo [5/6] Releasing stale backend process on port %PORT%...
for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":%PORT%.*LISTENING"') do (
    taskkill /F /PID %%a >nul 2>&1
    echo   Killed PID %%a
)

echo.
echo [6/6] Starting backend...
echo   DATABASE_URL=%DATABASE_URL%
echo   QDRANT_URL=%QDRANT_URL%
echo   Health: http://localhost:%PORT%/health
echo   Docs:   http://localhost:%PORT%/docs
echo.
%PYTHON_CMD% -m uvicorn app.main:app --reload --host 0.0.0.0 --port %PORT%

pause
exit /b 0

:find_python
set "PYTHON_CMD="
if exist "%BACKEND_DIR%\.venv\Scripts\python.exe" (
    set "PYTHON_CMD=%BACKEND_DIR%\.venv\Scripts\python.exe"
) else if exist "%ROOT_DIR%\.venv\Scripts\python.exe" (
    set "PYTHON_CMD=%ROOT_DIR%\.venv\Scripts\python.exe"
) else (
    py -3 --version >nul 2>&1
    if not errorlevel 1 set "PYTHON_CMD=py -3"
)

if not defined PYTHON_CMD (
    python --version >nul 2>&1
    if not errorlevel 1 set "PYTHON_CMD=python"
)

if not defined PYTHON_CMD (
    echo [ERROR] Python was not found.
    echo Install Python 3.11+ or create a venv, then run this script again.
    pause
    exit /b 1
)
echo Python: %PYTHON_CMD%
exit /b 0

:check_docker
docker info >nul 2>&1
if errorlevel 1 (
    echo [ERROR] Docker is not running. Please start Docker Desktop first.
    pause
    exit /b 1
)
docker compose version >nul 2>&1
if errorlevel 1 (
    echo [ERROR] Docker Compose v2 was not found.
    pause
    exit /b 1
)
echo Docker: running
exit /b 0

:ensure_env
if not exist "%BACKEND_DIR%\.env" (
    if exist "%BACKEND_DIR%\.env.example" (
        echo [INFO] Creating apps\backend\.env from .env.example
        copy "%BACKEND_DIR%\.env.example" "%BACKEND_DIR%\.env" >nul
    )
)
exit /b 0

:check_python_deps
%PYTHON_CMD% -c "import uvicorn, fastapi, sqlalchemy, qdrant_client" >nul 2>&1
if errorlevel 1 exit /b 1
exit /b 0

:check_models
if not exist "%ROOT_DIR%\scripts\prefetch_model.py" (
    echo [WARN] Model prefetch script not found. Backend may download models on first startup.
    exit /b 0
)
%PYTHON_CMD% "%ROOT_DIR%\scripts\prefetch_model.py" --check
if errorlevel 1 (
    echo [WARN] Some local models are missing. Normal start.bat will download them.
)
exit /b 0

:ensure_models
if not exist "%ROOT_DIR%\scripts\prefetch_model.py" (
    echo [WARN] Model prefetch script not found. Skipping explicit model download.
    exit /b 0
)
%PYTHON_CMD% "%ROOT_DIR%\scripts\prefetch_model.py" --check >nul 2>&1
if not errorlevel 1 (
    echo   Models: ready
    exit /b 0
)
echo   Missing model files detected. Downloading embedding + reranker models...
echo   This is resumable; if interrupted, run start.bat again.
%PYTHON_CMD% "%ROOT_DIR%\scripts\prefetch_model.py" --all
if errorlevel 1 (
    echo [ERROR] Model download failed.
    pause
    exit /b 1
)
exit /b 0

:wait_container_healthy
set "CONTAINER_NAME=%~1"
set /a "MAX_SECONDS=%~2"
set /a "ELAPSED=0"
echo Waiting for %CONTAINER_NAME% to become healthy...

:wait_loop
set "HEALTH="
for /f "delims=" %%s in ('docker inspect -f "{{.State.Health.Status}}" %CONTAINER_NAME% 2^>nul') do set "HEALTH=%%s"
if "%HEALTH%"=="healthy" (
    echo   %CONTAINER_NAME% is healthy.
    exit /b 0
)
if %ELAPSED% GEQ %MAX_SECONDS% (
    echo [ERROR] %CONTAINER_NAME% did not become healthy within %MAX_SECONDS%s.
    docker logs %CONTAINER_NAME% --tail 40
    pause
    exit /b 1
)
set /a "ELAPSED+=3"
timeout /t 3 /nobreak >nul
goto :wait_loop
