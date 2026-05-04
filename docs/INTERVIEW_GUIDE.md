# ClaimInsight360 — Interview Guide

A walking tour of every feature, end-to-end, with the architectural decisions
you should be ready to defend in an interview.

---

## 1. One-liner pitch

> **ClaimInsight360 is an insurance-claims analytics platform.** It ingests
> raw claim payloads from upstream systems, computes KPIs (turnaround time,
> loss ratio, SLA compliance), surfaces fraud and denial-leakage risk, and
> alerts the right people in real time — all behind role-based access.

Built as a Spring Boot microservices backend (9 services) + React/TypeScript
frontend, with Eureka service discovery, JWT auth, and per-service MySQL.

---

## 2. Architecture at 30,000 ft

```
                                  ┌──────────────────────────┐
   Browser ─── React/Vite/AntD ───▶  Spring Cloud Gateway     │  :8086
                                  │  (WebFlux, JWT issuer)    │
                                  └────────────┬─────────────┘
                                               │ load-balanced via Eureka
                            ┌──────────────────┼──────────────────┐
                            ▼                  ▼                  ▼
                   ┌────────────────┐ ┌────────────────┐ ┌────────────────┐
                   │ claims-metrics │ │ fraud-risk     │ │ denial-leakage │
                   └───────┬────────┘ └───────┬────────┘ └───────┬────────┘
                           │                  │                  │
                   ┌───────▼────────┐ ┌───────▼────────┐ ┌───────▼────────┐
                   │ cost-reserve   │ │ data-ingestion │ │ AdjusterAndOps │
                   └───────┬────────┘ └───────┬────────┘ └───────┬────────┘
                           │                  │                  │
                           └──────────┬───────┴──────────┬───────┘
                                      │                  │
                                      ▼                  ▼
                           ┌────────────────────┐ ┌────────────────────┐
                           │ analytics-report   │ │ NotificationService│
                           └────────────────────┘ └─────────▲──────────┘
                                                            │ Feign
                                                  (every service ──┘ )
                                                            │
                                                  ┌─────────▼─────────┐
                                                  │ Eureka Discovery  │  :8761
                                                  └───────────────────┘

   Per-service MySQL schemas:
     claims_metrics · fraud_risk · denial_leakage · cost_reserve
     data_ingestion · adjuster_ops · analytics_report · notifications
```

### Why this shape
- **API Gateway is the single ingress.** All authentication happens here. Services trust the gateway-issued JWT and never re-validate against a user database.
- **Each service owns its database.** No shared schema. Cross-service relationships are by ID only (no foreign keys spanning services).
- **Eureka + Spring Cloud LoadBalancer** for service discovery. Services register; the gateway and Feign clients resolve by service name (`lb://claims-metrics-service`).
- **Two communication paradigms.** The gateway is reactive (WebFlux) → uses `WebClient`. The 8 business services are servlet-based (WebMVC) → use `OpenFeign`.
- **NotificationService is the alert hub.** Any service that needs to alert a user (fraud detected, SLA breached, ingestion rejected) calls it via Feign. Fan-out is role-based (`targetRoles: ["ADMIN"]`).

---

## 3. Tech stack

| Layer | Technology |
|---|---|
| Frontend | React 18, TypeScript, Vite, Ant Design v5, Redux Toolkit, axios, react-hook-form + zod, lucide-react |
| Backend | Spring Boot 3.x, Spring Cloud Gateway (WebFlux), OpenFeign, Spring Data JPA, Hibernate, Lombok, ModelMapper |
| Service discovery | Netflix Eureka |
| Auth | JWT (HS256, signing key in gateway config), Spring Security |
| Database | MySQL 8 (one schema per service) |
| Build | Maven (multi-module) on backend, npm/Vite on frontend |
| Cache | Spring `@Cacheable` (in-memory by default; Caffeine-compatible) |
| API docs | Springdoc OpenAPI annotations on each controller |

---

## 4. Feature-by-feature tour

