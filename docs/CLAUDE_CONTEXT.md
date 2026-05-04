# ClaimInsight360 — Codebase Context File

**Purpose of this document.** This is a structural reference for the entire
codebase. Paste this into a Claude/ChatGPT conversation and you'll have
enough context to answer "what does line X in file Y do" without the LLM
having to grep around blind. Every section names real files, classes,
methods, and the architectural choices behind them.

The document is organised by **what something is** (project structure) and
**why it's there** (design choice). When you want to ask about a specific
line, find the file in the relevant section first — that gives the LLM the
surrounding context it needs to answer accurately.

---

## 1. Topology — what lives where

```
cali/                                     ← repo root, contains parent pom.xml
├── eureka-server/                        ← service registry        :8761
├── api-gateway/                          ← public ingress + auth   :8086 (WebFlux)
├── data-ingestion-service/               ← feeds + raw claims      :8082
├── claims-metrics-service/               ← KPIs + claim status     :8083
├── analytics-report-service/             ← reports                 :8084
├── denial-leakage-service/               ← denial codes + leakage  :8085
├── AdjusterAndOperations/                ← adjusters + SLA         :8087
├── NotificationService/                  ← notifications hub       :8088
├── cost-reserve-service/                 ← costs/reserves/aging    :8089
├── fraud-risk-service/                   ← scores + indicators     :8090
│
├── frontendwihtoutgragh/                 ← PRIMARY React app (chart-stripped)
├── claiminsight360-frontend/             ← original React app
├── claiminsight360-frontend-v2/          ← second-iteration design (charts kept)
├── claiminsight360-frontend-v3/          ← third-iteration design
│
├── pom.xml                               ← Maven aggregator (10 modules listed)
├── seed_all_databases.sql                ← canonical seed data
├── fix_mojibake.sql                      ← mojibake repair (em-dash → hyphen)
├── INTERVIEW_GUIDE.md                    ← narrative project explanation
├── CLAUDE_CONTEXT.md                     ← this file
└── .idea/runConfigurations/              ← 10 IntelliJ Spring Boot run configs
```

Source-file count: ~574 across `*.java`, `*.tsx`, `*.ts`, `*.yml`, `*.css`.

### Database-per-service layout

| Service | MySQL schema |
|---|---|
| api-gateway | `claiminsight_db` (the shared identity DB; `gateway_users`, `audit_logs`) |
| data-ingestion-service | `claiminsight_db` (shares `claim_raw`, `data_feed`, `claim_status` tables) |
| claims-metrics-service | `claiminsight_db` (shares `claim_kpi`, `claim_status`) |
| denial-leakage-service | `denial_leakage` (`denial_pattern`, `leakage_flag`) |
| fraud-risk-service | `fraud_risk_db` (`risk_score`, `risk_indicator`) |
| cost-reserve-service | `claims_cost_reserve_db` (`claim_cost`, `claim_reserve`, `aging_record`) |
| AdjusterAndOperations | `adjusterperformancedb` (`claim`, `adjuster_performance`, `sla_violation`) |
| analytics-report-service | `claims_analytics_report_db` (`analytics_report`) |
| NotificationService | `notificationsdb` (`notifications`, `users` mock-mirror, `risk_scores`, `sla_violations`, `aging_records`) |

---

## 2. Tech stack at a glance

