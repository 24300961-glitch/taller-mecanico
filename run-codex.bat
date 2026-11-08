@echo off
chcp 65001 >nul
echo =======================================================
echo   TALLER MECANICO - EJECUCION RAPIDA PARA CODEX
echo =======================================================
echo.

REM === PASO 1: Buscar JDK valido (24 o 25) ===
set "JAVA_HOME="
if exist "%LOCALAPPDATA%\Programs\Eclipse Adoptium\jdk-25.0.4.101-hotspot" (
    set "JAVA_HOME=%LOCALAPPDATA%\Programs\Eclipse Adoptium\jdk-25.0.4.101-hotspot"
    echo [OK] JDK 25 encontrado en: %JAVA_HOME%
) else if exist "C:\Program Files\Java\jdk-24" (
    set "JAVA_HOME=C:\Program Files\Java\jdk-24"
    echo [OK] JDK 24 encontrado en: %JAVA_HOME%
)

if "%JAVA_HOME%"=="" (
    echo [ERROR] No se encontro JDK 24 ni JDK 25. Instala Eclipse Temurin 25.
    pause
    exit /b 1
)

set "PATH=%JAVA_HOME%\bin;%PATH%"

REM === PASO 2: Verificar JAR compilado ===
set "JAR_FILE=backend\target\taller-mecanico-0.0.1-SNAPSHOT.jar"
if not exist "%JAR_FILE%" (
    echo.
    echo [INFO] JAR no encontrado. Compilando con Maven...
    set "MVN_CMD="
    if exist "%USERPROFILE%\.m2\wrapper\dists\apache-maven-3.8.5-bin\5i5jha092a3i37g0paqnfr15e0\apache-maven-3.8.5\bin\mvn.cmd" (
        set "MVN_CMD=%USERPROFILE%\.m2\wrapper\dists\apache-maven-3.8.5-bin\5i5jha092a3i37g0paqnfr15e0\apache-maven-3.8.5\bin\mvn.cmd"
    ) else if exist "C:\Program Files\JetBrains\IntelliJ IDEA 2026.2.3\plugins\maven-plugin\lib\maven3\bin\mvn.cmd" (
        set "MVN_CMD=C:\Program Files\JetBrains\IntelliJ IDEA 2026.2.3\plugins\maven-plugin\lib\maven3\bin\mvn.cmd"
    )

    if "%MVN_CMD%"=="" (
        echo [ERROR] No se encontro Maven.
        pause
        exit /b 1
    )

    echo [INFO] Usando Maven en: %MVN_CMD%
    call "%MVN_CMD%" -f backend\pom.xml package -DskipTests
    if errorlevel 1 (
        echo [ERROR] Fallo la compilacion.
        pause
        exit /b 1
    )
    echo [OK] Compilacion exitosa.
) else (
    echo [OK] JAR ya compilado: %JAR_FILE%
)

REM === PASO 3: Configurar variables de admin ===
set "APP_ADMIN_EMAIL=admin@taller.local"
set "APP_ADMIN_PASSWORD=AdminTallerMecanico2026!"

REM === PASO 4: Levantar servidor ===
echo.
echo =======================================================
echo   INICIANDO SERVIDOR EN http://localhost:8080
echo   Login:    http://localhost:8080/login
echo   Admin:    %APP_ADMIN_EMAIL%
echo   Password: %APP_ADMIN_PASSWORD%
echo   (Presiona Ctrl+C para detener)
echo =======================================================
echo.

java -jar "%JAR_FILE%"
pause
