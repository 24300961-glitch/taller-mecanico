# ============================================================
# start-taller.ps1
# Levanta TODO el sistema en Docker: MySQL + App Spring Boot
# Incluye build del Dockerfile con Maven.
# ============================================================
# EJECUCIÓN (PowerShell como usuario normal):
#   cd "C:\Users\ac765\OneDrive\Desktop\Taller Mecanico"
#   powershell -ExecutionPolicy Bypass -File .\start-taller.ps1
# ============================================================

$ErrorActionPreference = "Continue"
$ProjectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $ProjectRoot

Write-Host ""
Write-Host "=============================================="  -ForegroundColor Cyan
Write-Host "  Taller Mecanico - Levantar TODO (Docker)"     -ForegroundColor Cyan
Write-Host "  MySQL 8.4 + App Spring Boot Java 25"           -ForegroundColor Cyan
Write-Host "=============================================="  -ForegroundColor Cyan
Write-Host ""

# --- Paso 1: Docker disponible y corriendo ---
try { $null = docker --version 2>&1 } catch {
    Write-Host "[ERROR] Docker CLI no encontrado. Instala Docker Desktop." -ForegroundColor Red
    Read-Host "Presiona Enter"; exit 1
}

$daemonOk = $false
for ($i = 1; $i -le 3; $i++) {
    try { $null = docker info 2>&1; $daemonOk = $true; break }
    catch {
        Write-Host "[Paso $i/3] Docker Desktop no iniciado. Abriendo..." -ForegroundColor Yellow
        Start-Process "C:\Program Files\Docker\Docker\Docker Desktop.exe" -ErrorAction SilentlyContinue
        Start-Sleep -Seconds 15
    }
}
if (-not $daemonOk) {
    Write-Host "[ERROR] Docker Desktop no responde. Abrelo manualmente y espera ballena verde." -ForegroundColor Red
    Read-Host "Presiona Enter"; exit 1
}
Write-Host "[OK] Docker daemon corriendo." -ForegroundColor Green

# --- Paso 2: .env y carpeta logs ---
if (-not (Test-Path ".env")) { Copy-Item ".env.example" ".env" -Force; Write-Host "[OK] .env creado desde example." -ForegroundColor Green }
$null = New-Item -ItemType Directory -Path "$ProjectRoot\logs" -Force -ErrorAction SilentlyContinue

# --- Paso 3: Construir y levantar todo (con build --no-cache opcional? no, usamos build normal) ---
Write-Host ""
Write-Host "[...] Construyendo imagen App Spring Boot + levantando servicios..." -ForegroundColor Cyan
Write-Host "      (la PRIMERA vez toma ~2-5 min, descarga Temurin 25 y compila Maven)" -ForegroundColor DarkGray

docker compose up -d --build 2>&1 | ForEach-Object { Write-Host "  $_" }

if ($LASTEXITCODE -ne 0) {
    Write-Host "[ERROR] Fallo docker compose up --build." -ForegroundColor Red
    Read-Host "Presiona Enter"; exit 1
}

# --- Paso 4: Esperar MySQL healthy ---
Write-Host ""
Write-Host "[...] Esperando MySQL healthy..." -ForegroundColor Cyan
$healthy = $false
for ($i = 1; $i -le 25; $i++) {
    $st = docker inspect --format "{{.State.Health.Status}}" taller-mysql 2>$null
    if ($st -eq "healthy") { $healthy = $true; break }
    Write-Host "  Espera $i/25 ... MySQL: $st" -ForegroundColor DarkGray
    Start-Sleep -Seconds 3
}
if ($healthy) { Write-Host "[OK] MySQL listo (healthy)." -ForegroundColor Green }

# --- Paso 5: Esperar que la App arranque (8080 responda) ---
Write-Host ""
Write-Host "[...] Esperando que la App Spring Boot inicie y cree las tablas..." -ForegroundColor Cyan
Write-Host "      (toma ~15-45 s adicionales despues de MySQL)" -ForegroundColor DarkGray
$appReady = $false
for ($i = 1; $i -le 30; $i++) {
    try {
        $r = Invoke-WebRequest -Uri "http://localhost:8080/login" -UseBasicParsing -TimeoutSec 3 -ErrorAction Stop
        if ($r.StatusCode -eq 200) { $appReady = $true; break }
    } catch {
        # esperar
    }
    Write-Host "  Espera $i/30 ... App no responde aun en puerto 8080" -ForegroundColor DarkGray
    Start-Sleep -Seconds 3
}

if ($appReady) { Write-Host "[OK] Aplicacion Spring Boot respondiendo en puerto 8080." -ForegroundColor Green }

# --- Paso 6: Resumen ---
Write-Host ""
Write-Host "==============================================" -ForegroundColor Green
Write-Host "  SISTEMA TALLER MECANICO 100% OPERATIVO"     -ForegroundColor Green
Write-Host "==============================================" -ForegroundColor Green
Write-Host ""
Write-Host "  🌐 APLICACION WEB:   http://localhost:8080/"
Write-Host "     → Login:          http://localhost:8080/login"
Write-Host "     → Registro:       http://localhost:8080/registro"
Write-Host "     → Dashboard:      http://localhost:8080/dashboard (login requerido)"
Write-Host ""
Write-Host "  🗄️ BASE DE DATOS (desde tu PC):"
Write-Host "     Host:   localhost  Puerto: 3306"
Write-Host "     BD:     taller_mecanico"
Write-Host "     User:   taller"
Write-Host "     Pass:   (mirar .env -> DB_PASSWORD)"
Write-Host ""
Write-Host "  👤 CUENTA ADMIN INICIAL (si no habia usuarios):"
Write-Host "     Email:  $((Get-Content .env | Select-String 'APP_ADMIN_EMAIL').ToString().Split('=')[1])"
Write-Host "     Pass:   (mirar .env -> APP_ADMIN_PASSWORD, >=16 chars)"
Write-Host ""
Write-Host "  📁 LOGS FISICOS (carpeta local):"
Write-Host "     $ProjectRoot\logs\ "
Write-Host ""
Write-Host "  🐳 DOCKER - COMANDOS UTILES:"
Write-Host "     Ver estado:      docker compose ps"
Write-Host "     Logs BD:         docker logs -f taller-mysql"
Write-Host "     Logs App:        docker logs -f taller-app"
Write-Host "     Apagar todo:     .\stop-taller.ps1   ó   docker compose down"
Write-Host "     Borrar BD + todo: docker compose down -v  (⚠️  pierdes datos)"
Write-Host ""
Read-Host "Presiona Enter para cerrar"
