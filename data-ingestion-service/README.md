# Data Ingestion & Claim Data Aggregator — Microservice 2

Part of the **ClaimInsight360** insurance analytics platform.

This microservice is responsible for:
- Registering data feed sources (Claims, Policy, Payment, Reserve systems)
- Accepting and storing raw claim JSON payloads from those sources
- Providing REST APIs to query and manage the ingested data

---

## Prerequisites

| Tool  | Version | Download |
|-------|---------|----------|
| Java  | 17+     | https://adoptium.net |
| Maven | 3.8+    | https://maven.apache.org |
| MySQL | 8.0+    | https://dev.mysql.com/downloads |

---

## Step 1 — Create the MySQL Database

```sql
CREATE DATABASE claiminsight_db;
```

---

## Step 2 — Configure the Application

Open `src/main/resources/application.properties`:
```properties
spring.datasource.username=root
spring.datasource.password=your_password_here
```

---

## Step 3 — Run the Application

```bash
mvn spring-boot:run
```

---

## Step 4 — Run All Tests

```bash
mvn test
```

Tests use H2 in-memory database — they never touch MySQL.

---

## Step 5 — Generate JaCoCo Test Coverage Report

```bash
mvn test
```

After tests complete, open the coverage report in your browser:
```
target/site/jacoco/index.html
```

The report shows:
- Which lines of code are covered by tests (green)
- Which lines are not covered (red)
- Overall coverage percentage per class and package

---

## Step 6 — Generate Javadoc API Documentation

```bash
mvn javadoc:javadoc
```

After generation, open the HTML documentation in your browser:
```
target/site/apidocs/index.html
```

The documentation is generated from the `/** ... */` Javadoc comments in every class.

---

## Step 7 — Open Swagger UI

```
http://localhost:8082/swagger-ui.html
```

---

## Actuator Monitoring Endpoints

| URL | Description |
|-----|-------------|
| http://localhost:8082/actuator/health   | Database and app health status |
| http://localhost:8082/actuator/info     | App name, version, description |
| http://localhost:8082/actuator/metrics  | Memory, CPU, HTTP request counts |
| http://localhost:8082/actuator/caches   | Current cache contents |

---

## All API Endpoints

### DataFeed APIs — `/api/feeds`

| Method | Endpoint | Description | Status |
|--------|----------|-------------|--------|
| POST   | `/api/feeds` | Register a new feed | 201 |
| GET    | `/api/feeds` | Get all feeds (cached) | 200 |
| GET    | `/api/feeds/{feedId}` | Get feed by ID (cached) | 200 |
| PUT    | `/api/feeds/{feedId}/status` | Update feed status | 200 |
| DELETE | `/api/feeds/{feedId}` | Delete feed | 204 |

### Ingestion APIs — `/api/ingest`

| Method | Endpoint | Description | Status |
|--------|----------|-------------|--------|
| POST   | `/api/ingest` | Ingest a raw claim | 201 |
| GET    | `/api/ingest/raw-claims` | Get all records (cached) | 200 |
| GET    | `/api/ingest/raw-claims/{claimId}` | Get by claim ID (cached) | 200 |
| GET    | `/api/ingest/raw-claims/feed/{feedId}` | Get by feed ID (cached) | 200 |
| DELETE | `/api/ingest/raw-claims/{rawId}` | Delete a record | 204 |

---

## Sample Requests

### Register a feed
```json
POST /api/feeds
{
  "feedType": "CLAIM",
  "sourceSystem": "ClaimsPro v3",
  "status": "ACTIVE"
}
```

### Ingest a claim
```json
POST /api/ingest
{
  "claimId": "CLM-20240101-001",
  "feedId": 1,
  "payloadJson": "{\"claimNumber\":\"CLM-001\",\"amount\":5000}"
}
```

---

## Project Structure

```
src/main/java/com/claiminsight/ingestion/
├── DataIngestionApplication.java   — entry point (@EnableCaching)
├── aspect/
│   └── LoggingAspect.java          — AOP logging (@Before @After @Around etc.)
├── config/
│   ├── SwaggerConfig.java          — ModelMapper bean + Swagger metadata
│   └── ValidationConfig.java       — Wires error.properties to Bean Validation
├── controller/                     — REST endpoints (thin, no logic)
├── service/                        — Business logic + caching
├── repository/                     — Database queries (JPA)
├── model/                          — JPA entities + enums
├── mapper/                         — DTO <-> Entity conversion (ModelMapper)
├── dto/                            — Request and response objects
└── exception/                      — GlobalExceptionHandler + custom exceptions

src/main/resources/
├── application.properties          — DB, cache, actuator, logging config
├── error.properties                — Centralised validation error messages
└── schema.sql                      — Manual DDL script for MySQL
```

---

## Valid Values

| Field | Allowed Values |
|-------|---------------|
| feedType | `CLAIM`, `POLICY`, `PAYMENT`, `RESERVE` |
| status   | `ACTIVE`, `INACTIVE`, `FAILED` |

---

## Common Errors

| Error | Fix |
|-------|-----|
| `Access denied` | Check username/password in application.properties |
| `Unknown database` | Run `CREATE DATABASE claiminsight_db;` |
| `Port 8082 in use` | Change `server.port` in application.properties |
| `/actuator/health` shows DOWN | Check MySQL is running |
