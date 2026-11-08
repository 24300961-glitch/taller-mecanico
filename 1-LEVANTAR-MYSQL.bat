@echo off
REM ============================================================
REM  1-LEVANTAR-MYSQL.bat   (doble clic)
REM  Inicia Docker Desktop si no corre y levanta MySQL.
REM ============================================================
chcp 65001 >nul
cd /d "%~dp0"
echo.
echo ==============================================
echo   Taller Mecanico - Levantar MySQL en Docker
echo ==============================================
echo.
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0start-mysql.ps1"
if errorlevel 1 (
    echo.
    echo [ERROR] El script devolvio error. Revisa las lineas de arriba.
    pause
)
