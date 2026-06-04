@echo off
cd /d "%~dp0..\apps\backend"
if not exist "app\main.py" (
    echo [ERROR] Wrong directory - app\main.py not found
    pause
    exit /b 1
)

if not defined DATABASE_URL set "DATABASE_URL=postgresql+asyncpg://shopping:shopping123@127.0.0.1:5432/shopping_agent?ssl=disable"
if not defined QDRANT_URL set "QDRANT_URL=http://localhost:6333"
if not defined PORT set "PORT=8080"
set "PYTHON_CMD="

py -3 --version >nul 2>&1
if not errorlevel 1 set "PYTHON_CMD=py -3"

if not defined PYTHON_CMD (
    python --version >nul 2>&1
    if not errorlevel 1 set "PYTHON_CMD=python"
)

if not defined PYTHON_CMD (
    echo [ERROR] Python was not found.
    echo Please install Python 3.11+ or disable the Microsoft Store python alias.
    pause
    exit /b 1
)

echo ==============================
echo  Backend starting on Windows
echo  DATABASE_URL=%DATABASE_URL%
echo  QDRANT_URL=%QDRANT_URL%
echo  PORT=%PORT%
echo ==============================

%PYTHON_CMD% -m uvicorn app.main:app --host 0.0.0.0 --port %PORT%
pause
