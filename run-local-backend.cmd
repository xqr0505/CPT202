@echo off
setlocal

set "PORT_PID="
for /f %%i in ('powershell -NoProfile -Command "$conn = Get-NetTCPConnection -LocalPort 8081 -State Listen -ErrorAction SilentlyContinue | Select-Object -First 1 -ExpandProperty OwningProcess; if ($conn) { $conn }"') do set "PORT_PID=%%i"
if defined PORT_PID (
    echo Port 8081 is already in use by PID %PORT_PID%.
    powershell -NoProfile -Command "Get-Process -Id %PORT_PID% | Select-Object Id, ProcessName, Path | Format-Table -AutoSize"
    exit /b 1
)

set "SPRING_PROFILES_ACTIVE=dev"
set "DB_HOST=127.0.0.1"
set "DB_PORT=9001"
set "DB_USERNAME=root"
set "DB_PASSWORD=Root@123"
set "REDIS_HOST=127.0.0.1"
set "REDIS_PORT=9002"
set "AI_RAG_REDIS_HOST=127.0.0.1"
set "AI_RAG_REDIS_PORT=9002"

echo Starting backend with:
echo   DB    = %DB_HOST%:%DB_PORT%
echo   Redis = %REDIS_HOST%:%REDIS_PORT%
echo.

cd /d "%~dp0backend"
mvn.cmd spring-boot:run -Dspring-boot.run.profiles=dev

endlocal
