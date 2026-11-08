@echo off
REM ============================================================
REM  boot-jar.bat  — Arranca el JAR Spring Boot con H2 (rapido).
REM  Logs en carpeta logs-boot del proyecto.
REM ============================================================
setlocal

set "JAVA_HOME=C:\Users\ac765\AppData\Local\Programs\Eclipse Adoptium\jdk-25.0.4.101-hotspot"
set "PROJECT=c:\Users\ac765\OneDrive\Desktop\Taller Mecanico"
set "PATH=%JAVA_HOME%\bin;%PATH%"
set "JAR=%PROJECT%\backend\target\taller-mecanico-0.0.1-SNAPSHOT.jar"
set "LOGDIR=%PROJECT%\logs-boot"
if not exist "%LOGDIR%" mkdir "%LOGDIR%"
set "LOG=%LOGDIR%\boot.log"

echo [BOOT] JAVA_HOME=%JAVA_HOME%
echo [BOOT] JAR=%JAR%
echo [BOOT] LOG=%LOG%
echo [%date% %time%] INICIANDO > "%LOG%"

REM ======= Cargar .env =======
cd /d "%PROJECT%"
if exist ".env" (
  for /f "usebackq tokens=*" %%L in (".env") do (
    set "LINE=%%L"
    setlocal enabledelayedexpansion
    if not "!LINE:~0,1!"=="#" if not "!LINE!"=="" (
      for /f "tokens=1,* delims==" %%A in ("!LINE!") do (
        endlocal
        set "%%A=%%B"
      )
    ) else (
      endlocal
    )
  )
  echo [BOOT] .env cargado
)

REM ======= H2 fallback (sin Docker) =======
set DB_URL=
set DB_DRIVER=
set DB_USERNAME=
set DB_DIALECT=
set DB_DDL_AUTO=create-drop

echo [BOOT] Admin email=%APP_ADMIN_EMAIL%
echo [BOOT] Admin pass.len:
REM Contar longitud de password
setlocal enabledelayedexpansion
set "L=0"
:loop
if defined APP_ADMIN_PASSWORD (
  set "APP_ADMIN_PASSWORD=!APP_ADMIN_PASSWORD:~1!"
  set /a L+=1
  goto loop
)
echo %L%
endlocal

REM ======= EJECUTAR =======
cd /d "%PROJECT%"
echo [%date% %time%] EJECUTANDO java -jar ... >> "%LOG%"
"%JAVA_HOME%\bin\java.exe" -Xmx512m -XX:MaxRAMPercentage=75 --enable-native-access=ALL-UNNAMED -jar "%JAR%" >> "%LOG%" 2>&1
set EX=%ERRORLEVEL%
echo [%date% %time%] TERMINADO EXIT=%EX% >> "%LOG%"
exit /b %EX%
