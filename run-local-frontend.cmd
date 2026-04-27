@echo off
setlocal

set "PORT_PID="
for /f %%i in ('powershell -NoProfile -Command "$conn = Get-NetTCPConnection -LocalPort 5331 -State Listen -ErrorAction SilentlyContinue | Select-Object -First 1 -ExpandProperty OwningProcess; if ($conn) { $conn }"') do set "PORT_PID=%%i"
if defined PORT_PID (
    echo Port 5331 is already in use by PID %PORT_PID%.
    powershell -NoProfile -Command "Get-Process -Id %PORT_PID% | Select-Object Id, ProcessName, Path | Format-Table -AutoSize"
    exit /b 1
)

set "VITE_API_BASE_URL=http://localhost:8081/api"

echo Starting frontend with:
echo   VITE_API_BASE_URL=%VITE_API_BASE_URL%
echo.

cd /d "%~dp0frontend"
npm.cmd run dev

endlocal
