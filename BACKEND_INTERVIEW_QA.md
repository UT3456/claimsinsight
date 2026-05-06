# Backend — Detailed Interview Q&A

This document contains detailed interview questions and answers about the ClaimInsight360 backend, based on scanning the repository in `C:/Users/2478140/Downloads/cali`.

Use this to prepare for backend/system interviews or to onboard backend engineers.

---

## Short plan

- Provide an overview of architecture and components
- Explain authentication & auth flow (gateway + JWT)
- Describe inter-service communication (Feign, Eureka)
- Describe important cross-service flows (ingestion → KPIs → downstream tasks)
- Explain persistence, caching, and performance patterns (JPA batching, indexes, Caffeine)
- Discuss error handling, retries, and resiliency patterns
- Provide common interview questions with model answers
- Give practical commands and debugging tips for local development

---

## Architecture overview

Q: What is the overall backend architecture of ClaimInsight360?

A:
- Microservices-based architecture running multiple Spring Boot services.
- Service discovery using Eureka (`eureka-server`).
- API Gateway (`api-gateway`) is the single entry point for frontend traffic; it routes to backend services and issues JWTs (dev-mode secret in config).
- Each business capability is its own Spring Boot service with its own DB schema. There are no cross-schema foreign keys — services communicate via REST/Feign and share string `claimId` references.
- Key services:
  - `data-ingestion-service` (ingest claims)
  - `claims-metrics-service` (KPIs)
  - `fraud-risk-service` (risk scoring)
  - `denial-leakage-service` (denial patterns)
  - `cost-reserve-service` (costs & reserves)
  - `analytics-report-service` (reports)
  - `NotificationService` (alerts/notifications)
  - `AdjusterAndOperations` (SLA violations, adjuster performance)
- Frontend communicates with the gateway; gateway forwards to services.

---

## Authentication & Authorization

Q: How is auth handled across the stack?

A:
- The API Gateway handles login and issues JWTs containing user claims (username, role, userId, expiresIn). The JWT secret in `api-gateway` is hardcoded in dev configuration.
- Frontend stores the JWT and sends it as `Authorization: Bearer <token>` on every request. The shared `axiosInstance` adds this header.
- Downstream services trust the gateway-issued JWT for auth/role checks. Services (if implemented) can check role claims or use Spring Security to enforce authorization.
- Token refresh is supported: `POST /auth/refresh` returns a new token pair; clients use stored refresh token.

Security notes:
- In production you would not hardcode secrets in the gateway; use a secure secret store.
- Services do not revalidate token against a DB — they trust the signed JWT from the gateway.

---

## Service discovery and inter-service communication

Q: How do services find and call each other?

A:
- Services register with `eureka-server` and are discoverable by name.
- Services call each other using **Feign clients** annotated with `@FeignClient(name = "<service-name>", path = "/api/...", contextId = "...")`.
- Convention: Feign clients are injected with `@Autowired(required = false)` so that when a downstream service is down the caller still starts; calls are tried in try/catch and failures logged as WARN.
- When two Feign beans use the same `name`, a unique `contextId` is required to allow multiple beans for different paths.

Example:
- `analytics-report-service` may call `cost-reserve-service` via a Feign client. The client is annotated with the Eureka service name and path.

---

## Important cross-service flow: ingestion → KPI → downstream tasks

Q: Describe the claim ingestion auto-calculation flow.

A:
When a claim is ingested via `POST /api/ingest` in `data-ingestion-service`:
1. The service persists raw claim payload to its DB.
2. Synchronously trigger KPI calculation for the claim (`claims-metrics-service`): this is required because fraud rules read `SEVERITY` KPI — must be available.
3. In parallel (CompletableFuture.allOf) trigger additional downstream workflows (non-blocking):
   - Fraud evaluation (`fraud-risk-service`): `POST /api/risk-scores/auto-evaluate` — runs rules including HighCost (reads KPI) and UnusualTiming (payload dates), persists risk indicators and scores, and possibly alerts NotificationService.
   - Cost initialization (`cost-reserve-service`): creates ClaimCost and ClaimReserve rows.
   - Denial analysis (`denial-leakage-service`): creates DenialPattern and LeakageFlag.
   - SLA violation generation (`AdjusterAndOperations`): creates SLA violation record.
4. Feign clients in `data-ingestion-service` are `@Autowired(required = false)` and calls are wrapped in try/catch. Failures are logged but do not abort ingestion — this is important for resilience.

