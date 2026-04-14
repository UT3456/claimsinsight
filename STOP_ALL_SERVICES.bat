@echo off
REM ============================================================================
REM ClaimInsight360 - Stop All Microservices
REM This script stops all running microservices and frees up their ports
REM ============================================================================

setlocal enabledelayedexpansion

echo.
echo ============================================================================
echo ClaimInsight360 - Stop All Services
echo ============================================================================
echo.

REM Define all service ports
set "ports=8761 8086 8082 8083 8090 8085 8087 8089 8088 8084 9411"
set "service_names=eureka-server api-gateway data-ingestion-service claims-metrics-service fraud-risk-service denial-leakage-service AdjusterAndOperations cost-reserve-service NotificationService analytics-report-service Zipkin"

echo [*] Stopping all Java processes...
echo.

REM Force kill all java.exe processes
taskkill /F /IM java.exe >nul 2>&1
if %ERRORLEVEL% EQU 0 (
    echo [✓] Java processes terminated
) else (
    echo [!] No Java processes found to terminate
)

echo.
echo [*] Waiting 3 seconds for ports to be released...
timeout /t 3 /nobreak

echo.
echo [*] Verifying ports are free...
echo.

REM Check each port
set "count=0"
for %%P in (%ports%) do (
    netstat -ano | findstr ":%%P" >nul 2>&1
    if !ERRORLEVEL! EQU 0 (
        echo [X] Port %%P still in use
        set /a "count+=1"
    ) else (
        echo [✓] Port %%P is FREE
    )
)

echo.
if %count% EQU 0 (
    echo ============================================================================
    echo [✓] SUCCESS: All services stopped and all ports are free!
    echo ============================================================================
) else (
    echo ============================================================================
    echo [!] WARNING: %count% ports still in use - Manual cleanup may be needed
    echo ============================================================================
)

echo.
echo [*] Service Summary:
echo     - Eureka Server (8761)
echo     - API Gateway (8086)
echo     - Data Ingestion Service (8082)
echo     - Claims Metrics Service (8083)
echo     - Fraud Risk Service (8090)
echo     - Denial Leakage Service (8085)
echo     - Adjuster And Operations (8087)
echo     - Cost Reserve Service (8089)
echo     - Notification Service (8088)
echo     - Analytics Report Service (8084)
echo     - Zipkin Tracing (9411)
echo.

pause
