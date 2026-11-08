@echo off
REM ============================================================
REM  9-DETENER-TODO.bat  (doble clic)
REM  Apaga App + MySQL sin borrar datos.
REM ============================================================
chcp 65001 >nul
cd /d "%~dp0"
echo.
echo ==============================================
echo   Deteniendo Taller Mecanico (Docker)
echo ==============================================
echo.
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0stop-taller.ps1"
pause
