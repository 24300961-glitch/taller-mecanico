# ============================================================
# start-mysql.ps1
# Levanta SOLAMENTE el contenedor MySQL en Docker.
# Úsalo cuando quieras ejecutar la app Spring Boot desde
# IntelliJ IDEA (sin Dockerizar la app).
# ============================================================
# EJECUCIÓN (PowerShell como usuario normal):
#   cd "C:\Users\ac765\OneDrive\Desktop\Taller Mecanico"
#   powershell -ExecutionPolicy Bypass -File .\start-mysql.ps1
# ============================================================

$ErrorActionPreference = "Continue"
$ProjectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $ProjectRoot

Write-Host ""
Write-Host "=============================================="  -ForegroundColor Cyan
Write-Host "  Taller Mecnico - Levantar MySQL (Docker)"      -ForegroundColor Cyan
Write-Host "=============================================="  -ForegroundColor Cyan
Write-Host ""

# --- Paso 1: Comprobar Docker Desktop instalado ---
try {
    $null = docker --version 2>&1
    Write-Host "[OK] Docker CLI detectado." -ForegroundColor Green
} catch {
    Write-Host "[ERROR] Docker CLI no encontrado. Instala Docker Desktop." -ForegroundColor Red
    Write-Host "  https://www.docker.com/products/docker-desktop/" -ForegroundColor Yellow
    Read-Host "Presiona Enter para salir"; exit 1
}

# --- Paso 2: Comprobar que Docker Desktop esté corriendo ---
$daemonOk = $false
for ($i = 1; $i -le 3; $i++) {
    try {
        $null = docker info --format "{{.ServerVersion}}" 2>&1
        $daemonOk = $true
        break
    } catch {
        Write-Host "[Paso $i/3] Docker Desktop NO esta iniciado. Intentando abrir Docker Desktop..." -ForegroundColor Yellow
        Start-Process "C:\Program Files\Docker\Docker\Docker Desktop.exe" -ErrorAction SilentlyContinue
        Start-Sleep -Seconds 15
    }
}

if (-not $daemonOk) {
    Write-Host ""
    Write-Host "[ERROR] Docker Desktop no responde." -ForegroundColor Red
    Write-Host "  Abre Docker Desktop manualmente, espera a que la ballena sea verde y vuelve a ejecutar este script." -ForegroundColor Yellow
    Read-Host "Presiona Enter para salir"; exit 1
}
Write-Host "[OK] Docker daemon corriendo." -ForegroundColor Green

# --- Paso 3: Comprobar archivo .env ---
if (-not (Test-Path ".env")) {
    Write-Host "[AVISO] No existe .env, copiando desde .env.example..." -ForegroundColor Yellow
    Copy-Item ".env.example" ".env" -Force
    Write-Host "[LISTO] Archivo .env creado. Revisa las credenciales si deseas cambiarlas." -ForegroundColor Green
}

# --- Paso 4: Crear carpeta logs (si no existe) ---
$null = New-Item -ItemType Directory -Path "$ProjectRoot\logs" -Force -ErrorAction SilentlyContinue

# --- Paso 5: Levantar contenedor MySQL ---
Write-Host ""
Write-Host "[...] Levantando contenedor MySQL 8.4..." -ForegroundColor Cyan
docker compose up -d mysql 2>&1 | ForEach-Object { Write-Host "  $_" }

if ($LASTEXITCODE -ne 0) {
    Write-Host "[ERROR] Fallo al levantar MySQL con docker compose." -ForegroundColor Red
    Read-Host "Presiona Enter para salir"; exit 1
}

# --- Paso 6: Esperar healthcheck (MySQL listo para conexiones) ---
Write-Host ""
Write-Host "[...] Esperando healthcheck de MySQL (paciencia ~30 s)..." -ForegroundColor Cyan
$healthy = $false
$max = 25
for ($i = 1; $i -le $max; $i++) {
    $status = docker inspect --format "{{.State.Health.Status}}" taller-mysql 2>$null
    if ($status -eq "healthy") { $healthy = $true; break }
    Write-Host "  Espera $i/${max} ... estado actual: $status" -ForegroundColor DarkGray
    Start-Sleep -Seconds 3
}

if (-not $healthy) {
    Write-Host ""
    Write-Host "[ADVERTENCIA] MySQL no reporto healthy en el tiempo esperado." -ForegroundColor Yellow
    Write-Host "  Comprueba los logs: docker logs taller-mysql" -ForegroundColor Yellow
} else {
    Write-Host "[OK] MySQL saludable y listo para conexiones." -ForegroundColor Green
}

# --- Paso 7: Resumen ---
Write-Host ""
Write-Host "==============================================" -ForegroundColor Green
Write-Host "  MySQL DOCKER LISTO" -ForegroundColor Green
Write-Host "==============================================" -ForegroundColor Green
Write-Host "  Host/local:   localhost:3306"
Write-Host "  BD:           taller_mecanico"
Write-Host "  Usuario:      taller"
Write-Host "  Password:     (definido en .env -> DB_PASSWORD)"
Write-Host "  Root pass:    (definido en .env -> MYSQL_ROOT_PASSWORD)"
Write-Host "  Contenedor:   taller-mysql"
Write-Host "  Logs BD:      docker logs -f taller-mysql"
Write-Host ""
Write-Host "  Ahora abre IntelliJ IDEA y ejecuta TallerMecanicoApplication"
Write-Host "  o bien ejecuta:  .\start-app.ps1"
Write-Host ""
Read-Host "Presiona Enter para cerrar"
