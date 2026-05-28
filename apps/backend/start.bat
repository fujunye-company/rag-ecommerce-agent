@echo off
cd /d "%~dp0"

echo ===========================================
echo   RAG E-Commerce Backend Launcher
echo ===========================================
echo.
echo Killing stale uvicorn on port 8082...
for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":8082.*LISTENING"') do (
    taskkill /F /PID %%a >nul 2>&1
    echo   Killed PID %%a
)
echo.
echo HTTP proxy: DISABLED (port 7897 is VPN, not HTTP proxy)
REM set HTTP_PROXY=http://127.0.0.1:7897
REM set HTTPS_PROXY=http://127.0.0.1:7897
echo.

echo Working dir: %cd%
echo Port: 8082
echo.

"C:\Users\fujunye\AppData\Local\Python\pythoncore-3.14-64\python.exe" -m uvicorn app.main:app --reload --host 0.0.0.0 --port 8082

pause
