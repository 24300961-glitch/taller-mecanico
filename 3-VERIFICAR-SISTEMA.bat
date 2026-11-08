@echo off
REM ============================================================
REM  3-VERIFICAR-SISTEMA.bat  (doble clic)
REM  Prueba de humo: Docker, MySQL healthy, tablas en BD, HTTP.
REM ============================================================
chcp 65001 >nul
cd /d "%~dp0"
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0test-taller.ps1"
pause