Why synchronous KPI calc first? Because fraud rules need the computed SEVERITY KPI to run deterministically.

---

## Persistence, batching, and indexes

Q: What database and ORM patterns are used?

A:
- Each service uses Spring Data JPA (Hibernate) with H2 / MySQL in development configs (service-specific DB names are documented in CLAUDE.md).
- `spring.jpa.properties.hibernate.jdbc.batch_size` is set (e.g., 50) and `order_inserts`, `order_updates` are enabled — this enables JDBC batching.
- Code uses `repository.saveAll(list)` for multi-row inserts to leverage batching.
- Entities include `@Index` annotations on queried columns so Hibernate will create indexes on startup (with `ddl-auto: update`).

Best practices in repo:
- When adding `@Column` used in `findBy*` methods add a corresponding `@Index` in the entity table annotation.

---

## Caching

Q: How is caching configured?

A:
- All business services use Caffeine as Spring Cache provider.
- Configured in `application.yml` of each service:

```yaml
spring:
  cache:
    type: caffeine
    caffeine:
      spec: maximumSize=5000,expireAfterWrite=5m
```
- NotificationService uses a 2 minute TTL in its service-specific config.
- Cache annotations include `@Cacheable`, `@CacheEvict` patterns. Ingestion evicts raw claims caches on ingest, etc.

---

## Error handling & resiliency patterns

Q: How are errors and downstream failures handled?

A:
- Feign calls are wrapped in try/catch; many downstream calls are non-fatal and logged as WARN.
- `axiosInstance` on frontend normalizes error envelopes from backend and supports retry via refresh token when 401 is seen.
- For ingestion flows, the failure of downstream optional services is tolerated — ingestion returns 201 even if some downstream tasks failed.
- Services often return friendly envelopes like `{timestamp, status, error, message}`; `NotificationService` uses `{success, message, data}` which is a special case and normalized by the frontend.

---

## Notifications and scheduled jobs

Q: How are notifications generated?

A:
- `NotificationService` contains scheduled jobs that poll other services at intervals (5–10 minutes) to gather events to notify users (risk alerts, aging, SLA violations).
- It exposes endpoints to query unread counts and notifications which the frontend polls or streams via Server-Sent Events / polling.
- Notification dispatch is role-aware and uses the Notification DB `users` mirror table to decide recipients.

---

## Operational concerns and startup order

Q: Any special startup ordering or run instructions?

A:
- Strict startup order: 1) `eureka-server`, 2) `api-gateway`, 3) business services (any order), 4) frontend.
- Use the provided scripts:
  - `START_ALL_SERVICES.ps1` — starts all services in separate PowerShell windows
  - `STOP_ALL_SERVICES.ps1` / `.bat` — stop all services
- Check health with `curl http://localhost:<port>/actuator/health`.
- Logs are available under each service's `logs/` folder and target build logs.

---

## Testing & local development

Q: How are unit tests and API tests executed?

A:
- Each service uses Maven `mvn test` to run unit and integration tests.
- Frontend uses Vitest / Jest for unit tests (`npm run test` in frontend project).
- There are API tests in `src/test` for the frontend api wrappers using mocked axios.

---

## Common backend interview questions (with model answers)

Q1: Explain how you would design a microservice to be resilient to downstream failures.

A (model answer):
- Use timeouts and circuit breakers for outbound calls to prevent resource exhaustion.
- Wrap calls in retry policies with exponential backoff where idempotent.
- Design operations as asynchronous/non-blocking where possible; use message queues for fire-and-forget.
- Gracefully handle and log downstream errors, return useful partial responses where appropriate.
- Add metrics and health checks to detect degradation early.

Q2: How do you handle cross-service data consistency?

A:
- Prefer eventual consistency: each service owns its own data and publishes events (or APIs) to notify others.
- Use compensating transactions for complex workflows or Saga pattern.
- Keep business keys (claimId) stable across services and avoid cross-schema FK.

Q3: What are trade-offs of Feign synchronous calls vs message-based async communication?

A:
- Feign (synchronous REST) is simpler and easier to reason about, good for request/response flows.
- For long-running or fire-and-forget tasks, async messaging (Kafka/RabbitMQ) decouples services, provides retries and backpressure handling.
- Using REST can cause caller slowdown if downstream is slow; using async enables better resilience at cost of complexity.

