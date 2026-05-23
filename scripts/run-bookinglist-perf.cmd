@echo off
setlocal enabledelayedexpansion

if "%~1"=="" goto :usage

set "AUTH_TOKEN=%~1"
set "THREADS=%~2"
set "RAMP_UP=%~3"
set "LOOPS=%~4"
set "BASE_HOST=%~5"
set "BASE_PORT=%~6"
set "BASE_PROTOCOL=%~7"

if not defined THREADS set "THREADS=20"
if not defined RAMP_UP set "RAMP_UP=20"
if not defined LOOPS set "LOOPS=180"
if not defined BASE_HOST set "BASE_HOST=127.0.0.1"
if not defined BASE_PORT set "BASE_PORT=8081"
if not defined BASE_PROTOCOL set "BASE_PROTOCOL=http"

set "SCRIPT_DIR=%~dp0"
set "ROOT_DIR=%SCRIPT_DIR%.."
set "JMX_FILE=%ROOT_DIR%\backend\perf\jmeter\booking-list-tab-toggle.jmx"
set "RESULT_ROOT=%ROOT_DIR%\backend\perf\jmeter\results"
set "REPORT_ROOT=%ROOT_DIR%\backend\perf\jmeter\reports"
set "RUN_ID=%DATE:~0,4%%DATE:~5,2%%DATE:~8,2%_%TIME:~0,2%%TIME:~3,2%%TIME:~6,2%"
set "RUN_ID=%RUN_ID: =0%"
set "RUN_DIR=%RESULT_ROOT%\run_%RUN_ID%"

if not exist "%JMX_FILE%" (
    echo ERROR: JMeter plan not found:
    echo   %JMX_FILE%
    exit /b 1
)

if defined JMETER_CMD (
    if exist "%JMETER_CMD%" goto :jmeter_resolved
)
set "JMETER_CMD="
for /f "usebackq delims=" %%I in (`where.exe jmeter.bat 2^>nul`) do (
    if not defined JMETER_CMD set "JMETER_CMD=%%I"
)
for /f "usebackq delims=" %%I in (`where.exe jmeter.cmd 2^>nul`) do (
    if not defined JMETER_CMD set "JMETER_CMD=%%I"
)
for /f "usebackq delims=" %%I in (`where.exe jmeter.exe 2^>nul`) do (
    if not defined JMETER_CMD set "JMETER_CMD=%%I"
)
if not defined JMETER_CMD (
    for /f "usebackq delims=" %%I in (`powershell -NoProfile -Command "$c = Get-Command jmeter -ErrorAction SilentlyContinue; if ($c -and $c.Source) { $c.Source }"`) do (
        if not defined JMETER_CMD set "JMETER_CMD=%%I"
    )
)
if not defined JMETER_CMD (
    if defined JMETER_HOME (
        if exist "%JMETER_HOME%\bin\jmeter.bat" (
            set "JMETER_CMD=%JMETER_HOME%\bin\jmeter.bat"
        )
    )
)
if not defined JMETER_CMD (
    echo ERROR: JMeter not found.
    echo Please install Apache JMeter 5.6+ or set JMETER_HOME/JMETER_CMD.
    exit /b 1
)
:jmeter_resolved

if not exist "%RUN_DIR%" mkdir "%RUN_DIR%"
if not exist "%REPORT_ROOT%" mkdir "%REPORT_ROOT%"

set "WARMUP_JTL=%RUN_DIR%\warmup.jtl"
set "ON_JTL=%RUN_DIR%\list-cache-on.jtl"
set "OFF_JTL=%RUN_DIR%\list-cache-off.jtl"
set "ON_REPORT=%REPORT_ROOT%\run_%RUN_ID%_list-cache-on"
set "OFF_REPORT=%REPORT_ROOT%\run_%RUN_ID%_list-cache-off"

echo ================================================
echo Booking List Perf A/B Runner
echo ================================================
echo Run ID         : %RUN_ID%
echo JMX            : %JMX_FILE%
echo Base URL       : %BASE_PROTOCOL%://%BASE_HOST%:%BASE_PORT%
echo Threads        : %THREADS%
echo Ramp-up(sec)   : %RAMP_UP%
echo Loops          : %LOOPS%
echo Result Dir     : %RUN_DIR%
echo.

echo [1/4] Warm-up (30s profile)
echo JMETER_CMD=%JMETER_CMD%
call "%JMETER_CMD%" -n -t "%JMX_FILE%" ^
  -Jbase_protocol=%BASE_PROTOCOL% ^
  -Jbase_host=%BASE_HOST% ^
  -Jbase_port=%BASE_PORT% ^
  -Jthreads=10 ^
  -Jramp_up=10 ^
  -Jloops=30 ^
  -Jauth_token=%AUTH_TOKEN% ^
  -l "%WARMUP_JTL%"
set "WARMUP_EXIT=%errorlevel%"
echo Warm-up exit code: %WARMUP_EXIT%
if errorlevel 1 goto :jmeter_failed

echo.
echo [2/4] Booking list cache ON benchmark
echo Ensure backend is running with BOOKING_CACHE_LIST_ENABLED=true
call "%JMETER_CMD%" -n -t "%JMX_FILE%" ^
  -Jbase_protocol=%BASE_PROTOCOL% ^
  -Jbase_host=%BASE_HOST% ^
  -Jbase_port=%BASE_PORT% ^
  -Jthreads=%THREADS% ^
  -Jramp_up=%RAMP_UP% ^
  -Jloops=%LOOPS% ^
  -Jauth_token=%AUTH_TOKEN% ^
  -l "%ON_JTL%" ^
  -e -o "%ON_REPORT%"
set "ON_EXIT=%errorlevel%"
echo Booking list cache ON exit code: %ON_EXIT%
if errorlevel 1 goto :jmeter_failed

echo.
echo [3/4] Switch booking list cache OFF
echo Restart backend with BOOKING_CACHE_LIST_ENABLED=false, then press any key.
pause >nul

echo.
echo [4/4] Booking list cache OFF benchmark
call "%JMETER_CMD%" -n -t "%JMX_FILE%" ^
  -Jbase_protocol=%BASE_PROTOCOL% ^
  -Jbase_host=%BASE_HOST% ^
  -Jbase_port=%BASE_PORT% ^
  -Jthreads=%THREADS% ^
  -Jramp_up=%RAMP_UP% ^
  -Jloops=%LOOPS% ^
  -Jauth_token=%AUTH_TOKEN% ^
  -l "%OFF_JTL%" ^
  -e -o "%OFF_REPORT%"
set "OFF_EXIT=%errorlevel%"
echo Booking list cache OFF exit code: %OFF_EXIT%
if errorlevel 1 goto :jmeter_failed

echo.
echo ================================================
echo Completed.
echo List cache ON report : %ON_REPORT%
echo List cache OFF report: %OFF_REPORT%
echo Result files         : %RUN_DIR%
echo Acceptance rule      : p95_cache_on ^<= p95_cache_off * 0.70
echo ================================================
exit /b 0

:jmeter_failed
echo.
echo ERROR: JMeter execution failed.
echo Check generated files under: %RUN_DIR%
exit /b 1

:usage
echo Usage:
echo   %~nx0 ^<AUTH_TOKEN^> [THREADS] [RAMP_UP] [LOOPS] [BASE_HOST] [BASE_PORT] [BASE_PROTOCOL]
echo.
echo Example:
echo   %~nx0 eyJhbGciOiJIUzI1Ni... 20 20 180 127.0.0.1 8081 http
exit /b 1

