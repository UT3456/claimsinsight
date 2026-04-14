# ============================================================================
# ClaimInsight360 - Stop All Microservices (PowerShell Version)
# This script provides advanced options for stopping services
# ============================================================================

param(
    [switch]$Force = $false,
    [switch]$ShowPorts = $false
)

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "ClaimInsight360 - Stop All Services" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

$portMap = @{
    'eureka-server'             = 8761
    'api-gateway'               = 8086
    'data-ingestion-service'    = 8082
    'claims-metrics-service'    = 8083
    'fraud-risk-service'        = 8090
    'denial-leakage-service'    = 8085
    'AdjusterAndOperations'     = 8087
    'cost-reserve-service'      = 8089
    'NotificationService'       = 8088
    'analytics-report-service'  = 8084
    'Zipkin'                    = 9411
}

Write-Host "[*] Stopping all Java processes..." -ForegroundColor Yellow

try {
    Get-Process java -ErrorAction Stop | Stop-Process -Force -ErrorAction Stop
    Write-Host "[✓] Java processes terminated" -ForegroundColor Green
}
catch {
    Write-Host "[!] No Java processes found or already stopped" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "[*] Waiting 3 seconds for ports to be released..." -ForegroundColor Yellow
Start-Sleep -Seconds 3

Write-Host ""
Write-Host "[*] Verifying port status..." -ForegroundColor Yellow
Write-Host ""

$freedPorts = 0
$busyPorts = 0

foreach ($service in $portMap.GetEnumerator() | Sort-Object Value) {
    $port = $service.Value
    $name = $service.Key
    
    $connection = Test-NetConnection -ComputerName '127.0.0.1' -Port $port -WarningAction SilentlyContinue -InformationLevel Quiet
    
    if ($connection) {
        Write-Host "  [X] Port $port - $name (STILL IN USE)" -ForegroundColor Red
        $busyPorts++
    } else {
        Write-Host "  [✓] Port $port - $name (FREE)" -ForegroundColor Green
        $freedPorts++
    }
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
if ($busyPorts -eq 0) {
    Write-Host "[✓] SUCCESS: All services stopped!" -ForegroundColor Green
    Write-Host "    Freed $freedPorts ports" -ForegroundColor Green
} else {
    Write-Host "[!] WARNING: $busyPorts ports still in use" -ForegroundColor Yellow
    Write-Host "    $freedPorts ports freed" -ForegroundColor Yellow
    
    if ($Force) {
        Write-Host ""
        Write-Host "[*] Force killing remaining processes..." -ForegroundColor Yellow
        taskkill /F /IM java.exe 2>$null
        Start-Sleep -Seconds 2
        Write-Host "[✓] Force kill completed" -ForegroundColor Green
    }
}
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

Write-Host "Summary:"
Write-Host "  Total Services: 11"
Write-Host "  Ports Freed: $freedPorts"
Write-Host "  Ports Still Busy: $busyPorts"
Write-Host ""

if ($ShowPorts) {
    Write-Host "Port Details:"
    netstat -ano | findstr "LISTENING"
    Write-Host ""
}

Write-Host "Usage:"
Write-Host "  .\STOP_ALL_SERVICES.ps1              # Stop services normally"
Write-Host "  .\STOP_ALL_SERVICES.ps1 -Force       # Force kill if ports still busy"
Write-Host "  .\STOP_ALL_SERVICES.ps1 -ShowPorts   # Show detailed port info"
Write-Host ""