For each feature: what it does, the FE entry point, the BE chain, and the
"thing worth pointing out" in an interview.

### 4.1 Authentication & Authorization

**What it does:** Username/password login, JWT issuance, role-aware route
gating, and a register flow that mirrors the user into NotificationService.

**End-to-end flow (login):**

```
LoginPage.tsx (form)
   ↓ axios POST /api/auth/login
Gateway: AuthController.login
   ↓ AuthService.authenticate
   ↓ load user from gateway's identity DB
   ↓ BCrypt.matches(rawPwd, hashed)
   ↓ generate JWT { sub: userId, role: ROLE_X, iat, exp }
   ↓ optional: NotificationEmitter.emitDisabledAccountLogin(...)
Response: { token, userId, username, role }
   ↓
authSlice.setCredentials() → Redux + localStorage
   ↓
axiosInstance request interceptor attaches "Authorization: Bearer <jwt>"
on every subsequent call (except /auth/*)
```

**Register flow extras:**
- `NotificationEmitter.emitUserSync(...)` — POSTs to `NotificationService /api/notifications/users/sync` so role-based dispatch can reach the new user.
- `NotificationEmitter.emitUserRegistered(...)` — fires a SYSTEM notification to all ADMIN+EXECUTIVE roles ("New user registered").

**Role gating in the FE:** `RoleRoute.tsx` checks `currentUser.role` against the route's `allowedRoles` and redirects unauthorized users back to `/dashboard`.

**Talking points:**
- *"Why not OAuth/Auth0?"* — internal-facing app, simpler ops, no external IdP requirement.
- *"Where's the JWT signing key?"* — `api-gateway/application.yml` (acknowledged as something to move to env-var/secret store before prod).
- *"What if the JWT expires mid-session?"* — axios response interceptor catches 401 and dispatches `clearCredentials` + redirects to `/login`.

---

### 4.2 Dashboard

**What it does:** Single-page overview — 4 KPI tiles, aging buckets bar chart, top fraud signals, top denial reasons. Time-window filter (7d / 30d / 90d / all).

**Data composition:** unusual for a dashboard — it's not one endpoint, it's
**5 parallel calls fanned out via `Promise.allSettled`** so any one failure
degrades gracefully:

```ts
Promise.allSettled([
  ingestApi.getAll(),                       // raw claims → open count, aging, weekly volume
  claimsApi.getByMetric('TAT'),             // turnaround time avg
  claimsApi.getByMetric('LOSS_RATIO'),      // loss ratio
  riskScoresApi.getAll(),                   // top fraud signals
  denialPatternsApi.getAll(),               // top denial reasons
])
```

**Talking points:**
- *"Why `allSettled` instead of `Promise.all`?"* — if claims-metrics is down, you still want to see fraud and denial data. `all` would fail the whole dashboard.
- *"How do you calculate aging buckets?"* — client-side: take ingested-claim dates, compute `daysSince`, bucket into `0-30 / 31-60 / 61-90 / 90+`. The "13 claims breached 90-day SLA" alert is just `bucket[3].count`.

---

### 4.3 Claims Analytics

**What it does:** CRUD on claim KPIs (TAT, cycle time, severity, frequency, loss ratio). Side feature: per-claim status tracking (Open/In review/Settled/Closed) persisted to backend with localStorage as optimistic cache.

**End-to-end (Add KPI):**
```
ClaimsPage modal → handleCreate
  ↓ claimsApi.create({ claimId, metricName, metricValue, metricDate })
Gateway: forward /api/kpis/** → claims-metrics-service
ClaimKpiController.createKpi(@Valid request)
  ↓ ClaimKpiService → ClaimKpiRepository.save
  ↓ @CacheEvict("kpis", allEntries=true)
Response: ClaimKpiResponseDTO
```