Q4: How would you optimize database writes for large batches?

A:
- Use JDBC batch inserts via Hibernate (set jdbc.batch_size) and call `saveAll()` to group inserts.
- Use `order_inserts`/`order_updates` to improve batching efficiency.
- Tune connection pool and commit frequency; consider bulk-native SQL for huge loads.

Q5: How do you ensure slow services do not block user requests?

A:
- Use a warmup ping for lazily initialized services (as this repo does).
- Increase timeouts only where necessary and use async processing for non-critical tasks.
- Offload heavy processing to background jobs or dedicated workers.

---

## Practical debugging checklist (end-to-end)

1. Reproduce problem on local environment (use provided sample data if needed).
2. Check frontend logs / browser network tab for failing request and full URL.
3. Use `curl` to call the gateway endpoint and check gateway logs.
4. Check Eureka dashboard (http://localhost:8761) to verify the service is registered.
5. Call the target service health endpoint `/actuator/health`.
6. Inspect service logs under `logs/` for stack traces or startup errors.
7. If Feign client issues, ensure `contextId` uniqueness when multiple Feign beans use the same name.
8. For DB issues, inspect the database and SQL logs; check for missing indexes or constraint errors.

---

## Useful commands (PowerShell)

```powershell
# From repo root
# Start all services (provided script)
.\START_ALL_SERVICES.ps1

# Stop all services
.\STOP_ALL_SERVICES.ps1 -Force

# Build a single service (skip tests)
cd fraud-risk-service; mvn clean install -DskipTests; mvn spring-boot:run

# Check health
curl http://localhost:8086/actuator/health  # gateway
curl http://localhost:8083/actuator/health  # claims-metrics

# Reload sample DB data (MySQL example)
& "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe" -u root -proot < insert_realistic_data.sql
```

---

## File-to-file map (backend)

- `eureka-server/` — service discovery
- `api-gateway/` — single entry point, auth, routing
- `data-ingestion-service/` — POST /api/ingest and fan-out orchestration
- `claims-metrics-service/` — KPI calculation and storage
- `fraud-risk-service/` — risk scoring, indicators
- `denial-leakage-service/` — denial pattern detection
- `cost-reserve-service/` — cost & reserve initialization
- `analytics-report-service/` — report generation/export
- `NotificationService/` — scheduled jobs and notification APIs

---

## Next steps

- If you want, I can add a concise "two-minute verbal summary" you can use in interviews.
- I will now save this file into the repo (already created) and can attempt to `git commit` and `git push` for you — I will show the exact commands to run locally because I cannot push with your credentials.

### Suggested local git commands to push the new docs:

```powershell
cd C:\Users\2478140\Downloads\cali
git add FRONTEND_INTERVIEW_QA.md BACKEND_INTERVIEW_QA.md
git commit -m "docs: add frontend and backend interview Q&A"
git push origin master
```

If you want, I can run the git commands here and report back, but you will likely need to authenticate with your GitHub credentials (or a token).

---

## Service-by-service: code references and targeted interview Q&A

Below are per-microservice summaries with the most important source files to review and 2–3 interview-style questions (with model answers) that reference the code. Use these to drill into the codebase before an interview.

Notes on file paths: all paths are relative to the repo root `C:\Users\2478140\Downloads\cali`.

---

### API Gateway (`api-gateway`)

Key files to read:
- `api-gateway/src/main/java/com/claiminsight/gateway/ApiGatewayApplication.java` — application entry, `@EnableAsync`.
- `api-gateway/src/main/java/com/claiminsight/gateway/identity/AuthController.java` — login/refresh endpoints (issues JWTs).
- `api-gateway/src/main/java/com/claiminsight/gateway/security/JwtAuthManager.java` and `JwtSecurityContextRepository.java` — JWT validation and integration with WebFlux security.
- `api-gateway/src/main/java/com/claiminsight/gateway/filter/AuthHeaderForwardFilter.java` — forwards auth header to downstream services.

What to look for in code:
- How `AuthController` builds `AuthResponseDTO` (JWT, refresh token) — search `AuthController` to see token creation.
- `JwtService` / `JwtAuthManager` — key places where token claims (role, userId) are extracted and validated.

Interview Qs (with model answers):
1) Q: How does the gateway issue and validate JWTs? Point to the code.
   A: The login endpoint in `AuthController` calls `AuthService`/`JwtService` to create a signed token (see `api-gateway/src/.../identity/service/JwtService.java`). The token is returned in `AuthResponseDTO`. Incoming requests are validated by `JwtAuthManager` and `JwtSecurityContextRepository` which extract the Authorization header and populate security context.

