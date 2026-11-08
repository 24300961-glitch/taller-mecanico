# ============================================================
# 0-INSTALAR-MAVEN-WRAPPER.ps1
# Descarga el maven-wrapper.jar necesario para ejecutar mvnw.cmd
# Solo es necesario ejecutar UNA VEZ si no tienes Maven instalado globalmente.
# ============================================================

$ProjectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $ProjectRoot

$WrapperDir  = "backend\.mvn\wrapper"
$WrapperJar  = "$WrapperDir\maven-wrapper.jar"
$WrapperUrl  = "https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.3.2/maven-wrapper-3.3.2.jar"
$Sha512Url   = "https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.3.2/maven-wrapper-3.3.2.jar.sha512"

Write-Host ""
Write-Host "==============================================" -ForegroundColor Cyan
Write-Host "  Instalando Maven Wrapper 3.3.2"            -ForegroundColor Cyan
Write-Host "==============================================" -ForegroundColor Cyan
Write-Host ""

if (-not (Test-Path $WrapperDir)) {
    New-Item -ItemType Directory -Path $WrapperDir -Force | Out-Null
}

if (Test-Path $WrapperJar) {
    Write-Host "  OK  maven-wrapper.jar ya existe." -ForegroundColor Green
    Write-Host "       Puedes ejecutar:  .\backend\mvnw.cmd -v" -ForegroundColor DarkGray
    exit 0
}

Write-Host "[1/2] Descargando maven-wrapper-3.3.2.jar ..."
Write-Host "       URL: $WrapperUrl"
try {
    [Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
    Invoke-WebRequest -Uri $WrapperUrl -OutFile $WrapperJar -UseBasicParsing -ErrorAction Stop
} catch {
    Write-Host ""
    Write-Host "  ERROR al descargar: ${_}" -ForegroundColor Red
    Write-Host ""
    Write-Host "  Alternativa: abre IntelliJ IDEA, carga el proyecto y ejecuta:" -ForegroundColor Yellow
    Write-Host "    Maven -> Lifecycle -> clean package" -ForegroundColor Yellow
    Write-Host "  O desde la terminal integrada de IntelliJ (donde mvn si esta en PATH):" -ForegroundColor Yellow
    Write-Host "    mvn wrapper:wrapper -Dmaven=3.9.9" -ForegroundColor Yellow
    exit 1
}

$size = (Get-Item $WrapperJar).Length
if ($size -lt 50000) {
    Write-Host "  ERROR: el archivo descargado solo tiene $size bytes (corrupto?)." -ForegroundColor Red
    exit 1
}
Write-Host "  OK  Descarga correcta ($([math]::Round($size/1KB,1)) KB)." -ForegroundColor Green
Write-Host ""
Write-Host "[2/2] Verificando funcionamiento:"
try {
    & .\backend\mvnw.cmd -v
    if ($LASTEXITCODE -eq 0) {
        Write-Host ""
        Write-Host "==============================================" -ForegroundColor Green
        Write-Host "  MAVEN WRAPPER LISTO." -ForegroundColor Green
        Write-Host "==============================================" -ForegroundColor Green
        Write-Host "  Compilar:    cd backend; .\mvnw.cmd -DskipTests package" -ForegroundColor White
        Write-Host "  Ejecutar:    cd backend; .\mvnw.cmd spring-boot:run"    -ForegroundColor White
        Write-Host ""
    }
} catch {
    Write-Host "  WARNING: no se pudo ejecutar mvnw.cmd. Asegurate de tener JAVA_HOME configurado con Java 25." -ForegroundColor Yellow
}