**Talking points:**
- *"What was the trickiest bug?"* — claim-status persistence: on first render the localStorage cache hydrated immediately, but the backend fetch could overwrite with stale data. Fix: hydrate from BE on mount with cancellation flag, write-through cache on update.
- *"Validation strategy?"* — Zod schemas on the FE, `@Valid` + Bean Validation annotations on the BE. Both layers validate; the BE is the source of truth.

---

### 4.4 Adjusters & SLA Violations

**What it does:** Two pages from the same service.
- **Adjusters page** — performance metrics per adjuster (claims handled, avg TAT, SLA compliance rate, error rate).
- **SLA Violations page** — log + filter SLA breaches with severity (HIGH/MEDIUM/LOW), violation type, and escalation flag.

**Notable wiring:** `AdjusterAndOperations` service uses Feign to call NotificationService when an SLA threshold is crossed → notifies CLAIMS_MANAGER role.

---

### 4.5 Financial — Costs, Reserves, Aging

Three pages, one service (`cost-reserve-service`):
- **Costs** — claim cost ledger entries, grouped by `costType`, area-chart of total cost over time
- **Reserves** — current reserve amounts per claim, with `KpiCard` deltas computed against prior month
- **Aging** — explicit aging records with bucket assignment (`0-30 / 31-60 / 61-90 / 91-120 / 120+`)

**Talking point:** *"Why a separate aging table when you could derive it?"* — historical accuracy. Computing aging on demand from `ingestedDate` works for current state but loses the snapshot at month-end. Storing aging records lets you ask *"how was the portfolio aging on March 31?"*.

---

### 4.6 Risk Intelligence — Fraud Risk & Denial Leakage

**Fraud Risk page:**
- Risk scores per claim (numeric 0-100)
- Risk indicators (REPEAT_PATTERN, UNUSUAL_TIMING, HIGH_COST, etc., each with severity)
- Filters: score threshold (≥70/≥85/critical), indicator type, time window
- Auto-generates SYSTEM alerts via NotificationService when score ≥ 75

**Denial Leakage page:**
- Two correlated entities: `denial_pattern` (which codes are getting denied) and `leakage_flag` (estimated $ lost per claim)
- Filters: leakage type, time period
- Click a pattern → filter the claim queue to claims hitting that pattern

**Talking point:** *"How do scores get computed?"* — currently rule-based: each indicator contributes weighted points; a row in `risk_scores` is created on ingest. Real-world this would be ML-driven; the architecture is ready (the indicator+score split mirrors the feature/inference pattern).

---

### 4.7 Data Ingestion — Feeds & Raw Claims

This is the **entry point** for everything else. Two-table model:

```
data_feed (1) ───────── (N) claim_raw
  feedId                    rawId
  feedType (CLAIM/POLICY/   claimId  ← business ID from upstream
            PAYMENT/RESERVE)feedId   FK
  sourceSystem              payloadJson (LONGTEXT)
  status (ACTIVE/INACTIVE/  ingestedDate
          FAILED)
  lastSyncDate
```

**Two operations, two pages:**
1. **FeedsPage** registers a source (`POST /api/feeds`) — done once per upstream system.
2. **RawClaimsPage** ingests payloads (`POST /api/ingest`) — done N times per feed.

**The interesting business rule:** ingestion is gated on feed status:
```java
if (feed.getStatus() != FeedStatus.ACTIVE) {
    notifyIngestionRejected(feed, request);   // SYSTEM notification → ADMIN+EXECUTIVE
    throw new InvalidFeedStatusException(...) // 400 to caller
}
```
Why both? The HTTP 400 is for the upstream caller (often a batch job with no human). The notification ensures **a human knows the feed is dead** even when the batch caller silently retries.

**Talking point:** *"Why is `claimId` user-supplied, not auto-generated?"* — we don't *create* claims here, we *mirror* claims from upstream systems that already have IDs. Storing the upstream ID verbatim lets every downstream service correlate by it.

---

### 4.8 Reports

**What it does:** Generate analytics reports (claim portfolio, fraud trends, etc.) with filters by scope and date range, export as CSV (client-side Blob download).

