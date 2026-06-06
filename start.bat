@echo off
setlocal EnableExtensions

cd /d "%~dp0"
call "%~dp0apps\backend\start.bat" %*
