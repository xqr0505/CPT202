@echo off
setlocal

rem Keep %~dp0 pointing at repository root\scripts\
echo Starting Docker services: db + redis
docker compose -f "%~dp0..\\docker-compose.dev.yml" up -d db redis
if errorlevel 1 exit /b 1

echo.
echo Opening backend terminal...
start "backend-local" cmd /k call "%~dp0run-local-backend.cmd"

echo Opening frontend terminal...
start "frontend-local" cmd /k call "%~dp0run-local-frontend.cmd"

echo.
echo Local dev flow started:
echo   Frontend: http://localhost:5331
echo   Backend : http://localhost:8081
echo   Swagger : http://localhost:8081/swagger-ui/index.html
echo.
echo If backend still reports FT._LIST unknown, it is not connected to Docker Redis on 127.0.0.1:9002.

endlocal