2) Q: How is the Authorization header propagated to downstream services?
   A: `AuthHeaderForwardFilter` adds or forwards the `Authorization` header on proxied requests so downstream services receive the JWT (see `api-gateway/src/.../filter/AuthHeaderForwardFilter.java`). This centralizes auth propagation.

---

### Eureka Server (`eureka-server`)

Key files:
- `eureka-server/src/main/java/com/claiminsight/eureka/EurekaServerApplication.java` — entry point, annotated `@EnableEurekaServer`.

Interview Qs:
1) Q: Why is a service registry used here? How is it started in the code?
   A: `EurekaServerApplication` uses `@EnableEurekaServer` to run the registry; services register themselves for dynamic discovery rather than hard-coded URLs.

---

### Data Ingestion Service (`data-ingestion-service`)

Key files to read:
- `data-ingestion-service/src/main/java/com/claiminsight/ingestion/controller/IngestionController.java` — `POST /api/ingest` handler.
- `data-ingestion-service/src/main/java/com/claiminsight/ingestion/service/IngestionService.java` — core orchestration logic: persist raw, call KPI calc, fan-out.
- Feign clients: `client/ClaimsMetricsServiceClient.java`, `client/FraudRiskServiceClient.java`, `client/CostReserveServiceClient.java`, `client/DenialLeakageServiceClient.java`, `client/AdjusterOperationsClient.java`.
- Repositories / mappers: `repository/ClaimRawRepository.java`, `mapper/ClaimRawMapper.java`.

What to read in code:
- `IngestionService.ingestClaim(...)` shows the 1) synchronous KPI call then 2) parallel CompletableFuture fan-out. See `IngestionService.java`.
- Notice each Feign client is `@Autowired(required = false)` in the service to avoid startup failure when target is down.

Interview Qs:
1) Q: Walk me through `IngestionController` → `IngestionService` when a claim is posted. Which method ensures KPIs are available to fraud rules?
   A: `IngestionController.ingest` delegates to `IngestionService.ingestClaim`, which first saves the raw claim and calls `claimsMetricsClient.calculateKpis(claimId)` synchronously (this completes KPI calculation). After that it triggers `autoEvaluate`/`autoInitialize` calls in parallel with `CompletableFuture`.

2) Q: How does the ingestion service avoid failing when a downstream service is temporarily unavailable?
   A: Feign clients are `@Autowired(required=false)` and calls are wrapped in try/catch; failures are logged as WARN and the ingest still returns 201. See the try/catch blocks in `IngestionService` and client injection patterns in `IngestionService` and `AppConfig`.

Code references:
- `data-ingestion-service/src/main/java/com/claiminsight/ingestion/service/IngestionService.java`
- `data-ingestion-service/src/main/java/com/claiminsight/ingestion/controller/IngestionController.java`

---

### Claims Metrics Service (`claims-metrics-service`)

Key files:
- Controllers: `controller/ClaimKpiController.java`, `controller/ClaimStatusController.java`.
- Service: `service/KpiCalculationService.java`, `service/ClaimKpiService.java`, `service/ClaimStatusService.java`.
- Repository: `repository/ClaimKpiRepository.java`, `repository/ClaimStatusRepository.java`.
- DTOs and mappers: `dto/ClaimKpiRequestDTO.java`, `dto/ClaimKpiResponseDTO.java`, `mapper/ClaimKpiMapper.java`.

What to look for:
- `KpiCalculationService.calculateForClaim(String claimId)` which builds 6 KPI entities and calls `repository.saveAll(List.of(...))` to persist in a single batch.
- `@Cacheable` annotations and `@CacheEvict` used around ingestion endpoints (see controllers).

Interview Qs:
1) Q: How are KPI rows written efficiently to the DB? Point to the code.
   A: `KpiCalculationService` constructs the 6 KPI entities and calls `claimKpiRepository.saveAll(list)` which leverages Hibernate JDBC batching configured in `application.yml` (see service `KpiCalculationService.java` and `application.yml`).

