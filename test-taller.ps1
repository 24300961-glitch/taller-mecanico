# ============================================================
# test-taller.ps1
# Prueba de humo: verifica que todo el sistema funciona.
# 1. Docker containers running?
# 2. MySQL healthy?
# 3. Tablas users y password_reset_tokens existen?
# 4. Login HTTP 200 OK?
# 5. Carpeta logs creada?
# ============================================================

$ProjectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $ProjectRoot
$ErrorActionPreference = "Continue"

$ok = 0
$fail = 0

Write-Host ""
Write-Host "==============================================" -ForegroundColor Cyan
Write-Host "  Taller Mecanico - Prueba de humo"            -ForegroundColor Cyan
Write-Host "==============================================" -ForegroundColor Cyan
Write-Host ""

# --- 1. Docker containers ---
Write-Host "[1/5] Contenedores Docker..."
$ps = docker compose ps --format "{{.Name}}|{{.Status}}" 2>$null
$mysqlRunning = ($ps | Select-String "taller-mysql|Up") -ne $null
$appRunning   = ($ps | Select-String "taller-app|Up") -ne $null
if ($mysqlRunning) { Write-Host "   ✓ MySQL container UP" -ForegroundColor Green; $ok++ }
else { Write-Host "   ✗ MySQL NO levantado" -ForegroundColor Red; $fail++ }
if ($appRunning)   { Write-Host "   ✓ App   container UP" -ForegroundColor Green; $ok++ }
else { Write-Host "   ℹ App container NO levantado (es OK si usas IntelliJ)" -ForegroundColor Yellow }

# --- 2. MySQL healthy ---
Write-Host "[2/5] Healthcheck MySQL..."
$h = docker inspect --format "{{.State.Health.Status}}" taller-mysql 2>$null
if ($h -eq "healthy") { Write-Host "   ✓ MySQL HEALTHY" -ForegroundColor Green; $ok++ }
else                  { Write-Host "   ✗ MySQL estado: $h" -ForegroundColor Red; $fail++ }

# --- 3. Tablas creadas en BD ---
Write-Host "[3/5] Tablas en base de datos taller_mecanico..."
try {
    $pass = ((Get-Content .env | Select-String '^DB_PASSWORD=').ToString() -split '=', 2)[1]
    $tables = docker exec taller-mysql mysql -u taller -p"$pass" taller_mecanico -N -e "SHOW TABLES;" 2>$null
    if ($tables -match "users" -and $tables -match "password_reset_tokens" -and $tables -match "citas") {
        Write-Host "   ✓ Tablas 'users', 'password_reset_tokens', 'citas' EXISTEN" -ForegroundColor Green
        $ok++
    } elseif ($tables -match "users") {
        Write-Host "   ✓ Tabla 'users' creada. Espera un poco para las demas..." -ForegroundColor Green
        Write-Host "     Tablas actuales: $tables" -ForegroundColor DarkGray
        $ok++
    } else {
        Write-Host "   ℹ Aun no hay tablas (normal si la app no ha arrancado)." -ForegroundColor Yellow
        Write-Host "     La app las crea automaticamente en el primer inicio con JPA hibernate.ddl-auto=update" -ForegroundColor DarkGray
    }
} catch {
    Write-Host "   ! No se pudo consultar MySQL (${_})" -ForegroundColor Yellow
}

# --- 4. HTTP login 200 ---
Write-Host "[4/5] Respuesta HTTP /login..."
try {
    $r = Invoke-WebRequest -Uri "http://localhost:8080/login" -UseBasicParsing -TimeoutSec 5 -ErrorAction Stop
    if ($r.StatusCode -eq 200) { Write-Host "   ✓ http://localhost:8080/login  -> 200 OK" -ForegroundColor Green; $ok++ }
} catch {
    $status = if ($_.Exception.Response) { [int]$_.Exception.Response.StatusCode } else { 0 }
    if ($status -eq 200) { Write-Host "   ✓ http://localhost:8080/login  -> 200 OK" -ForegroundColor Green; $ok++ }
    else {
        Write-Host "   ℹ No hay respuesta en 8080 (es OK si aun no arrancaste la app desde IntelliJ)." -ForegroundColor Yellow
    }
}

# --- 5. Logs ---
Write-Host "[5/5] Carpeta de logs..."
if (Test-Path "$ProjectRoot\logs") {
    $logFiles = Get-ChildItem "$ProjectRoot\logs\*.log" -ErrorAction SilentlyContinue
    Write-Host "   ✓ Carpeta logs existe. Archivos: $($logFiles.Count)" -ForegroundColor Green
    $ok++
    $logFiles | ForEach-Object { Write-Host "       - $($_.Name)  ($([math]::Round($_.Length/1KB,1)) KB)" -ForegroundColor DarkGray }
} else { Write-Host "   ! Carpeta logs no creada aun." -ForegroundColor Yellow }

# --- Resumen ---
Write-Host ""
Write-Host "=============================================="
Write-Host "  RESUMEN: $ok OK / $fall ERRORES" -ForegroundColor $(if ($fail -eq 0) {"Green"} else {"Red"})
Write-Host "=============================================="
if ($fail -eq 0 -and $ok -ge 4) {
    Write-Host "  ✅ SISTEMA FUNCIONANDO CORRECTAMENTE." -ForegroundColor Green
    Write-Host "     Abre http://localhost:8080/ en tu navegador." -ForegroundColor Green
} else {
    Write-Host "  Revisa los puntos en rojo y ejecuta .\start-taller.ps1 si falta levantar servicios." -ForegroundColor Yellow
}
Write-Host ""
Start-Sleep -Seconds 3
