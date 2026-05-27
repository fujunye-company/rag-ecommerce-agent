@echo off
cd /d C:\Users\fujunye\Desktop\Hermes\04-rag-ecommerce\apps\backend
if not exist "app\main.py" (
    echo [ERROR] Wrong directory - app\main.py not found
    pause
    exit /b 1
)

set DATABASE_URL=postgresql+asyncpg://shopping:shopping123@127.0.0.1:5432/shopping_agent?ssl=disable
set QDRANT_URL=http://localhost:6333

echo ==============================
echo  Backend starting on Windows
echo  DATABASE_URL=%DATABASE_URL%
echo  QDRANT_URL=%QDRANT_URL%
echo ==============================

py -m uvicorn app.main:app --host 0.0.0.0 --port 8080
pause