2) Q: Where is caching used and why must the ingestion endpoint evict caches?
   A: Frequently-read endpoints use `@Cacheable` (e.g., `getRawClaims`). When ingestion occurs, `@CacheEvict(value = {"rawClaims", "feeds"}, allEntries = true)` is used on the ingest handler to ensure reads reflect the new data. See `ClaimKpiController` and method annotations.

Code references:
- `claims-metrics-service/src/main/java/com/claiminsight/metrics/service/KpiCalculationService.java`
- `claims-metrics-service/src/main/java/com/claiminsight/metrics/controller/ClaimKpiController.java`

---

### Fraud Risk Service (`fraud-risk-service`)

Key files:
- Controllers: `controller/RiskScoreController.java`, `controller/RiskIndicatorController.java`, `controller/InvestigationController.java`.
- Services: `service/RiskScoreServiceImpl.java`, `service/RiskIndicatorServiceImpl.java`, `service/InvestigationService.java`.
- Repositories: `repository/RiskScoreRepository.java`, `repository/RiskIndicatorRepository.java`, `repository/InvestigationRepository.java`.
- Clients: `client/ClaimsMetricsServiceClient.java` to fetch KPIs, `client/NotificationServiceClient.java` to send alerts.

What to read:
- `RiskScoreController.autoEvaluate(...)` / `AutoEvaluateRequest` — shows how payload + KPIs are combined to evaluate rules.
- `RiskScoreServiceImpl` — persistence and alerting logic; look for thresholds (>= 75) and notification calls.

Interview Qs:
1) Q: How does fraud auto-evaluation use metrics from another service? Which files show this integration?
   A: `RiskScoreServiceImpl` calls `ClaimsMetricsServiceClient` (Feign) to fetch the SEVERITY KPI for the claim, then evaluates HighCost and UnusualTiming rules. See `fraud-risk-service/src/main/java/.../service/RiskScoreServiceImpl.java` and `client/ClaimsMetricsServiceClient.java`.

2) Q: How are investigators or SIU escalations created from a risk score? Point to the controller.
   A: `InvestigationController.createInvestigation(...)` persists an `Investigation` entity via `InvestigationService` and may call `NotificationServiceClient` to dispatch alerts. See `InvestigationController.java` and `InvestigationService.java`.

Code references:
- `fraud-risk-service/src/main/java/com/claim360/fraudrisk/service/RiskScoreServiceImpl.java`
- `fraud-risk-service/src/main/java/com/claim360/fraudrisk/controller/InvestigationController.java`

---

### Denial Leakage Service (`denial-leakage-service`)

Key files:
- Controllers: `controller/DenialPatternController.java`, `controller/LeakageFlagController.java`.
- Services: `service/DenialPatternServiceImpl.java`, `service/LeakageFlagServiceImpl.java`.
- Validation annotations: `validation/ValidateDenialCode.java`, `validation/ValidateEstimatedLoss.java`.

What to read:
- `AutoAnalyzeRequest` and controller `autoAnalyze` endpoint used by ingestion; see `client/dto/AutoAnalyzeRequest.java` in ingestion service to understand payload shape.

Interview Qs:
1) Q: How does this service validate incoming denial codes and estimated loss? Which classes implement validation?
   A: Custom validation annotations like `@ValidateDenialCode` and `@ValidateEstimatedLoss` implement `ConstraintValidator` classes (`DenialCodeValidator`, `EstimatedLossValidator`) to enforce business rules on DTOs before controller methods run.

Code references:
- `denial-leakage-service/src/main/java/com/claim360/denialleakage/validation/DenialCodeValidator.java`
- `denial-leakage-service/src/main/java/com/claim360/denialleakage/controller/DenialPatternController.java`

---

### Cost Reserve Service (`cost-reserve-service`)

Key files:
- Controllers: `controller/ClaimCostController.java`, `controller/ClaimReserveController.java`, `controller/AgingRecordController.java`.
- Services: `service/ClaimCostServiceImpl.java`, `service/ClaimReserveServiceImpl.java`, `service/AgingRecordServiceImpl.java`.
- Entities: `entity/ClaimCost.java`, `entity/ClaimReserve.java`, `entity/AgingRecord.java`.

What to read:
- `AutoInitializeRequest` DTO and `ClaimReserveServiceImpl.autoInitialize(...)` to see how reserves are set (e.g., 120% of claim amount).

Interview Qs:
1) Q: Where is the reserve percentage applied in code? How would you change it to be configurable?
   A: The reserve computation lives in `ClaimReserveServiceImpl` (look for `reserve = claimAmount * 1.2`). To make it configurable, inject a property from `application.yml` and use that value instead of hardcoded 1.2.