**End-to-end (Generate report):**
```
ReportsPage → handleCreate
  ↓ reportsApi.create({ scope, scopeValue, metrics, generatedDate, generatedBy })
analytics-report-service
  ↓ ReportService.create
  ↓ stores report metadata + reportData JSON snapshot
```

**Export:** No server-side PDF generation. All exports are client-side `Blob` downloads — keeps the service stateless and avoids dragging in a heavy PDF lib.

---

### 4.9 Notifications (the trickiest one)

**What it does:** A central inbox per user, populated from two sources:
1. Manual creation via NotificationsPage modal (admin-only typically)
2. **Cross-service dispatch** — any service can fan out a notification to a set of users *or* to all users of a given role

**The cross-service dispatch contract:**
```java
NotificationDispatchRequestDTO {
  Set<Long>    targetUserIds;   // explicit recipients
  Set<String>  targetRoles;     // role-based fan-out (ADMIN, ANALYST, …)
  String       title;
  String       message;
  String       category;        // RISK | DENIAL | COST | PERFORMANCE | AGING | SYSTEM
  String       referenceId;     // usually a claimId
}
```

**Role-based fan-out resolution** (in `NotificationServiceImpl`):
```sql
SELECT u.userId FROM User u WHERE u.role IN :roles AND u.isActive = true
```
This is why the **user-sync from the gateway matters** — if newly-registered users aren't mirrored into NotificationService's `users` table, they won't appear in this query and will never receive role-targeted notifications.

**The bell:**
- `NotificationBell.tsx` polls `/notifications/unread-count/{userId}` every 30s
- `useNotificationPolling` hook handles cleanup on unmount
- Click the bell → dropdown → click "View all" → `/notifications` page

**Two envelope shapes (this trips people up):**
- 8 services use: `{ timestamp, status, error, message }`
- NotificationService alone uses: `{ success, message, data }`

The FE handles both via a normalizer in `axiosInstance.ts:getApiErrorMessage` — extracts a human-readable error message regardless of which envelope the BE emits. This is what unblocked surfacing real backend errors (e.g., "Cannot ingest into feed 3 — status is INACTIVE") in the UI instead of generic "Failed" toasts.

**Talking points:**
- *"Why two envelope shapes?"* — historical drift. NotificationService was built earlier with `ApiResponse<>` wrapper; the rest standardized on the Spring `ErrorResponse` shape later. Rather than rewriting every NotificationService controller, we normalize on read in the FE.
- *"What happens if a notification fails to dispatch?"* — non-blocking. The Feign call is wrapped in try/catch; the originating action (e.g., claim ingestion) succeeds even if NotificationService is down. Critical for resilience: a notification outage shouldn't block business operations.

---

### 4.10 Admin

Three admin-only pages (gated by `RoleRoute` requiring `ROLE_ADMIN`):
- **Users & Roles** — CRUD on platform users
- **KPI Definitions** — manage the metric catalog (what TAT/LOSS_RATIO/etc. mean)
- **Audit Logs** — read-only view of audit-trail entries (login, role changes, deletions)

---

## 5. Cross-cutting concerns

### 5.1 Error envelope normalization (FE)

```ts
// axiosInstance.ts
export function getApiErrorMessage(err: unknown): string {
  const ax = err as AxiosError<any>;
  if (ax?.response?.data) {
    const d = ax.response.data;
    if (typeof d === 'string') return d;                 // raw String body
    if (typeof d.message === 'string') return d.message; // 8 services
    if (typeof d.error === 'string')   return d.error;   // fallback
  }
  if (ax?.code === 'ECONNABORTED') return 'Request timed out…';
  if (ax?.message === 'Network Error') return 'Cannot reach the server…';
  return ax?.message || 'Something went wrong.';
}
```

Used by every page's catch block as `(err as any).userMessage` so backend
validation messages, business-rule failures, and network errors all surface
consistently.

### 5.2 Caching

