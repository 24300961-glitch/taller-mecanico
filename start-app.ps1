# start-app.ps1
# Arranca la app Spring Boot. Setea variables de entorno. Usa H2 por defecto (no Docker).
# Requiere Java 25 y Maven 3.9.9 (instalados arriba).
param(
  [switch]$UsarMySQL
)
$ErrorActionPreference = 'Stop'

$JAVA_HOME = "C:\Users\ac765\AppData\Local\Programs\Eclipse Adoptium\jdk-25.0.4.101-hotspot"
$MAVEN_HOME = "$env:USERPROFILE\tools\apache-maven-3.9.9"
$PROJECT = "c:\Users\ac765\OneDrive\Desktop\Taller Mecanico"
$LOG = "$env:TEMP\spring-boot-run.log"
$MVN = "$MAVEN_HOME\bin\mvn.cmd"

# --- Configurar environment (efecto solo este proceso + hijos) ---
[Environment]::SetEnvironmentVariable("JAVA_HOME", $JAVA_HOME, "Process")
[Environment]::SetEnvironmentVariable("MAVEN_HOME", $MAVEN_HOME, "Process")
$pathNow = [Environment]::GetEnvironmentVariable("PATH", "Process")
if (-not $pathNow.StartsWith($JAVA_HOME)) {
  [Environment]::SetEnvironmentVariable("PATH", "$JAVA_HOME\bin;$MAVEN_HOME\bin;$pathNow", "Process")
}

# --- Cargar .env en variables de entorno ---
$envPath = Join-Path $PROJECT ".env"
if (Test-Path $envPath) {
  Get-Content $envPath | ForEach-Object {
    $line = $_.Trim()
    if ($line -and -not $line.StartsWith("#") -and $line.Contains("=")) {
      $idx = $line.IndexOf("=")
      $k = $line.Substring(0, $idx).Trim()
      $v = $line.Substring($idx + 1).Trim()
      if ($v.StartsWith('"') -and $v.EndsWith('"')) { $v = $v.Substring(1, $v.Length - 2) }
      [Environment]::SetEnvironmentVariable($k, $v, "Process")
    }
  }
  Write-Host "[OK] .env cargado" -ForegroundColor Green
}

# --- Perfil BD: por defecto H2 (no requiere Docker). Si -UsarMySQL: configurar vars ---
if ($UsarMySQL) {
  Write-Host "[BD] Perfil MySQL (requiere taller-mysql container running en 3306)" -ForegroundColor Yellow
  [Environment]::SetEnvironmentVariable("DB_URL", "jdbc:mysql://localhost:3306/taller_mecanico?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&useUnicode=true&characterEncoding=utf8", "Process")
  [Environment]::SetEnvironmentVariable("DB_DRIVER", "com.mysql.cj.jdbc.Driver", "Process")
  [Environment]::SetEnvironmentVariable("DB_USERNAME", "taller", "Process")
  [Environment]::SetEnvironmentVariable("DB_DIALECT", "org.hibernate.dialect.MySQLDialect", "Process")
  [Environment]::SetEnvironmentVariable("DB_DDL_AUTO", "update", "Process")
} else {
  Write-Host "[BD] Perfil H2 in-memory (sin Docker, rápido para smoke test)" -ForegroundColor Cyan
  [Environment]::SetEnvironmentVariable("DB_URL",      "", "Process")
  [Environment]::SetEnvironmentVariable("DB_DRIVER",   "", "Process")
  [Environment]::SetEnvironmentVariable("DB_USERNAME", "", "Process")
  [Environment]::SetEnvironmentVariable("DB_DIALECT",  "", "Process")
  [Environment]::SetEnvironmentVariable("DB_DDL_AUTO", "create-drop", "Process")
}

# --- Validar que admin password cumple 16 chars (lo requiere InicializadorSistema) ---
$pw = [Environment]::GetEnvironmentVariable("APP_ADMIN_PASSWORD", "Process")
if ([string]::IsNullOrWhiteSpace($pw) -or $pw.Length -lt 16) {
  Write-Host "[FATAL] APP_ADMIN_PASSWORD debe ser >= 16 chars (actual: $($pw?.Length) chars)" -ForegroundColor Red
  exit 2
}

Write-Host ""
Write-Host "==========================================" -ForegroundColor Cyan
Write-Host " Arranque Spring Boot"
Write-Host " JAVA_HOME  = $JAVA_HOME"
Write-Host " MAVEN_HOME = $MAVEN_HOME"
Write-Host " ADMIN      = $([Environment]::GetEnvironmentVariable('APP_ADMIN_EMAIL','Process')) / PASS=$($pw.Length)chars"
Write-Host " LOG        = $LOG"
Write-Host "==========================================" -ForegroundColor Cyan
Write-Host ""
Remove-Item $LOG -ErrorAction SilentlyContinue
"[INICIO] Spring Boot run $(Get-Date -Format o)" | Out-File -FilePath $LOG -Encoding utf8

# Arrancar maven
Set-Location $PROJECT
& $MVN spring-boot:run 1>> $LOG 2>> $LOG
$EXIT = $LASTEXITCODE
"[FIN] exit=$EXIT $(Get-Date -Format o)" | Out-File -FilePath $LOG -Append -Encoding utf8
Write-Host "!!! TERMINO. EXIT=$EXIT  Revisa $LOG para detalles" -ForegroundColor $(if ($EXIT -eq 0){'Green'} else {'Red'})
exit $EXIT
