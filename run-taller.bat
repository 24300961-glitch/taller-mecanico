@echo off
REM ============================================================
REM  run-taller.bat - Arranca la aplicacion Spring Boot con H2
REM  No requiere Docker. Escribe logs en %TEMP%\run-taller*.log
REM ============================================================
setlocal enabledelayedexpansion

set "JAVA_HOME=C:\Users\ac765\AppData\Local\Programs\Eclipse Adoptium\jdk-25.0.4.101-hotspot"
set "MAVEN_HOME=%USERPROFILE%\tools\apache-maven-3.9.9"
set "PROJECT_DIR=c:\Users\ac765\OneDrive\Desktop\Taller Mecanico"
set "PATH=%JAVA_HOME%\bin;%MAVEN_HOME%\bin;%PATH%"

set "TS=%date:~-4%%date:~3,2%%date:~0,2%%time:~0,2%%time:~3,2%%time:~6,2%"
set "TS=%TS: =0%"
set "LOG_OUT=%TEMP%\taller-out-%TS%.log"
set "LOG_ERR=%TEMP%\taller-err-%TS%.log"

echo [INFO] JAVA_HOME=%JAVA_HOME%
echo [INFO] MAVEN_HOME=%MAVEN_HOME%
echo [INFO] LOG_OUT=%LOG_OUT%
echo [INFO] LOG_ERR=%LOG_ERR%

REM Guardar rutas para luego poder leer los logs
echo %LOG_OUT%> "%TEMP%\taller-last-out.log"
echo %LOG_ERR%> "%TEMP%\taller-last-err.log"

cd /d "%PROJECT_DIR%"

REM ======= Cargar variables de .env =======
if exist ".env" (
  for /f "usebackq tokens=1,* delims==" %%A in (".env") do (
    set "LINE=%%A"
    set "LINE=!LINE:"=!"
    if not "!LINE:~0,1!"=="#" if not "!LINE!"=="" (
      set "%%A=%%B"
    )
  )
  echo [INFO] .env cargado
)

REM ======= Perfil BD: H2 por defecto =======
set DB_URL=
set DB_DRIVER=
set DB_USERNAME=
set DB_DIALECT=
set DB_DDL_AUTO=create-drop

echo [INFO] Perfil BD: H2 in-memory (sin Docker)
echo [INFO] Admin: %APP_ADMIN_EMAIL% (pass.len %APP_ADMIN_PASSWORD: =,len=? no, contemos abajo)

echo [%date% %time%] EMPEZANDO > "%LOG_OUT%"
echo [%date% %time%] ERRORES  > "%LOG_ERR%"

REM ======= ARRANCAR SPRING BOOT =======
"%MAVEN_HOME%\bin\mvn.cmd" spring-boot:run 1>> "%LOG_OUT%" 2>> "%LOG_ERR%"
set EXIT=%ERRORLEVEL%

echo [%date% %time%] TERMINO CON EXIT=%EXIT% >> "%LOG_OUT%"
echo EXIT=%EXIT%
exit /b %EXIT%
