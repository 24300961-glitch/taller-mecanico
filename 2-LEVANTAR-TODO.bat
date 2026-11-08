@echo off
REM ============================================================
REM  2-LEVANTAR-TODO.bat   (doble clic)
REM  Construye y levanta MySQL + App Spring Boot EN DOCKER.
REM ============================================================
chcp 65001 >nul
cd /d "%~dp0"
echo.
echo ==============================================
echo   Taller Mecanico - Levantar TODO en Docker
echo   MySQL 8.4 + App Spring Boot Java 25
echo ==============================================
echo.
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0start-taller.ps1"
if errorlevel 1 (
    echo.
    echo [ERROR] El script devolvio error. Revisa las lineas de arriba.
    pause
)
