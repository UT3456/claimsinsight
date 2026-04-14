# Claims Metrics Engine - Microservice 3

Part of the **ClaimInsight360** insurance analytics platform.

This microservice computes and stores KPI metrics for insurance claims:
- **TAT** - Turnaround Time (days from submission to resolution)
- **CYCLE_TIME** - Total processing time through the workflow
- **SEVERITY** - Financial or operational impact score of the claim
- **FREQUENCY** - How often claims of this type occur
- **LOSS_RATIO** - Ratio of claims paid to premiums earned (for example `0.65 = 65%`)

---

## Prerequisites

| Tool  | Version | Download |
|-------|---------|----------|
| Java  | 25+     | https://adoptium.net |
| Maven | 3.8+    | https://maven.apache.org |
| MySQL | 8.0+    | https://dev.mysql.com/downloads |

---

## Step 1 - Create the MySQL Database

```sql
CREATE DATABASE claiminsight_db;
```

---

## Step 2 - Configure the Application

Open `src/main/resources/application.yml`:

```yaml
spring:
  datasource:
    username: root
    password: your_password_here
```

---

## Step 3 - Run the Application

```bash
mvn spring-boot:run
```

The app runs on **port 8083**.

---

## Step 4 - Run All Tests

```bash
mvn test
```

Tests use the H2 in-memory database and do not touch MySQL.

---

## Step 5 - Generate the JaCoCo Coverage Report

```bash
mvn test
```

JaCoCo runs automatically during the `test` phase and writes the HTML report to:

```text
target/site/jacoco/index.html
```

---

## Step 6 - Generate Javadoc

```bash
mvn javadoc:javadoc
```

Javadoc output is written to:

```text
target/reports/apidocs/index.html
```

---

## Useful URLs

| URL | Description |
|-----|-------------|
| http://localhost:8083/swagger-ui.html | Swagger UI |
| http://localhost:8083/actuator/health | Health check |
| http://localhost:8083/actuator/info | App name and version |
| http://localhost:8083/actuator/metrics | Memory, CPU, and HTTP metrics |
| http://localhost:8083/actuator/caches | Current cache contents |
| http://localhost:8083/actuator/httpexchanges | Recent HTTP request and response exchanges |

---

## API Endpoints

Base URL: `/api/kpis`

| Method | Endpoint | Description | Status |
|--------|----------|-------------|--------|
| POST   | `/api/kpis` | Record a new KPI metric | 201 |
| GET    | `/api/kpis` | Get all KPI records (cached) | 200 |
| GET    | `/api/kpis/{kpiId}` | Get one record by ID (cached) | 200 |
| GET    | `/api/kpis/claim/{claimId}` | Get all KPIs for a claim (cached) | 200 |
| GET    | `/api/kpis/metric/{metricName}` | Get all KPIs by metric type (cached) | 200 |
| GET    | `/api/kpis/claim/{claimId}/metric/{metricName}` | Get KPIs by claim and metric | 200 |
| GET    | `/api/kpis/date-range?start=&end=` | Get KPIs in a date range | 200 |
| POST   | `/api/kpis/calculate/{claimId}` | Calculate and save all KPI metrics for a claim | 201 |
| DELETE | `/api/kpis/{kpiId}` | Delete a KPI record | 204 |

---

## Sample Requests

### Record a New KPI

```json
POST /api/kpis
{
  "claimId": "CLM-20240101-001",
  "metricName": "TAT",
  "metricValue": 5.0,
  "metricDate": "2026-01-15"
}
```

### Get All TAT Metrics

```text
GET /api/kpis/metric/TAT
```

### Get KPIs for a Date Range

```text
GET /api/kpis/date-range?start=2026-01-01&end=2026-01-31
```

---

## Valid Values

| Field | Allowed Values |
|-------|----------------|
| metricName | `TAT`, `CYCLE_TIME`, `SEVERITY`, `FREQUENCY`, `LOSS_RATIO` |
| metricValue | Any number `>= 0` |
| metricDate | Any valid date in `YYYY-MM-DD` format |
| claimId | Letters, digits, hyphens, and underscores only, 3 to 100 characters |

---

## Project Structure

```text
src/main/java/com/claiminsight/metrics/
|- ClaimsMetricsApplication.java    - entry point (@EnableCaching)
|- aspect/
|  `- LoggingAspect.java            - AOP logging
|- config/
|  |- AppConfig.java                - shared beans and HTTP exchange repository
|  |- SwaggerConfig.java            - Swagger metadata
|  `- ValidationConfig.java         - validation message wiring
|- controller/
|  `- ClaimKpiController.java       - REST endpoints
|- service/
|  |- ClaimKpiService.java          - business logic and caching
|  `- KpiCalculationService.java    - KPI calculations
|- repository/
|  |- ClaimKpiRepository.java       - JPA queries
|  `- ClaimRawProjectionRepository.java
|- model/
|  |- ClaimKPI.java                 - JPA entity
|  |- ClaimRawProjection.java
|  `- MetricName.java               - KPI enum values
|- mapper/
|  `- ClaimKpiMapper.java           - DTO/entity mapping
|- dto/
|  |- ClaimKpiRequestDTO.java
|  |- ClaimKpiResponseDTO.java
|  |- ErrorResponseDTO.java
|  `- KpiSummaryDTO.java
`- exception/
   |- GlobalExceptionHandler.java
   |- InvalidMetricException.java
   `- ResourceNotFoundException.java

src/main/resources/
|- application.yml                  - DB, cache, actuator, logging, swagger
|- error.properties                 - validation error messages
|- schema.sql                       - MySQL DDL
`- data.sql                         - seed data
```

---

## Common Errors

| Error | Fix |
|-------|-----|
| `Access denied` | Check MySQL credentials in `application.yml` |
| `Unknown database` | Run `CREATE DATABASE claiminsight_db;` |
| `Port 8083 in use` | Change `server.port` in `application.yml` |
| `/actuator/health` is DOWN | Make sure MySQL is running |
