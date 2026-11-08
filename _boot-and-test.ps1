$ErrorActionPreference = "Continue"
$env:JAVA_HOME = "C:\Users\ac765\AppData\Local\Programs\Eclipse Adoptium\jdk-25.0.4.101-hotspot"
$JAVA = "$env:JAVA_HOME\bin\java.exe"
$ROOT = "c:\Users\ac765\OneDrive\Desktop\Taller Mecanico"
$JAR  = Join-Path $ROOT "backend\target\taller-mecanico-0.0.1-SNAPSHOT.jar"
$LOG_DIR = Join-Path $ROOT "logs-boot"
New-Item -ItemType Directory -Force -Path $LOG_DIR | Out-Null
$ts = Get-Date -Format yyyyMMddHHmmss
$LOG_O = Join-Path $LOG_DIR "boot-$ts.log"
$LOG_E = Join-Path $LOG_DIR "boot-$ts-err.log"
Set-Content -Path (Join-Path $ROOT ".last-boot-log.txt") -Value $LOG_O -Encoding ascii

# Cargar .env -> variables de entorno de este proceso
Get-Content (Join-Path $ROOT ".env") | ForEach-Object {
    $ln = $_.Trim()
    if ($ln -and -not $ln.StartsWith("#") -and $ln.Contains("=")) {
        $i = $ln.IndexOf("=")
        $k = $ln.Substring(0, $i).Trim()
        $v = $ln.Substring($i + 1).Trim()
        [Environment]::SetEnvironmentVariable($k, $v, "Process")
    }
}
foreach ($k in @("DB_URL","DB_DRIVER","DB_USERNAME","DB_DIALECT")) {
    [Environment]::SetEnvironmentVariable($k, "", "Process")
}
[Environment]::SetEnvironmentVariable("DB_DDL_AUTO", "create-drop", "Process")

"[$(Get-Date -Format o)] INICIANDO java.exe con H2..." | Out-File -FilePath $LOG_O -Encoding utf8
"ADMIN=$([Environment]::GetEnvironmentVariable('APP_ADMIN_EMAIL','Process'))" | Out-File -Append $LOG_O

Write-Host "JAVA: $JAVA"
Write-Host "JAR : $JAR"
Write-Host "LOG : $LOG_O"
Write-Host "PID_FILE: $(Join-Path $ROOT '.app-pid.txt')"

$p = Start-Process -FilePath $JAVA -ArgumentList @(
    "-Xmx768m",
    "--enable-native-access=ALL-UNNAMED",
    "-Dfile.encoding=UTF-8",
    "-jar",
    $JAR
) -RedirectStandardOutput $LOG_O -RedirectStandardError $LOG_E -NoNewWindow -PassThru

$p.Id | Out-File (Join-Path $ROOT ".app-pid.txt") -Encoding ascii
Write-Host "=== Java lanzado PID=$($p.Id) ==="

Write-Host "Esperando 22s de arranque..."
Start-Sleep -Seconds 22

Write-Host ""
Write-Host "=== Procesos java vivos ==="
Get-Process java -ErrorAction SilentlyContinue | ForEach-Object {
    $cmd = ""
    try { $cmd = (Get-CimInstance Win32_Process -Filter "ProcessId=$($_.Id)").CommandLine } catch {}
    [pscustomobject]@{
        Id=$_.Id
        CPU=[math]::Round($_.CPU,1)
        TallerJar=($cmd -match 'taller-mecanico-0.0.1-SNAPSHOT')
    }
} | Format-Table -AutoSize | Out-Host

Write-Host ""
Write-Host "=== netstat 8080 LISTENING ==="
$lines = netstat -ano -p tcp 2>$null
$lines | Select-String ":8080" | Select-String "LISTENING|ESTABLISHED" | Out-Host

Write-Host ""
Write-Host "=== HTTP GET http://127.0.0.1:8080/login ==="
try {
    $ProgressPreference = "SilentlyContinue"
    $r = Invoke-WebRequest -Uri "http://127.0.0.1:8080/login" -UseBasicParsing -TimeoutSec 15
    Write-Host "HTTP Status=$($r.StatusCode) Len=$($r.Content.Length)" -ForegroundColor Green
    $hasRoot = [bool]($r.Content -match 'id="root"')
    $hasAssets = [bool]($r.Content -match '/assets/')
    $hasForm = [bool]($r.Content -match 'name="email"')
    Write-Host "  Raiz React:          $hasRoot"
    Write-Host "  Bundle /assets/:     $hasAssets"
    Write-Host "  Form email/password: $hasForm (tras hidratar en navegador)"
    Write-Host ""
    Write-Host "=== HTTP GET /usuarios (debe 302 -> /login sin auth) ==="
    try {
        $r2 = Invoke-WebRequest -Uri "http://127.0.0.1:8080/usuarios" -UseBasicParsing -MaximumRedirection 0 -TimeoutSec 15 -ErrorAction Stop
        Write-Host "  HTTP=$($r2.StatusCode) (esperado 302)" -ForegroundColor Yellow
    } catch {
        if ($_.Exception.Response) {
            $st = [int]$_.Exception.Response.StatusCode
            $loc = $_.Exception.Response.Headers["Location"]
            Write-Host "  HTTP=$st  Location=$loc" -ForegroundColor Green
        } else {
            Write-Host "  ERR: $_" -ForegroundColor Red
        }
    }
} catch {
    Write-Host "HTTP ERROR: $_" -ForegroundColor Red
}

Write-Host ""
Write-Host "=== Tail 80 lines $LOG_O ==="
Get-Content $LOG_O -Tail 80