**Backend**
- Spring Boot 3.5.x (everywhere)
- Spring Cloud Gateway (WebFlux) — only the gateway is reactive
- Spring Cloud LoadBalancer + Netflix Eureka — service discovery
- OpenFeign — synchronous service-to-service calls (in WebMVC services)
- WebClient — async service-to-service calls (only from the gateway, since it's WebFlux)
- Spring Data JPA + Hibernate — ORM
- MySQL 8 — per-service schemas
- Lombok — boilerplate elimination (`@Data`, `@RequiredArgsConstructor`, `@Builder`)
- ModelMapper — DTO ↔ entity mapping
- Spring Security — JWT auth at the gateway
- BCrypt — password hashing
- Spring `@Cacheable` — read-path caching, in-memory by default
- Springdoc OpenAPI — auto-generated `/swagger-ui` per service
- Java 21 (project-wide)

**Frontend (`frontendwihtoutgragh`)**
- React 18 + TypeScript (strict mode)
- Vite 5 — dev server with proxy to gateway
- Ant Design v5 — component library
- Redux Toolkit — auth state slice (only persisted state)
- react-hook-form + zod — auth-page form validation
- axios — HTTP client with interceptors
- lucide-react — icon set
- recharts (kept on AgingPage + CostsPage in `frontendwihtoutgragh`)

---

## 3. Backend service deep-dives

For each service: package layout, the entities it owns, the controllers it exposes, and the interesting cross-service wiring.

### 3.1 eureka-server (port 8761)

**Single class:** `EurekaServerApplication.java` — `@EnableEurekaServer` annotation flips the dial. `application.yml` disables self-registration (a Eureka server doesn't register itself).

This service has no business logic. Start it first; everyone else registers with it.

---

### 3.2 api-gateway (port 8086)

The single most important service. Documented separately in INTERVIEW_GUIDE.md but here's the file map:

```
api-gateway/src/main/java/com/claiminsight/gateway/
├── ApiGatewayApplication.java                — main class, @EnableDiscoveryClient
├── config/
│   └── SecurityConfig.java                   — reactive sec chain, role/path matrix
├── exception/
│   └── GlobalExceptionHandler.java           — reactive @RestControllerAdvice
├── filter/
│   ├── AuditFilter.java                      — async audit row per request
│   └── AuthHeaderForwardFilter.java          — adds X-Auth-* headers downstream
├── identity/                                 ← embedded auth module
│   ├── controller/
│   │   ├── AuthController.java               — /api/auth/login, /register, /logout
│   │   └── AuditController.java              — /api/audit/logs (admin only)
│   ├── dto/
│   │   ├── AuthResponseDTO.java              — { token, userId, username, role }
│   │   ├── LoginRequestDTO.java              — { username, password }
│   │   └── RegisterRequestDTO.java           — { username, email, name, phone, password, role }
│   ├── model/
│   │   ├── AuditLog.java                     — audit_logs table
│   │   ├── Role.java                         — enum: ROLE_CLAIMS_ANALYST, ROLE_ADMIN, etc.
│   │   └── User.java                         — gateway_users table
│   ├── notification/
│   │   ├── NotificationDispatchRequest.java  — outgoing dispatch payload
│   │   └── NotificationEmitter.java          — WebClient → NotificationService
│   ├── repository/
│   │   ├── AuditLogRepository.java
│   │   └── UserRepository.java
│   └── service/
│       ├── AuditService.java                 — @Async log()
│       ├── AuthService.java                  — login/register orchestration
│       └── JwtService.java                   — generate/parse HS256
└── security/
    ├── JwtAuthManager.java                   — validates JWT, builds Authentication
    └── JwtSecurityContextRepository.java     — pulls header → calls JwtAuthManager
```

**Five jobs:** routing, JWT issuance, JWT validation + role gating, audit logging, header forwarding (so downstream services trust the gateway and skip re-validation).

**Key route table:** lives in `application.yml` lines 39-122. Each route is `Path=/api/X/**` → `lb://service-name`. 14 routes, all 8 business services covered.

**Critical gotcha (commented inline):** `SecurityConfig.java:26-29` warns that calling `.authenticationManager()` on `ServerHttpSecurity` creates a stray `AuthenticationWebFilter` that breaks `permitAll()` paths. Don't do it. The auth manager is invoked from inside `JwtSecurityContextRepository` instead.

**JWT signing key:** `application.yml:142` (HS256, 64 hex chars). Should move to env var / vault before prod — flagged in INTERVIEW_GUIDE.md.

---

### 3.3 data-ingestion-service (port 8082)

```
data-ingestion-service/src/main/java/com/claiminsight/ingestion/
├── DataIngestionApplication.java
├── client/
│   ├── NotificationServiceClient.java        — @FeignClient
│   └── dto/NotificationDispatchRequestDTO.java
├── controller/
│   ├── DataFeedController.java               — /api/feeds/*
│   └── IngestionController.java              — /api/ingest/*
├── dto/
│   ├── DataFeedRequestDTO.java
│   ├── DataFeedResponseDTO.java
│   ├── ErrorResponseDTO.java                  — { timestamp, status, error, message }
│   ├── FeedStatusUpdateDTO.java
│   ├── IngestionRequestDTO.java               — { claimId, feedId, payloadJson }
│   └── IngestionResponseDTO.java              — { rawId, claimId, feedId, feedType, payloadJson, ingestedDate }
├── exception/GlobalExceptionHandler.java
├── mapper/
│   ├── ClaimRawMapper.java                    — denormalizes feedId/feedType into response
│   └── DataFeedMapper.java
├── model/
│   ├── ClaimRaw.java                          — claim_raw table
│   ├── DataFeed.java                          — data_feed table, @OneToMany(cascade=ALL, orphanRemoval=true)
│   ├── FeedStatus.java                        — ACTIVE, INACTIVE, FAILED
│   └── FeedType.java                          — CLAIM, POLICY, PAYMENT, RESERVE
├── repository/
│   ├── ClaimRawRepository.java
│   └── DataFeedRepository.java
└── service/
    ├── DataFeedService.java                   — feed CRUD
    └── IngestionService.java                  — ingestClaim with ACTIVE-only gate
```

**The interesting business rule** lives in `IngestionService.ingestClaim()`:
1. Look up the `DataFeed` by id → throw `ResourceNotFoundException` (404) if missing.
2. If `feed.getStatus() != ACTIVE`, **fire a SYSTEM notification** to ADMIN+EXECUTIVE roles (via Feign to NotificationService) AND throw `InvalidFeedStatusException` (400). The notification is fire-and-forget; failures are swallowed.
3. Persist the `ClaimRaw` row.
4. Bump `feed.lastSyncDate = now()`.
5. Cache evict `rawClaims` and `feeds`.

**Why 1:N model:** `DataFeed` is the *source registration* (one per upstream system), `ClaimRaw` is the *individual payload* (N rows per feed). The `feed_id` FK is what lets you ask "which pipe did this claim come through".

**`@OneToMany(cascade=ALL, orphanRemoval=true)`** on `DataFeed.claimRawList` — deleting a feed deletes every claim_raw under it. Silent data-loss risk; flagged in the audit. The FE `Popconfirm` doesn't currently warn about it.

---

### 3.4 claims-metrics-service (port 8083)

```
claims-metrics-service/src/main/java/com/claiminsight/metrics/
├── ClaimsMetricsApplication.java
├── client/                                   — Feign to NotificationService
├── controller/
│   ├── ClaimKpiController.java               — /api/kpis/*
│   └── ClaimStatusController.java            — /api/claim-status/* (added later)
├── dto/
│   ├── ClaimKpiRequestDTO.java
│   ├── ClaimKpiResponseDTO.java
│   ├── ClaimStatusRequestDTO.java
│   ├── ClaimStatusResponseDTO.java
│   └── ErrorResponseDTO.java
├── exception/GlobalExceptionHandler.java
├── model/
│   ├── ClaimKpi.java                         — claim_kpi table
│   ├── ClaimStatus.java                      — claim_status table (ACTIVE/INACTIVE)
│   └── MetricName.java                       — TAT, CYCLE_TIME, SEVERITY, FREQUENCY, LOSS_RATIO
├── repository/
└── service/
    ├── ClaimKpiService.java
    ├── ClaimStatusService.java
    └── KpiCalculationService.java            — derives aggregates from ClaimKpi rows
```

**Five metric names** map to the `metricName` column. The FE `claimsApi.getByMetric('TAT')` filters at the BE.

**ClaimStatus** is per-claim Active/Inactive. The frontend `ClaimsPage` persists it via this endpoint, with localStorage as an optimistic cache.

---

### 3.5 cost-reserve-service (port 8089)

```
cost-reserve-service/src/main/java/com/claims/
├── CostReserveServiceApplication.java
├── client/                                   — Feign to NotificationService
├── controller/
│   ├── ClaimCostController.java              — /api/costs/*
│   ├── ClaimReserveController.java           — /api/reserves/*
│   └── AgingRecordController.java            — /api/aging/*
├── dto/                                      — request/response per entity
├── exception/GlobalExceptionHandler.java
├── model/
│   ├── ClaimCost.java                        — costType: MEDICAL, LEGAL, SETTLEMENT, ADMIN
│   ├── ClaimReserve.java                     — single reserve amount per claim
│   └── AgingRecord.java                      — bucket: 0-30, 31-60, 61-90, 91-120, 120+
├── repository/
└── service/
    ├── ClaimCostServiceImpl.java
    ├── ClaimReserveServiceImpl.java
    └── AgingRecordServiceImpl.java
```

**Why a separate aging table** (vs deriving from claim age): historical accuracy. Aging records preserve the snapshot at reporting date. Computing aging on demand from `ingestedDate` works for current state but loses the snapshot at month-end.

---

### 3.6 AdjusterAndOperations (port 8087)

The naming is the odd one out (others are kebab-case; this one is PascalCase). Under `com.demo.*` package — placeholder package never renamed.

```
AdjusterAndOperations/src/main/java/com/demo/
├── AdjusterAndOperationsApplication.java
├── client/NotificationServiceClient.java
├── configuration/ModelMapperConfiguration.java
├── controller/
│   ├── AdjusterPerformanceController.java    — /api/adjusters/*
│   ├── ClaimController.java                  — /api/claims/* (legacy; not used by FE)
│   └── SLAViolationController.java           — /api/sla-violations/*
├── dto/
├── entities/
│   ├── AdjusterPerformance.java              — adjuster_performance table, with quarterly periods
│   ├── Claim.java                            — DELETED in recent commit
│   └── SLAViolation.java                     — sla_violation table
├── exception/GlobalExceptionHandler.java     — emits standard {timestamp,status,error,message}
├── repositories/
└── service/                                  — *ServiceImpl
```

**`AdjusterPerformance`** has quarterly columns (Q1-2026, Q4-2025, etc.) — it's a historical performance snapshot, not a real-time count.

**SLA logic:** When an adjuster's `slaBreachedCount` crosses a threshold, `AdjusterPerformanceServiceImpl` fires a notification to `ROLE_CLAIMS_MANAGER`.

---

### 3.7 fraud-risk-service (port 8090)

```
fraud-risk-service/src/main/java/com/claim360/fraudrisk/
├── FraudRiskServiceApplication.java
├── client/                                   — Feign + DTOs
├── controller/
│   ├── RiskScoreController.java              — /api/risk-scores/*
│   └── RiskIndicatorController.java          — /api/risk-indicators/*
├── dto/
├── exception/
│   ├── GlobalExceptionHandler.java
│   ├── ResourceNotFoundException.java
│   └── ErrorResponse.java
├── model/
│   ├── RiskScore.java                        — risk_score table, scoreValue 0-100
│   ├── RiskIndicator.java                    — risk_indicator table
│   ├── IndicatorType.java                    — REPEAT_PATTERN, UNUSUAL_TIMING, HIGH_COST, …
│   └── Severity.java                         — LOW, MEDIUM, HIGH
├── repository/
└── service/
```

**Score and indicators are split by design** — the architecture mirrors a feature/inference ML pattern. Scores are the numeric inference (0-100); indicators are the categorical features that explain the score. Today both are rule-based.

**Auto-alert:** `RiskScoreServiceImpl` fires a SYSTEM notification when `scoreValue >= 75`.

**pom note:** historically had a self-contradiction (`<java.version>21</java.version>` but `maven-compiler-plugin` was pinned to 24). Fixed to use `${java.version}` everywhere.

---

### 3.8 denial-leakage-service (port 8085)

```
denial-leakage-service/src/main/java/com/claim360/denialleakage/
├── DenialLeakageServiceApplication.java
├── client/
├── controller/
│   ├── DenialPatternController.java          — /api/denial-patterns/*
│   └── LeakageFlagController.java            — /api/leakage-flags/*
├── dto/
├── exception/
├── model/
│   ├── DenialPattern.java                    — denial_pattern table; reason text matches mojibake fix
│   ├── LeakageFlag.java                      — leakage_flag table; estimatedLoss in $
│   └── LeakageType.java                      — UNDERPAYMENT, OVERPAYMENT, MISSED_RECOVERY, …
├── repository/
└── service/
```

**Mojibake history:** The `denial_pattern.reason` column historically contained `ÔÇö` characters from a previous seed where MySQL imported em-dash UTF-8 bytes (`0xE2 0x80 0x94`) with a DOS code page (cp437/cp850). Fixed via `fix_mojibake.sql` — now uses plain hyphens.

---

### 3.9 analytics-report-service (port 8084)

```
analytics-report-service/src/main/java/com/claims/
├── AnalyticsReportServiceApplication.java
├── controller/
│   ├── AnalyticsReportController.java        — /api/reports/*
│   └── ReportExportController.java           — /api/reports/export (CSV download)
├── dto/
├── exception/
├── model/
│   └── AnalyticsReport.java                  — analytics_report table; reportData is JSON snapshot
├── repository/
└── service/
    ├── AnalyticsReportServiceImpl.java
    └── ReportExportService.java
```

**Export strategy:** No server-side PDF rendering. CSV downloads happen client-side via Blob in the FE. Keeps the BE stateless and avoids dragging in a heavy PDF lib.

---

### 3.10 NotificationService (port 8088)

The hub everyone else calls. Only service using a different envelope shape (`{success, message, data}` instead of `{timestamp, status, error, message}`).

```
NotificationService/src/main/java/com/demo/
├── NotificationServiceApplication.java
├── aspect/                                   — request/method-timing aspects
├── config/
├── controller/
│   ├── NotificationController.java           — interface; /api/notifications/*
│   └── NotificationControllerImpl.java
├── dto/
│   ├── ApiResponse.java                      — {success, message, data} envelope
│   ├── NotificationDispatchRequestDTO.java   — {targetUserIds, targetRoles, title, message, category, referenceId}
│   ├── NotificationRequestDTO.java
│   ├── NotificationResponseDTO.java
│   ├── NotificationStatusUpdateDTO.java
│   └── UserSyncRequestDTO.java               — {userId, name, email, role, isActive}
├── entities/
│   ├── Notification.java                     — notifications table
│   ├── User.java                             — mock users mirror; populated by gateway sync
│   ├── AgingRecord.java, RiskScore.java, SlaViolation.java  — read-only views for scheduled alerts
├── enums/
│   ├── NotificationCategory.java             — RISK, DENIAL, COST, PERFORMANCE, AGING, SYSTEM
│   ├── NotificationStatus.java               — UNREAD, READ, DISMISSED, ACTIONED
│   └── UserRole.java                         — ANALYST, MANAGER, FRAUD, ACTUARY, EXECUTIVE, ADMIN
├── exception/GlobalExceptionHandler.java     — wraps in ApiResponse.failure(...)
├── repository/
│   ├── NotificationRepository.java
│   └── UserRepository.java                   — added during the user-sync fix
└── services/
    ├── NotificationService.java              — interface
    └── NotificationServiceImpl.java          — dispatchNotification, syncUser, scheduled alert generators
```

**The 11 endpoints:**
1. `POST /api/notifications` — manual create
2. `POST /api/notifications/dispatch` — cross-service fan-out (used by every other service)
3. `POST /api/notifications/users/sync` — added recently to fix user-mirror gap
4. `GET /api/notifications/user/{userId}` — list per user
5. `GET /api/notifications/user/{userId}/status/{status}`
6. `GET /api/notifications/user/{userId}/category/{category}`
7. `GET /api/notifications/{notificationId}`
8. `PATCH /api/notifications/{notificationId}/status`
9. `PATCH /api/notifications/user/{userId}/mark-all-read`
10. `GET /api/notifications/unread-count/{userId}`
11. `DELETE /api/notifications/{notificationId}`

**Role-based dispatch (in `NotificationServiceImpl.dispatchNotification`)** queries the `users` table:
```sql
SELECT u.userId FROM User u WHERE u.role IN :roles AND u.isActive = true
```
This is why **user-sync from the gateway matters**. New users registered via the gateway must be mirrored into `notificationsdb.users` or role-targeted notifications never reach them.

**Scheduled alerts:** `@Scheduled` methods generate alerts from `RiskScore`, `AgingRecord`, `SlaViolation` views — alerts for stuck claims, high-risk scores, aging breaches. See `generateRiskAlerts()`, `generateAgingAlerts()`, `generatePerformanceAlerts()`.

---

## 4. Cross-cutting flows

### 4.1 Authentication flow (login)

```
1. FE LoginPage.tsx → axios POST /api/auth/login { username, password }
2. Vite proxy /api → http://localhost:8086/api/auth/login
3. Gateway SecurityConfig.permitAll() lets it through
4. AuthController.login(LoginRequestDTO)
5. AuthService.authenticate:
     - userRepository.findByUsernameOrEmail(...) → throw BadCredentialsException if not found
     - if (!user.isEnabled()) → audit LOGIN_FAILED, throw BadCredentialsException
     - passwordEncoder.matches(raw, hashed) → audit LOGIN_FAILED if false
     - audit LOGIN_SUCCESS
     - notificationEmitter.emitUserSync(...)   ← back-fill, idempotent
     - jwtService.generateToken(username, role, userId)
6. Response: AuthResponseDTO { token, userId, username, role }
7. FE authSlice.setCredentials(response) → Redux + localStorage
8. FE navigate('/dashboard')
```

The JWT is HS256-signed with the secret in `application.yml:142`. Claims: `sub`, `role`, `userId`, `iat`, `exp`.

### 4.2 Authorization on every protected request

```
1. Browser sends GET /api/kpis with Authorization: Bearer <jwt>
2. Gateway CORS filter validates origin
3. JwtSecurityContextRepository pulls the header
4. JwtAuthManager validates signature, expiry, parses role
5. SecurityConfig role rules: /api/kpis/** requires ROLE_CLAIMS_ANALYST | ROLE_CLAIMS_MANAGER | ROLE_ACTUARY | ROLE_OPERATIONS_EXEC | ROLE_ADMIN
6. AuthHeaderForwardFilter mutates request: adds X-Auth-Username, X-Auth-Role
7. AuditFilter registers doFinally → async write to audit_logs
8. Spring Cloud Gateway: Path=/api/kpis/** → lb://claims-metrics-service via Eureka
9. claims-metrics-service ClaimKpiController handles, returns ResponseEntity<List<ClaimKpiResponseDTO>>
10. AuditFilter doFinally fires → audit row written
```

Downstream services **do not** re-validate the JWT. They trust the gateway. The `X-Auth-Username` header is what they read for the caller identity.

### 4.3 Cross-service notification dispatch

Any service that needs to alert someone goes through NotificationService:

```
1. Some service does business logic, decides to alert (e.g., feed not ACTIVE, score >= 75, SLA breach)
2. Service builds NotificationDispatchRequestDTO {
     targetRoles: Set<String>,    // e.g., ["ADMIN", "EXECUTIVE"]
     targetUserIds: Set<Long>,    // optional explicit recipients
     title: String,
     message: String,
     category: String,            // "RISK" | "DENIAL" | "COST" | "PERFORMANCE" | "AGING" | "SYSTEM"
     referenceId: String          // usually the claimId
   }
3. Service calls notificationServiceClient.dispatchNotification(req) — Feign
4. Feign resolves NotificationService via Eureka, makes HTTP POST /api/notifications/dispatch
5. NotificationServiceImpl.dispatchNotification:
     - resolves recipients: explicit userIds + role fan-out (SELECT users WHERE role IN ...)
     - iterates, persists one Notification per recipient
6. Recipients see the new alert next time their bell polls /unread-count/{userId} (every 30s)
```

**Failure handling:** all dispatch calls are wrapped in try/catch. NotificationService outages don't block business operations.

The gateway uses **WebClient** for this (since it's reactive); every other service uses **Feign**. The DTO shape is identical.

### 4.4 Audit logging

Three writers:
1. **AuthService** — `LOGIN_SUCCESS`, `LOGIN_FAILED` (with reason), `REGISTER` (with role) — rich, with userId
2. **AuditFilter** — every other request (not /api/auth/*) — coarse, with method/status/IP/userAgent

(Pre-fix there was duplication; now `/api/auth/*` is skipped by the filter.)

3. **AuditController** is read-only. Powers the admin **Audit Logs** page.

`AuditService.log(...)` is `@Async`, never blocks the request thread.

### 4.5 Error envelope normalization

Backend services emit two different shapes:

| Service | Shape |
|---|---|
| 8 services (gateway, claims-metrics, fraud-risk, denial-leakage, cost-reserve, data-ingestion, analytics-report, AdjusterAndOperations) | `{ timestamp, status, error, message }` |
| NotificationService alone | `{ success, message, data }` |

The frontend `axiosInstance.ts:getApiErrorMessage()` normalizes both into a `error.userMessage` property. Every page's catch block reads `(err as any).userMessage`. This is what allows backend validation errors and business-rule failures to surface accurately in the UI.

### 4.6 User-sync (gateway → NotificationService)

Recently fixed (the endpoint didn't exist before):

```
Gateway AuthService.register / .login
   → NotificationEmitter.emitUserSync(userId, name, email, role)
   → WebClient.post("/api/notifications/users/sync")
NotificationService NotificationControllerImpl.syncUser
   → NotificationServiceImpl.syncUser
   → UserRepository.upsert(userId, name, email, role, isActive)
     -- INSERT ... ON DUPLICATE KEY UPDATE (idempotent)
```

This is what makes role-based notification fan-out reach **real registered users** instead of only the seeded mock users (1-10).

---

## 5. Frontend deep-dive (`frontendwihtoutgragh`)

The primary frontend. The other variants (`-v2`, `-v3`, `-beginner`, `claiminsight360-frontend`) are earlier iterations.

### 5.1 Source layout

```
frontendwihtoutgragh/src/
├── main.tsx                                  — ReactDOM.createRoot + <Provider store>
├── App.tsx                                   — AntD <ConfigProvider> with isDark theme switch
├── vite.config.ts                            — proxy /api → :8086
├── api/                                      ← one file per backend service
│   ├── axiosInstance.ts                      ← interceptors: JWT inject, error normalization
│   ├── authApi.ts
│   ├── claimsApi.ts
│   ├── adjustersApi.ts
│   ├── auditApi.ts
│   ├── dataIngestionApi.ts                   — feedsApi + ingestApi
│   ├── financialApi.ts                       — costsApi + reservesApi + agingApi
│   ├── fraudRiskApi.ts                       — riskScoresApi + riskIndicatorsApi
│   ├── denialLeakageApi.ts                   — denialPatternsApi + leakageFlagsApi
│   ├── notificationsApi.ts                   — uses unwrap() for {success,message,data} envelope
│   └── reportsApi.ts
├── components/
│   ├── charts/ChartCard.tsx                  — used only by Aging + Costs pages
│   ├── common/
│   │   ├── Navbar.tsx
│   │   ├── Sidebar.tsx
│   │   ├── NotificationBell.tsx              — 30s polling, popover preview
│   │   └── ThemeToggle.tsx                   — sun/moon switch
│   └── ui/                                   ← design-system primitives
│       ├── PageHeader.tsx
│       ├── KpiCard.tsx
│       ├── DataCard.tsx
│       ├── Badge.tsx                         — tones: green/amber/red/blue/purple/teal/neutral
│       ├── Chip.tsx                          — filter chip with dropdown chevron
│       ├── StatusDot.tsx
│       ├── TintedAvatar.tsx
│       ├── EmptyState.tsx
│       ├── ErrorState.tsx                    — paired sibling of EmptyState
│       ├── GhostButton.tsx
│       ├── DarkButton.tsx                    — uses var(--ci-text-primary) → adapts to dark mode
│       └── index.ts                          — barrel + CHART palette
├── contexts/ThemeContext.tsx                 — theme provider, sets data-theme attr on <html>
├── hooks/
│   ├── useAuth.ts                            — wraps Redux selectors
│   └── useNotificationPolling.ts             — interval cleanup on unmount
├── layouts/AppLayout.tsx                     — Navbar + Sidebar + <Outlet>
├── pages/
│   ├── auth/
│   │   ├── LoginPage.tsx                     — RHF + zod
│   │   ├── RegisterPage.tsx
│   │   └── auth.css                          — shared, uses var(--ci-*)
│   ├── dashboard/Dashboard.tsx               — 5 parallel allSettled fetches
│   ├── claims/ClaimsPage.tsx
│   ├── ingestion/{FeedsPage,RawClaimsPage}.tsx
│   ├── financial/{CostsPage,ReservesPage,AgingPage}.tsx
│   ├── adjusters/{AdjustersPage,SlaViolationsPage}.tsx
│   ├── risk/{FraudRiskPage,DenialLeakagePage}.tsx
│   ├── reports/ReportsPage.tsx               — Dropdown menu with CSV export
│   ├── notifications/NotificationsPage.tsx
│   ├── profile/ProfilePage.tsx
│   └── admin/{AuditLogsPage,KpiDefinitionsPage,UsersRolesPage}.tsx
├── router/
│   ├── AppRouter.tsx                         — lazy routes + Suspense
│   ├── ProtectedRoute.tsx                    — requires auth
│   └── RoleRoute.tsx                         — requires specific roles
├── store/
│   ├── index.ts                              — configureStore
│   └── slices/authSlice.ts                   — selectIsAuthenticated, selectIsAdmin, selectUserId, selectUserRole
├── styles/global.css                         — design tokens (CSS variables) for light + dark
├── types/auth.types.ts
└── utils/
    ├── roles.ts                              — ROLE_LABELS map (raw enum → friendly label)
    └── tokenUtils.ts                         — JWT decode helpers
```

### 5.2 Page-level pattern

Every data page follows the same shape:

```tsx
import { useEffect, useReducer, useCallback } from 'react';
import { App as AntApp } from 'antd';

interface State { items: T[]; loading: bool; error: string|null; modalOpen: bool; submitting: bool; ...filters }
type Action = | {type:'START'} | {type:'SUCCESS', payload} | {type:'ERROR', payload} | ...

function reducer(s: State, a: Action): State { switch (a.type) { ... } }

export default function MyPage() {
  const [state, dispatch] = useReducer(reducer, INITIAL);
  const [form] = Form.useForm();
  const { message } = AntApp.useApp();

  const load = useCallback(async () => {
    dispatch({ type: 'START' });
    try {
      const data = await someApi.getAll();
      dispatch({ type: 'SUCCESS', payload: data });
    } catch (err) {
      const msg = (err as { userMessage?: string }).userMessage ?? 'fallback';
      dispatch({ type: 'ERROR', payload: msg });
    }
  }, []);

  useEffect(() => { load(); }, [load]);

  return (
    <div>
      <PageHeader title="..." actions={<>...</>} />
      {state.error && <Alert type="error" message={state.error} />}
      <DataCard>
        {state.loading ? <Spin /> : state.items.length === 0 ? <EmptyState /> : <Table ... />}
      </DataCard>
      <Modal>...form...</Modal>
    </div>
  );
}
```

### 5.3 axios interceptor pattern

```ts
// src/api/axiosInstance.ts
axiosInstance.interceptors.request.use((config) => {
  if (!config.url?.startsWith('/auth')) {
    const token = store.getState().auth.token;
    if (token) config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

export function getApiErrorMessage(err): string {
  // Returns the right message regardless of envelope shape
}

axiosInstance.interceptors.response.use(
  r => r,
  (err) => {
    if (err.response?.status === 401) {
      store.dispatch(clearCredentials());
      if (!window.location.pathname.startsWith('/login')) window.location.href = '/login';
    }
    err.userMessage = getApiErrorMessage(err);
    return Promise.reject(err);
  }
);
```

### 5.4 Theme system

```css
/* global.css — partial */
:root, :root[data-theme='light'] {
  --ci-bg-app:        #F1EFE8;
  --ci-bg-surface:    #ffffff;
  --ci-text-primary:  #2C2C2A;
  --ci-primary:       #185FA5;
  --ci-success-bg:    #EAF3DE;  --ci-success-text: #27500A;
  --ci-warning-bg:    #FAEEDA;  --ci-warning-text: #633806;
  --ci-danger-bg:     #FCEBEB;  --ci-danger-text:  #791F1F;
  /* + decorative tones: purple, teal, neutral */
}

:root[data-theme='dark'] {
  --ci-bg-app:        #1A1A18;
  --ci-bg-surface:    #242421;
  --ci-text-primary:  #F1EFE8;
  --ci-primary:       #378ADD;
  /* dark variants for every semantic var */
}
```

`ThemeContext` writes `data-theme="dark"` to `<html>` and `colorScheme: 'dark'` to inherit native form defaults. AntD's `theme.darkAlgorithm` is hooked in `App.tsx` via the `isDark` boolean.

**Pattern:** any color in inline `style={{}}` should use `var(--ci-...)`, never a literal hex. Hardcoded hexes don't switch on theme toggle.

### 5.5 Routes

`AppRouter.tsx` lazy-loads every page via `React.lazy() + Suspense`. Routes are flat; role-gating uses `<RoleRoute />` which checks the JWT's role claim against `allowedRoles`.

---

## 6. Critical files — the 25 where 80% of interesting logic lives

| File | Why it matters |
|---|---|
| `api-gateway/.../config/SecurityConfig.java` | The role/path matrix. If a user gets "403 forbidden", look here first |
| `api-gateway/.../filter/AuthHeaderForwardFilter.java` | How downstream services know who's calling |
| `api-gateway/.../filter/AuditFilter.java` | Where audit logs are written for non-auth requests |
| `api-gateway/.../service/AuthService.java` | Login/register orchestration; audit + notification side-effects |
| `api-gateway/.../service/JwtService.java` | JWT generation and parsing |
| `api-gateway/.../identity/notification/NotificationEmitter.java` | The only WebClient outbound; user-sync, register-announce, disabled-login-alert |
| `api-gateway/src/main/resources/application.yml` | All routes, JWT secret, CORS config |
| `data-ingestion-service/.../service/IngestionService.java` | The ACTIVE-only ingest gate + reject-notification |
| `NotificationService/.../services/NotificationServiceImpl.java` | dispatchNotification (role fan-out via SQL), syncUser, scheduled alert generators |
| `NotificationService/.../controller/NotificationController.java` | The 11-endpoint interface |
| `NotificationService/.../dto/ApiResponse.java` | The odd-one-out envelope shape |
| `NotificationService/.../repository/UserRepository.java` | Native upsert for user-sync |
| `frontendwihtoutgragh/src/api/axiosInstance.ts` | JWT injection + error normalization |
| `frontendwihtoutgragh/src/api/notificationsApi.ts` | The unwrap() helper for the {success,message,data} envelope |
| `frontendwihtoutgragh/src/store/slices/authSlice.ts` | Auth state, selectors, persistence to localStorage |
| `frontendwihtoutgragh/src/router/AppRouter.tsx` | Route → page mapping; Suspense wiring |
| `frontendwihtoutgragh/src/router/RoleRoute.tsx` | Role-gated route guard |
| `frontendwihtoutgragh/src/styles/global.css` | All design tokens (`--ci-*` CSS variables, light + dark) |
| `frontendwihtoutgragh/src/contexts/ThemeContext.tsx` | Theme switch (writes `data-theme` attr) |
| `frontendwihtoutgragh/src/pages/dashboard/Dashboard.tsx` | 5 parallel `allSettled` fetches; time-window filter |
| `frontendwihtoutgragh/src/pages/ingestion/RawClaimsPage.tsx` | Ingest modal; submits {claimId, feedId, payloadJson} |
| `frontendwihtoutgragh/src/pages/notifications/NotificationsPage.tsx` | Polling; client-side filter combine |
| `frontendwihtoutgragh/src/components/common/NotificationBell.tsx` | 30s poll; unread badge; popover preview |
| `pom.xml` (root) | Maven aggregator listing all 10 modules |
| `seed_all_databases.sql` | Canonical seed for all 7 business DBs |

---

## 7. Common patterns and idioms

| Pattern | Where you see it | Notes |
|---|---|---|
| `useReducer` per page with discriminated `Action` union | Every data page | Heavy for simple pages; could be `useState` × 3 |
| `Promise.allSettled` for parallel fetches | `Dashboard.tsx` | Graceful degradation: 1 failed call doesn't blank the page |
| `@Cacheable` on read service methods, `@CacheEvict` on writes | Every service | In-memory; scoped per-method |
| Feign client + DTO copy in every service | `*/client/dto/NotificationDispatchRequestDTO.java` | DTO duplication is on purpose — services don't share a module |
| `@RequiredArgsConstructor` (Lombok) for constructor injection | Every service class | Lets fields be `private final` |
| `@Builder` on DTOs + entities | Most DTOs/entities | For test fixture clarity |
| ModelMapper for entity↔DTO | `*/mapper/*Mapper.java` | Avoids hand-written copies |
| `@Async` for fire-and-forget logging/notifications | `AuditService`, `NotificationEmitter` | Never block the request |
| BCrypt for passwords | `AuthService` via `PasswordEncoder` bean | 10 rounds (default) |
| HS256 JWT | `JwtService` | Symmetric key in `application.yml` |
| Eureka `lb://` URIs | Gateway routes, all Feign clients | Service name only, no port |

---

## 8. Common gotchas (and where they bit us)

1. **Mojibake in `denial_pattern.reason`** — UTF-8 em-dash bytes interpreted as DOS code page. Fixed via `fix_mojibake.sql`. (See Section 3.8.)

2. **Audit log duplicates** — `AuthService` and `AuditFilter` both wrote rows for `/api/auth/*`. Fix: skip auth paths in `AuditFilter`. (See Section 4.4.)

3. **User-sync gap** — Gateway POSTed to `/api/notifications/users/sync`, endpoint didn't exist. Fixed by adding `UserRepository`, `syncUser` service method, and controller mapping. (See Section 4.6.)

4. **Two error envelope shapes** — 8 services use `{timestamp,status,error,message}`, NotificationService uses `{success,message,data}`. FE normalizes via `getApiErrorMessage`. (See Section 4.5.)

5. **`SecurityConfig.authenticationManager()` trap** — calling it on `ServerHttpSecurity` breaks `permitAll()` paths. Don't. Comment in the file warns about this. (See Section 3.2.)

6. **Hardcoded colors not switching on dark mode** — fix is to use `var(--ci-*)` instead of literal hex in inline styles.

7. **fraud-risk-service pom self-contradiction** — `<java.version>21</java.version>` but `maven-compiler-plugin` was pinned to `<release>24</release>`. Fixed to use `${java.version}`.

8. **IntelliJ run config phantom modules** — module names like `fraud-risk-service (1)` in `.idea/runConfigurations/*.xml` mean IntelliJ imported the module twice. Fix: Project Structure → Modules → remove the phantom; or delete `.idea/modules.xml` and re-import from parent pom.

9. **Vite proxy keeps `/api` prefix** — `vite.config.ts` doesn't strip it; gateway expects the full `/api/...` path. Documented inline.

10. **JWT 401 redirect loop** — fixed by checking `pathname.startsWith('/login')` before navigating.

11. **`ON DUPLICATE KEY UPDATE` ignores the entity's `IDENTITY` strategy** — that's deliberate in `UserRepository.upsert()`. We want the gateway-supplied userId, not an auto-generated one.

12. **AntD `App.useApp()` vs static `message`** — the hook gives theme-aware messages; the static API is global. Pages with `<App>` provider in scope should use the hook.

---

## 9. Glossary — terms specific to this codebase

| Term | Meaning |
|---|---|
| **CHART** / **CHART_PALETTE** | The 6-color palette in `components/ui/index.ts` (blue, teal, amber, red, purple, coral) |
| **DataCard** | The white-card container around every page section, with title + subtitle slots |
| **KpiCard** | The 4-tile stat block on every dashboard-style page; supports `delta` and `tone` |
| **Chip** | A filter pill with optional chevron (used to mean "click for dropdown") |
| **EmptyState** / **ErrorState** | Paired components for "no data" and "fetch failed" — same dimensions, different tones |
| **DarkButton** | A high-contrast inverted button (page-foreground color as bg). Used for primary "Add" CTAs |
| **GhostButton** | A white-bg, 1px-bordered secondary button |
| **TintedAvatar** | A circular avatar with one of 6 brand-color pairs, picked deterministically by name |
| **userMessage** | A string property the axios interceptor attaches to every error, normalized across envelope shapes |
| **CHART_PALETTE** | Array form of the 6 chart colors |
| **CI360** / **ci-** prefix | The design system token namespace; all CSS variables are `--ci-*` |
| **`ROLE_*` enums** | Spring Security authority names (`ROLE_CLAIMS_ANALYST`, `ROLE_ADMIN`, etc.) |
| **`UserRole`** in NotificationService | Different enum (`ANALYST`, `MANAGER`, etc.) — the `NotificationEmitter.ROLE_MAP` translates between them |
| **`MetricName`** | The 5-value enum for ClaimKpi: TAT, CYCLE_TIME, SEVERITY, FREQUENCY, LOSS_RATIO |
| **`FeedType`** | CLAIM, POLICY, PAYMENT, RESERVE — the 4 categories of upstream data feed |
| **`FeedStatus`** | ACTIVE, INACTIVE, FAILED — only ACTIVE feeds accept new ingestion |
| **`NotificationCategory`** | RISK, DENIAL, COST, PERFORMANCE, AGING, SYSTEM — the 6 alert categories |
| **`NotificationStatus`** | UNREAD, READ, DISMISSED, ACTIONED |
| **`IndicatorType`** (fraud) | REPEAT_PATTERN, UNUSUAL_TIMING, HIGH_COST, LATE_FILING, INCOMPLETE_DOCS, ... |
| **`Severity`** | LOW, MEDIUM, HIGH (used by both fraud indicators and SLA violations) |
| **`LeakageType`** | UNDERPAYMENT, OVERPAYMENT, MISSED_RECOVERY, UNNECESSARY_PAYMENT |
| **`X-Auth-Username` / `X-Auth-Role`** | Gateway-injected headers; downstream services trust them as caller identity |
| **`lb://`** | Spring Cloud LoadBalancer URI prefix; means "look up via Eureka" |
| **`@CacheEvict("rawClaims", allEntries=true)`** | Drops every cached entry under that name on this method's success — used after writes |
| **`@PrePersist`** | Hibernate callback: sets `createdDate` / `ingestedDate` to `now()` before INSERT |
| **`@OneToMany(cascade=ALL, orphanRemoval=true)`** | Deleting the parent deletes every child. Used on `DataFeed.claimRawList`. |
| **`useApp()`** | AntD hook returning theme-aware `message`, `notification`, `Modal` instances. Must be inside `<App>` provider |
| **`unwrap<T>(response)`** | Helper in `notificationsApi.ts` that strips `{success, message, data}` to return just the `data` payload |
| **`getApiErrorMessage(err)`** | The error-shape normalizer in `axiosInstance.ts` |

---

## 10. How to ask "what does line X do" with this file as context

Best results when you give the LLM:
1. The file path (so it can locate context in this document via search)
2. The specific lines or code block
3. Any error message or behavior you're seeing

Example query:
> Looking at `api-gateway/src/main/java/com/claiminsight/gateway/identity/service/AuthService.java`
> line 109: `auditService.log(user.getUsername(), user.getId(), "LOGIN_SUCCESS", "/api/auth/login", null);`
> What does this do, and why is the metadata argument `null` here when other audit calls pass JSON?

The LLM, with this context document, can answer:
- `auditService.log(...)` writes a row to `audit_logs` (Section 3.2 + 4.4)
- It's `@Async` (Section 3.2 + 7) — fire-and-forget
- The 5 arguments are `username, userId, action, resource, metadata`
- The `null` metadata is because `LOGIN_SUCCESS` carries no error reason; failure cases pass `{"reason":"..."}` to capture WHY a login failed (Section 8 #2 — pre-fix this was duplicated by the filter)
- The audit row will be visible in the admin Audit Logs page via `/api/audit/logs`

---

## 11. Quick start (sanity check)

If you want to run the platform end-to-end:

1. **MySQL up** with all 7 schemas (`seed_all_databases.sql` creates them).
2. **In IntelliJ**, open the root `pom.xml` as a project. Reload Maven Projects.
3. Run order in IntelliJ's run-config dropdown:
   - `1 - Eureka Server` (port 8761)
   - `2 - API Gateway` (port 8086)
   - The other 8 in any order
4. Frontend: `cd frontendwihtoutgragh && npm run dev` → http://localhost:3000
5. Sample logins (from seed):
   - `admin_alice` / `password123` → `ROLE_ADMIN`
   - `analyst_bob` / `password123` → `ROLE_CLAIMS_ANALYST`
   - `fraud_carol` / `password123` → `ROLE_FRAUD_ANALYST`

Health checks:
- `http://localhost:8761` — Eureka dashboard, should list all 9 services
- `http://localhost:8086/actuator/health` — gateway up
- `http://localhost:<port>/swagger-ui.html` — per-service API docs

---

**End of context file.** This is enough background for any LLM (or you) to answer line-level questions about any file in this codebase. When asking, name the file path explicitly so the LLM can map it to the right section above.
