# ClaimInsight360 - Start All Microservices
# This script starts all microservices in separate PowerShell windows

$workspace = "C:\Users\2478140\Downloads\cali"

# Define all services with their names and paths
$services = @(
    @{Name = "eureka-server"; Path = "$workspace\eureka-server"; Port = "8761"},
    @{Name = "api-gateway"; Path = "$workspace\api-gateway"; Port = "8080"},
    @{Name = "fraud-risk-service"; Path = "$workspace\fraud-risk-service"; Port = "8090"},
    @{Name = "AdjusterAndOperations"; Path = "$workspace\AdjusterAndOperations"; Port = "8081"},
    @{Name = "analytics-report-service"; Path = "$workspace\analytics-report-service"; Port = "8082"},
    @{Name = "claims-metrics-service"; Path = "$workspace\claims-metrics-service"; Port = "8083"},
    @{Name = "data-ingestion-service"; Path = "$workspace\data-ingestion-service"; Port = "8084"},
    @{Name = "cost-reserve-service"; Path = "$workspace\cost-reserve-service"; Port = "8085"},
    @{Name = "denial-leakage-service"; Path = "$workspace\denial-leakage-service"; Port = "8086"},
    @{Name = "NotificationService"; Path = "$workspace\NotificationService"; Port = "8087"}
)

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "ClaimInsight360 - Microservices Startup" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Start each service
foreach ($service in $services) {
    $serviceName = $service.Name
    $servicePath = $service.Path
    $port = $service.Port

    if (Test-Path "$servicePath\pom.xml") {
        Write-Host "Starting $serviceName (Port: $port)..." -ForegroundColor Green

        # Start in a new PowerShell window
        # JAVA_TOOL_OPTIONS: -XX:TieredStopAtLevel=1 skips full JIT (C2 compiler) for ~2x faster startup
        # at cost of slower runtime throughput — ideal for dev. Remove for production.
        Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd '$servicePath'; Write-Host 'Starting $serviceName on port $port...'; `$env:JAVA_TOOL_OPTIONS='-XX:TieredStopAtLevel=1 -Xms64m'; mvn spring-boot:run" -WindowStyle Normal

        # Small delay to prevent overwhelming the system
        Start-Sleep -Seconds 2
    } else {
        Write-Host "⚠ Skipping $serviceName - pom.xml not found at $servicePath" -ForegroundColor Yellow
    }
}

# Start Zipkin for distributed tracing
Write-Host ""
Write-Host "Starting Zipkin Server..." -ForegroundColor Magenta
$zipkinPath = "$env:USERPROFILE\Downloads\zipkin"
$zipkinJar = Get-ChildItem -Path $zipkinPath -Filter "zipkin*.jar" -ErrorAction SilentlyContinue | Select-Object -First 1

if ($zipkinJar) {
    Write-Host "Found Zipkin JAR: $($zipkinJar.Name)" -ForegroundColor Green
    Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd '$zipkinPath'; java -jar '$($zipkinJar.Name)'" -WindowStyle Normal
    Start-Sleep -Seconds 3
} else {
    Write-Host "⚠ Zipkin JAR not found in $zipkinPath" -ForegroundColor Yellow
    Write-Host "Expected a file matching: zipkin*.jar" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "All services started!" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Service Registry: http://localhost:8761" -ForegroundColor Yellow
Write-Host "API Gateway: http://localhost:8080" -ForegroundColor Yellow
Write-Host "Zipkin UI: http://localhost:9411" -ForegroundColor Yellow
Write-Host ""