Spring `@Cacheable` on hot read paths:
```java
@Cacheable(value = "rawClaims", key = "'all'")
public List<IngestionResponseDTO> getAllRawClaims() { ... }

@CacheEvict(value = {"rawClaims", "feeds"}, allEntries = true)
public IngestionResponseDTO ingestClaim(...) { ... }
```
Talking point: *"Why per-method cache instead of a CDN?"* — caches are short-lived in-process, scoped to derived views. CDN-level caching would be wrong for authenticated, user-scoped data.

### 5.3 Service discovery & inter-service calls

```java
// In a WebMVC service:
@FeignClient(name = "NotificationService")
public interface NotificationServiceClient {
    @PostMapping("/api/notifications/dispatch")
    void dispatchNotification(@RequestBody NotificationDispatchRequestDTO req);
}

// In the WebFlux gateway:
WebClient.builder().baseUrl("lb://NotificationService").build()
```

`@FeignClient(name=…)` resolves via Eureka; `lb://` prefix tells Spring Cloud LoadBalancer to do client-side load balancing. No hardcoded ports anywhere except `application.yml`.

### 5.4 Database per service

Each service owns its schema. No FKs across schemas. Cross-service relationships
are by ID only (e.g., `claim_raw.claimId` is a string that other services
correlate to but don't enforce referential integrity on).

**Why:** independent deploys, independent migrations, no schema-coupling
deadlock when one team is blocked on another's DDL change.

**Tradeoff:** consistency across services is eventual, not transactional. A claim that exists in `claim_raw` may not yet exist in `risk_scores`. The UI handles this by treating each domain independently.

---

## 6. Notable decisions & tradeoffs

| Decision | Why | Tradeoff |
|---|---|---|
| Spring Cloud Gateway (WebFlux) for ingress, Spring MVC for services | WebFlux is non-blocking and good at I/O-heavy auth/routing; WebMVC is simpler for CRUD-heavy services | Two paradigms means two HTTP-client patterns (`WebClient` vs Feign) and two exception-handler styles |
| Per-service MySQL schemas | Independent deploy/migration | No cross-schema joins; can't atomically update across domains |
| JWT issued by gateway, services trust it | No per-service auth DB lookup; minimal latency | Token revocation is hard; rely on short expiry |
| Role-based notification fan-out via mirrored user table | Eventually consistent role membership; one source of truth (gateway) | Sync hop on every register/login; if NotificationService is down during register, the new user is invisible until next sync |
| Standard error envelope `{timestamp, status, error, message}` | Predictable shape across 8 services | NotificationService is the historical odd-one-out (`{success, message, data}`); FE normalizes |
| 30-second poll on the bell | Simple, no WebSocket infra needed | Up to 30 s latency for a new alert to show |
| No PDF generation server-side | CSV is enough; export logic stays out of the BE | If a stakeholder asks for branded PDF, you have to add a service |

---

## 7. Common interview questions — quick answers

**Q: Walk me through what happens when a claim is ingested.**
A: User hits "Ingest claim" on RawClaimsPage. FE POSTs `{claimId, feedId, payloadJson}` to `/api/ingest`. Gateway forwards to `data-ingestion-service`. The service looks up the feed by ID — 404 if missing. Checks `feed.status == ACTIVE` — if not, fires a SYSTEM notification to ADMIN+EXECUTIVE roles via Feign and throws `InvalidFeedStatusException` (400). If active, persists a `ClaimRaw` row with the JSON payload, bumps `feed.lastSyncDate`, evicts the `rawClaims` cache, returns 201 with the saved DTO including the auto-generated `rawId` and `ingestedDate`.

**Q: How does authorization work after login?**
A: Gateway issues a JWT with `sub`, `role`, and standard claims. FE stores in Redux + localStorage. Axios request interceptor attaches `Authorization: Bearer <jwt>` to every non-auth request. Services don't validate JWT — they trust the gateway. Role gating is enforced (a) in the gateway's security config for path-based access, (b) in the FE via `RoleRoute` for navigation.

**Q: What's the most interesting bug you fixed?**
A: Two candidates. (1) Mojibake in denial reasons — UTF-8 em-dash bytes were stored as 3 separate Latin-1 characters because MySQL connection charset was set to a DOS code page during seed. Fixed with a `REPLACE` migration. (2) NotificationService's `users/sync` endpoint was called by the gateway but never implemented. Newly-registered users were invisible to role-based dispatch. Fixed by adding the repository, service method, and controller endpoint with idempotent `INSERT ... ON DUPLICATE KEY UPDATE`.

**Q: How would you scale this to 100x traffic?**
A: Several layers. (1) Multiple instances of each service registered with Eureka — load balancer fans out automatically. (2) Database read replicas; `@Cacheable` already protects hot reads. (3) NotificationService dispatch becomes async via a message queue (Kafka/RabbitMQ) so the calling service doesn't block on Feign. (4) The 30-second bell poll becomes WebSocket / SSE. (5) Gateway becomes stateless and replicated.

**Q: What would you change if you started over?**
A: (1) One error envelope from day one (NotificationService's `ApiResponse<>` wrapper everywhere or the `{timestamp, status, error, message}` shape everywhere — picking either is fine, mixing isn't). (2) Move the JWT signing key to env var / secret manager from the start. (3) An event bus instead of synchronous Feign for cross-service notifications — the current pattern works but tightly couples services to NotificationService availability. (4) Contract tests (Pact or similar) between FE and each service so DTO drift is caught at CI.

**Q: How is the frontend organized?**
A: Vite + React + TypeScript + AntD. `/src/api/` has one file per backend service (`claimsApi.ts`, `feedsApi.ts`, etc.). `/src/pages/` is route-grouped. `/src/components/ui/` has the design-system primitives (`KpiCard`, `DataCard`, `Chip`, `EmptyState`, `ErrorState`). `/src/store/` is Redux Toolkit slices (auth is the only persisted state). `/src/hooks/` contains the `useAsync` reducer-based fetch hook used by every data page.

**Q: How do you handle errors in the UI?**
A: Two places. (a) `axiosInstance.ts` response interceptor extracts a normalized message via `getApiErrorMessage` and attaches it as `err.userMessage`. (b) Every page's catch block reads `err.userMessage` and surfaces it via either the AntD `message.error` toast (mutations) or a page-level `<Alert>` (loads). The interceptor also handles 401 by clearing auth and redirecting.

---

## 8. Be ready to defend / improve

**Things you should acknowledge as not-yet-done:**
- JWT signing key is in `application.yml` (should be env var)
- Notification endpoints lack per-user authorization — `GET /api/notifications/user/{userId}` doesn't verify the JWT's userId matches the path
- Gateway CORS is `allowedOrigins: "*"` (fine for dev, must tighten for prod)
- No automated end-to-end tests yet (just unit/integration)
- DTO field-type drift: BE accepts `NotificationCategory` enum, FE/clients send `String`. Currently works because Jackson coerces, but it's schema fragility

**Things to mention as "nice next steps":**
- Migrate cross-service notifications to event-driven (Kafka topic per category)
- WebSocket bell instead of 30s poll
- Contract testing between FE and BE
- Centralized logging (ELK or Loki + Grafana)
- Observability: Micrometer metrics + Prometheus, distributed tracing via OpenTelemetry

---

## 9. Numbers worth memorizing

- **9 backend services** (1 gateway + 8 business services) + 1 Eureka registry
- **~60 frontend source files** in the main React app
- **6 notification categories** (RISK, DENIAL, COST, PERFORMANCE, AGING, SYSTEM)
- **6 user roles** (ADMIN, CLAIMS_ANALYST, CLAIMS_MANAGER, FRAUD_ANALYST, ACTUARY, OPERATIONS_EXEC)
- **~10 RESTful endpoints per service** (CRUD + 1-2 query variants + 1 metric/aggregate)
- **Gateway port 8086, Eureka 8761, services 8080-8088** (per `application.yml`)