Code references:
- `cost-reserve-service/src/main/java/com/claims/service/ClaimReserveServiceImpl.java`
- `cost-reserve-service/src/main/java/com/claims/controller/ClaimReserveController.java`

---

### Analytics Report Service (`analytics-report-service`)

Key files:
- Controllers: `controller/AnalyticsReportController.java`, `controller/ReportExportController.java`.
- Service: `service/AnalyticsReportServiceImpl.java`, `service/ReportExportService.java`.
- Repository: `repository/AnalyticsReportRepository.java` and entity `AnalyticsReport.java`.

What to read:
- `ReportExportService.exportReport(...)` for server-side export logic and Content-Disposition header handling.
- Dashboard aggregator methods that call multiple service clients in parallel.

Interview Qs:
1) Q: How is a PDF/CSV export implemented server-side? What fallback exists on the frontend?
   A: `ReportExportService` streams a Blob with appropriate `Content-Type` and `Content-Disposition`. The frontend `reportsApi.export()` requests `responseType: blob` and `ReportsPage` falls back to client-side CSV/PDF generation if server export fails (see `frontendwihtoutgragh/src/pages/reports/ReportsPage.tsx`).

Code references:
- `analytics-report-service/src/main/java/com/claims/service/ReportExportService.java`
- `analytics-report-service/src/main/java/com/claims/controller/ReportExportController.java`

---

### Notification Service (`NotificationService`)

Key files:
- Application: `NotificationService/src/main/java/com/demo/NotificationServiceApplication.java`.
- Controllers: `controller/NotificationController.java`, `controller/NotificationStreamController.java` (SSE endpoints).
- Services: `services/NotificationServiceImpl.java`, `services/NotificationStreamService.java`.
- Entities/Repo: `entities/Notification.java`, `User.java`, `repository/NotificationRepository.java`, `UserRepository.java`.

What to read:
- Scheduled jobs in `NotificationServiceImpl` that call other services via Feign clients (see methods like `generateRiskAlerts()` which calls `fraud-risk-service`).
- `UserSyncRequestDTO` and `POST /api/notifications/users/sync` to mirror gateway users.

Interview Qs:
1) Q: How does the NotificationService expose real-time streams to the frontend?
   A: `NotificationStreamController` exposes SSE endpoints that the frontend can subscribe to. `NotificationStreamService` manages emitters; see `NotificationStreamController.java`.

2) Q: Why does NotificationService keep a mirrored users table? Where is the sync endpoint?
   A: For fast recipient lookup and role-based dispatch without querying gateway on each notification. Sync endpoint: `POST /api/notifications/users/sync` implemented in `NotificationController`.

Code references:
- `NotificationService/src/main/java/com/demo/controller/NotificationController.java`
- `NotificationService/src/main/java/com/demo/services/NotificationServiceImpl.java`

---

### Adjuster And Operations (`AdjusterAndOperations`)

Key files:
- Controllers: `controller/SLAViolationController.java`, `controller/AdjusterPerformanceController.java`.
- Services: `service/SLAViolationServiceImpl.java`, `service/AdjusterPerformanceServiceImpl.java`.
- Repositories: `repositories/SLAViolationRepository.java`, `repositories/AdjusterPerformanceRepository.java`.

What to read:
- `AutoGenerateSlaRequest` DTO used by ingestion and SLA generation.
- Note the type mismatch: `SLAViolation.claimId` typed `Long` but ingestion uses business string IDs; see entity and ingestion client DTOs.

Interview Qs:
1) Q: How would you fix the claimId type mismatch between ingestion (string) and SLA violation entity (`Long`)?
   A: Prefer harmonizing business ID types across services (store as String). Update entity, migrations, and repository methods, and add compatibility mapping until migration completes.

Code references:
- `AdjusterAndOperations/src/main/java/com/demo/service/SLAViolationServiceImpl.java`
- `AdjusterAndOperations/src/main/java/com/demo/entities/SLAViolation.java`

---

## How to use these notes

- For each microservice you plan to discuss in an interview, open the referenced files and read the controller → service → repository chain. The sample questions above map directly to the code so you can cite file names and method names in answers.
- If you want, tell me one or two services and I will deep-scan their controllers and produce a per-endpoint Q&A list referencing specific methods and important lines.



