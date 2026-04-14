# ClaimInsight360 - Service Control Scripts

This directory contains scripts to manage all ClaimInsight360 microservices.

## Scripts Available

### 1. START_ALL_SERVICES.ps1
Starts all 11 microservices in separate PowerShell windows.

**Usage:**
```powershell
.\START_ALL_SERVICES.ps1
```

**Services Started:**
- Eureka Server (8761)
- API Gateway (8086)
- Data Ingestion Service (8082)
- Claims Metrics Service (8083)
- Fraud Risk Service (8090)
- Denial Leakage Service (8085)
- Adjuster And Operations (8087)
- Cost Reserve Service (8089)
- Notification Service (8088)
- Analytics Report Service (8084)
- Zipkin (9411)

---

### 2. STOP_ALL_SERVICES.bat (Batch File)
Simple batch file to stop all Java processes and free ports.

**Usage:**
```cmd
STOP_ALL_SERVICES.bat
```

**What it does:**
- Terminates all java.exe processes
- Verifies all ports are free
- Shows port status summary

**Ports freed:**
- 8761, 8086, 8082, 8083, 8090, 8085, 8087, 8089, 8088, 8084, 9411

---

### 3. STOP_ALL_SERVICES.ps1 (PowerShell)
Advanced PowerShell script with additional options.

**Usage:**
```powershell
# Stop services normally
.\STOP_ALL_SERVICES.ps1

# Force kill if ports still busy
.\STOP_ALL_SERVICES.ps1 -Force

# Show detailed port information
.\STOP_ALL_SERVICES.ps1 -ShowPorts

# Combine options
.\STOP_ALL_SERVICES.ps1 -Force -ShowPorts
```

**Features:**
- Graceful service termination
- Port status verification
- Force kill option
- Detailed reporting
- Port monitoring

---

## Port Mapping

| Port | Service |
|------|---------|
| 8761 | eureka-server |
| 8086 | api-gateway |
| 8082 | data-ingestion-service |
| 8083 | claims-metrics-service |
| 8090 | fraud-risk-service |
| 8085 | denial-leakage-service |
| 8087 | AdjusterAndOperations |
| 8089 | cost-reserve-service |
| 8088 | NotificationService |
| 8084 | analytics-report-service |
| 9411 | Zipkin (distributed tracing) |

---

## Environment Details

- **Java Version:** 21 LTS
- **Spring Boot:** 3.5.11 (standardized)
- **Spring Cloud:** 2025.0.0
- **Database:** MySQL 8.0.44
- **IDE:** IntelliJ IDEA 2025.3.1.1

---

## Quick Start

1. **Start all services:**
   ```powershell
   .\START_ALL_SERVICES.ps1
   ```

2. **Access services:**
   - Eureka Registry: http://localhost:8761
   - API Gateway: http://localhost:8086
   - Zipkin UI: http://localhost:9411/zipkin/

3. **Stop all services:**
   ```cmd
   .\STOP_ALL_SERVICES.bat
   ```
   OR
   ```powershell
   .\STOP_ALL_SERVICES.ps1
   ```

---

## Troubleshooting

### Ports Still In Use
If ports are still in use after running the stop script:

**Option 1: Force Kill (PowerShell)**
```powershell
.\STOP_ALL_SERVICES.ps1 -Force
```

**Option 2: Manual Port Release (PowerShell)**
```powershell
# Find process using specific port (e.g., 8086)
netstat -ano | findstr ":8086"

# Kill the process by PID
taskkill /PID <PID> /F
```

**Option 3: Manual Port Release (Command Prompt)**
```cmd
# Find and kill process on port
netstat -ano | findstr ":8086"
taskkill /PID <PID> /F
```

### Service Won't Start
- Ensure Java 21 is installed: `java -version`
- Check MySQL is running: `mysql -u root -proot -e "SELECT 1;"`
- Verify all ports are free before starting
- Check service logs in respective `/logs` directories

---

## IntelliJ IDEA Integration

All services can also be run directly from IntelliJ using the configured Run Configurations:
- View > Tool Windows > Run
- Select individual service configs or "ClaimInsight360_Start_All_Services" for all 10 services

---

## Database Setup

Initial sample data has been loaded. To reload:
```powershell
& "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe" -u root -proot < insert_realistic_data.sql
```

---

## Support

For issues or questions, check the individual service logs:
- `<service-name>/logs/boot.out.log`
- All services write to console and log files

---

**Last Updated:** April 14, 2026  
**System:** Windows 10/11  
**Status:** All 11/11 services operational
