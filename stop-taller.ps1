# ============================================================
# stop-taller.ps1
# Apaga ambos servicios: App + MySQL.
# Usa -v como argumento para borrar tambien volumen de BD.
# ============================================================

$ProjectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $ProjectRoot

Write-Host ""
Write-Host "==============================================" -ForegroundColor Yellow
Write-Host "  Deteniendo Taller Mecanico (Docker)"         -ForegroundColor Yellow
Write-Host "==============================================" -ForegroundColor Yellow
Write-Host ""

$borrarVolumen = ($args -contains "-v") -or ($args -contains "--volumes")

if ($borrarVolumen) {
    Write-Host "[!] FLAG -v DETECTADO. Se BORRARA el volumen de datos MySQL (perderas BD)." -ForegroundColor Red
    $confirm = Read-Host "Escribe SI para confirmar borrado total"
    if ($confirm -ne "SI") { Write-Host "Cancelado."; exit 0 }
    docker compose down -v
} else {
    docker compose down
}

Write-Host ""
Write-Host "[HECHO] Servicios detenidos." -ForegroundColor Green
if (-not $borrarVolumen) {
    Write-Host "  (Los datos de la BD se conservan en el volumen taller-mysql-data)" -ForegroundColor DarkGray
}
Start-Sleep -Seconds 2
