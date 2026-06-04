@echo off
cd /d "%~dp0"

if not defined PORT set "PORT=8080"

echo ===========================================
echo   RAG E-Commerce Backend Launcher
echo ===========================================
echo.
echo Killing stale uvicorn on port %PORT%...
for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":%PORT%.*LISTENING"') do (
    taskkill /F /PID %%a >nul 2>&1
    echo   Killed PID %%a
)


echo Working dir: %cd%
echo Port: %PORT%
echo.

set "PYTHON_CMD="

if exist ".venv\Scripts\python.exe" (
    set "PYTHON_CMD=.venv\Scripts\python.exe"
) else if exist "..\..\.venv\Scripts\python.exe" (
    set "PYTHON_CMD=..\..\.venv\Scripts\python.exe"
) else (
    py -3 --version >nul 2>&1
    if not errorlevel 1 set "PYTHON_CMD=py -3"
)

if not defined PYTHON_CMD (
    python --version >nul 2>&1
    if not errorlevel 1 set "PYTHON_CMD=python"
)

if not defined PYTHON_CMD (
    echo ERROR: Python was not found.
    echo Please install Python 3.11+ or create a virtual environment:
    echo   python -m venv .venv
    echo   .venv\Scripts\python -m pip install -r requirements.txt
    pause
    exit /b 1
)

%PYTHON_CMD% -m uvicorn app.main:app --reload --host 0.0.0.0 --port %PORT%

pause
