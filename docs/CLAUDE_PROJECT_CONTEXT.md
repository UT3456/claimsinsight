# ClaimInsight360 - COMPREHENSIVE PROJECT CONTEXT FILE
**For: Claude AI Code Generation & Interview Question Generation**  
**Generated: April 28, 2026**  
**Total Project Size: ~5,500 LOC**

---

## EXECUTIVE SUMMARY

**Project Name:** ClaimInsight360  
**Type:** Enterprise Insurance Claims Analytics & Intelligence Platform  
**Architecture:** Microservices (10 Spring Boot services) + React 18 Frontend  
**Status:** Production-Ready  
**Purpose:** Give to Claude to generate detailed interview questions/answers covering every project detail

---

## I. COMPLETE SYSTEM ARCHITECTURE

### Service Topology & Port Mapping

```
┌─────────────────────────────────────────────────────────┐
│ Browser (React 18 + TypeScript)                         │
│ Frontend runs on port 3000 (Vite dev server)           │
└─────────────────────────────────────────────────────────┘
                       ↓ HTTPS
┌─────────────────────────────────────────────────────────┐
│ API GATEWAY (Spring Cloud Gateway + WebFlux)            │
│ Port: 8086                                              │
│ - JWT issuance & validation                             │
│ - Request routing by path predicates                    │
│ - CORS handling                                         │
│ - Single entry point for all requests                  │
└─────────────────────────────────────────────────────────┘
                  ↓ via Eureka load balancing
┌─────────────────────────────────────────────────────────┐
│ EUREKA SERVICE REGISTRY                                 │
│ Port: 8761                                              │
│ - All 9 services register here on startup              │
│ - Spring Cloud LoadBalancer picks instances            │
└─────────────────────────────────────────────────────────┘
    ↓          ↓          ↓          ↓          ↓
9 MICROSERVICES (Spring Boot WebMVC):
├── data-ingestion-service (8082) - Raw claim ingestion
├── claims-metrics-service (8083) - KPI computation
├── fraud-risk-service (8090) - Risk scoring
├── denial-leakage-service (8085) - Denial patterns
├── cost-reserve-service (8089) - Reserves & aging
├── AdjusterAndOperations (8087) - Adjuster perf & SLA
├── NotificationService (8088) - Central alert hub
├── analytics-report-service (8084) - Report generation
└── [eureka-server] (8761) - Service registry

DATABASE LAYER (MySQL 8, per-service schema):
├── data_ingestion_db
├── claims_metrics_db
├── fraud_risk_db
├── denial_leakage_db
├── cost_reserve_db
├── adjuster_ops_db
├── analytics_report_db
└── notifications_db
```

### Service Ports Reference Table

| Service Name | Port | Type | Purpose |
|---|---|---|---|
| Eureka Server | 8761 | Registry | Service discovery & registration |
| API Gateway | 8086 | WebFlux | Single ingress, JWT issuance, routing |
| Data Ingestion | 8082 | WebMVC | Raw claim ingestion, feed management |
| Claims Metrics | 8083 | WebMVC | KPI computation, claim status tracking |
| Fraud Risk | 8090 | WebMVC | Risk scoring, fraud indicators |
| Denial Leakage | 8085 | WebMVC | Denial patterns, leakage estimation |
| Cost Reserve | 8089 | WebMVC | Reserves, costs, aging buckets |
| Adjuster Ops | 8087 | WebMVC | Adjuster performance, SLA violations |
| Analytics Reports | 8084 | WebMVC | Report generation, snapshots |
| Notifications | 8088 | WebMVC | Central alert hub, role-based dispatch |

---

## II. BACKEND ARCHITECTURE (Spring Boot Microservices)

### 2.1 API Gateway (Port 8086) - WebFlux/Reactive

**Framework:** Spring Cloud Gateway + Spring WebFlux  
**Key Responsibility:** Single ingress point, authentication, routing

**Configuration (application.yml):**
```yaml
server:
  port: 8086

spring:
  application:
    name: api-gateway
  main:
    web-application-type: reactive  # WebFlux mode

  cloud:
    gateway:
      discovery:
        locator:
          enabled: true
          lower-case-service-id: true
      routes:
        - id: data-ingestion-feeds
          uri: lb://data-ingestion-service
          predicates:
            - Path=/api/feeds/**
        - id: data-ingestion-ingest
          uri: lb://data-ingestion-service
          predicates:
            - Path=/api/ingest/**
        # ... more routes for each service

eureka:
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka/

application:
  security:
    jwt:
      secret-key: 5367566B59703373367639792F423F4528482B4D6251655468576D5A71347437
      expiration: 86400000  # 24 hours
```

**Key Features:**
- JWT issuance on `/api/auth/login`
- Request routing to downstream services via `lb://` prefix (load balanced)
- CORS configuration (currently: `allowedOrigins: *`)
- Path-based predicate routing

**Authentication Flow:**
1. User POSTs `/api/auth/login` with username/password
2. Gateway validates against embedded identity DB
3. Gateway generates JWT: `{ sub: userId, role: ROLE_X, iat, exp }`
4. Response: `{ token, userId, username, role }`
5. Frontend stores in Redux + localStorage
6. All subsequent requests include: `Authorization: Bearer <jwt>`
7. Services trust gateway-issued JWT (no re-validation at service level)

---

### 2.2 Eureka Server (Port 8761) - Service Registry

**Configuration (application.yml):**
```yaml
server:
  port: 8761

eureka:
  instance:
    hostname: localhost
  client:
    register-with-eureka: false  # It's the server
    fetch-registry: false
  server:
    wait-time-in-ms-when-sync-empty: 0
    enable-self-preservation: false  # Dev mode
    eviction-interval-timer-in-ms: 5000
```

**Role:** Central registry for all 9 microservices  
**Usage:** Services register on startup; gateway & Feign clients look up by service name

---

### 2.3 Data Ingestion Service (Port 8082) - WebMVC

**Database Schema:**
```sql
CREATE TABLE data_feed (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  feedType ENUM('CLAIM', 'POLICY', 'PAYMENT', 'RESERVE') NOT NULL,
  sourceSystem VARCHAR(255) NOT NULL,
  status ENUM('ACTIVE', 'INACTIVE', 'FAILED') DEFAULT 'ACTIVE',
  lastSyncDate TIMESTAMP,
  createdAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE claim_raw (
  rawId BIGINT PRIMARY KEY AUTO_INCREMENT,
  claimId VARCHAR(50) NOT NULL,
  feedId BIGINT NOT NULL,
  payloadJson LONGTEXT NOT NULL,
  ingestedDate TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (feedId) REFERENCES data_feed(id)
);
```

**REST APIs:**
- `POST /api/feeds` - Register new feed (201)
- `GET /api/feeds` - List all feeds (cached)
- `GET /api/feeds/{feedId}` - Get feed by ID (cached)
- `PUT /api/feeds/{feedId}/status` - Update feed status (200)
- `DELETE /api/feeds/{feedId}` - Delete feed (204)
- `POST /api/ingest` - Ingest raw claim (201)
- `GET /api/ingest/raw-claims` - List ingested claims (cached)
- `GET /api/ingest/raw-claims/{claimId}` - Get by claim ID (cached)

**Key Business Logic:**
```java
// Ingestion gated on feed status
if (feed.getStatus() != FeedStatus.ACTIVE) {
    // 1. Emit SYSTEM notification to ADMIN role
    notificationEmitter.emitIngestionRejected(feed, request);
    
    // 2. Throw error for caller
    throw new InvalidFeedStatusException("Feed must be ACTIVE");
}

// Save claim + invalidate cache
claimRepository.save(claimRaw);
cacheManager.getCache("rawClaims").clear();
```

**Caching:**
```java
@Cacheable(value = "rawClaims", key = "'all'")
public List<IngestionResponseDTO> getAllRawClaims() { }

@CacheEvict(value = {"rawClaims", "feeds"}, allEntries = true)
public IngestionResponseDTO ingestClaim(IngestionRequestDTO req) { }
```

**Known Issues:**
- UTF-8 encoding issue: Mojibake in denial reasons due to connection charset set to DOS code page during seed
- Fix: `ALTER TABLE claim_raw MODIFY payloadJson LONGTEXT CHARACTER SET utf8mb4;`

---

### 2.4 Claims Metrics Service (Port 8083) - WebMVC

**Database Schema:**
```sql
CREATE TABLE claim_kpi (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  claimId VARCHAR(50) NOT NULL,
  metricName VARCHAR(50) NOT NULL,  -- TAT, LOSS_RATIO, CYCLE_TIME, FREQUENCY, SLA_COMPLIANCE
  metricValue DECIMAL(10, 2) NOT NULL,
  metricDate TIMESTAMP,
  createdAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE claim_status_history (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  claimId VARCHAR(50) NOT NULL,
  status ENUM('OPEN', 'UNDER_REVIEW', 'APPROVED', 'DENIED', 'CLOSED') NOT NULL,
  changedAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

**KPI Types:**
| KPI | Unit | Meaning |
|---|---|---|
| TAT | Days | Average turnaround time to settle |
| LOSS_RATIO | Decimal | Claims paid / premiums collected |
| CYCLE_TIME | Days | Days from assignment to closure |
| FREQUENCY | Count | Number of claims in period |
| SLA_COMPLIANCE | Percent | Percent of on-time closes |

**REST APIs:**
- `GET /api/kpis` - All KPIs (cached)
- `GET /api/kpis/by-metric/{metricName}` - KPIs by type
- `POST /api/kpis` - Create KPI
- `PUT /api/kpis/{id}` - Update KPI
- `DELETE /api/kpis/{id}` - Delete KPI
- `POST /api/claim-status` - Update claim status

---

### 2.5 Fraud Risk Service (Port 8090) - WebMVC

**Annotations:**
```java
@SpringBootApplication
@EnableAspectJAutoProxy
@EnableDiscoveryClient  // Register with Eureka
@EnableFeignClients    // Enable Feign for inter-service calls
public class FraudRiskServiceApplication { }
```

**Database Schema:**
```sql
CREATE TABLE risk_scores (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  claimId VARCHAR(50) NOT NULL,
  score INT NOT NULL,  -- 0-100
  createdAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE risk_indicators (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  claimId VARCHAR(50) NOT NULL,
  indicatorType VARCHAR(50) NOT NULL,
  severity ENUM('LOW', 'MEDIUM', 'HIGH') NOT NULL,
  createdAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

**Risk Score Calculation:**
- Range: 0-100 (higher = riskier)
- ≥75: Auto-alert FRAUD_ANALYST role via NotificationService

**Indicator Types:**
- REPEAT_PATTERN: Claimant has multiple claims
- UNUSUAL_TIMING: Claim filed outside normal patterns
- HIGH_COST: Claim amount unusually high
- MULTIPLE_CLAIMS: Multiple simultaneous claims
- PROVIDER_MISMATCH: Provider not in database

**Feign Client (for inter-service communication):**
```java
@FeignClient(name = "NotificationService")
public interface NotificationServiceClient {
    @PostMapping("/api/notifications/dispatch")
    void dispatchNotification(@RequestBody NotificationDispatchRequestDTO req);
}

// Usage:
try {
    notificationClient.dispatchNotification(NotificationDispatchRequestDTO.builder()
        .targetRoles(Set.of("FRAUD_ANALYST"))
        .title("High fraud risk detected")
        .message("Claim CLM-001 scored 87 (critical)")
        .category("RISK")
        .referenceId("CLM-001")
        .build());
} catch (FeignException e) {
    log.warn("Failed to dispatch notification (non-blocking)", e);
}
```

**REST APIs:**
- `GET /api/risk-scores` - All risk scores
- `GET /api/risk-scores/{claimId}` - Score for specific claim
- `GET /api/risk-indicators` - All indicators
- `POST /api/risk-indicators` - Create indicator

---

### 2.6 Denial Leakage Service (Port 8085) - WebMVC

**Database Schema:**
```sql
CREATE TABLE denial_patterns (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  denialCode VARCHAR(50) NOT NULL,
  frequency INT NOT NULL,
  impactAmount DECIMAL(12, 2) NOT NULL,
  lastOccurrence TIMESTAMP
);

CREATE TABLE leakage_flags (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  claimId VARCHAR(50) NOT NULL,
  estimatedLoss DECIMAL(12, 2) NOT NULL,
  flagType VARCHAR(50) NOT NULL,
  createdAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

**REST APIs:**
- `GET /api/denial-patterns` - Top denial codes
- `GET /api/leakage-flags` - Estimated losses by claim
- `POST /api/denial-patterns` - Create pattern
- `POST /api/leakage-flags` - Create leakage flag

---

### 2.7 Cost Reserve Service (Port 8089) - WebMVC

**Database Schema:**
```sql
CREATE TABLE reserve_adjustments (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  claimId VARCHAR(50) NOT NULL,
  amount DECIMAL(12, 2) NOT NULL,
  adjustmentDate TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE cost_ledger (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  claimId VARCHAR(50) NOT NULL,
  costType VARCHAR(50) NOT NULL,
  amount DECIMAL(12, 2) NOT NULL,
  date TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE aging_buckets (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  claimId VARCHAR(50) NOT NULL,
  bucket ENUM('0-30', '31-60', '61-90', '91-120', '120+') NOT NULL,
  recordedDate TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

**REST APIs:**
- `GET /api/reserves` - All reserves
- `POST /api/reserves` - Create reserve
- `GET /api/costs` - Cost ledger entries
- `POST /api/costs` - Add cost entry
- `GET /api/aging` - Aging bucket records

---

### 2.8 Adjuster & Operations (Port 8087) - WebMVC

**Database Schema:**
```sql
CREATE TABLE adjusters (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(255) NOT NULL,
  email VARCHAR(255) NOT NULL,
  claimsHandled INT DEFAULT 0,
  avgTAT DECIMAL(5, 2),
  qualityScore DECIMAL(3, 1),
  createdAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE sla_violations (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  claimId VARCHAR(50) NOT NULL,
  adjusterId BIGINT NOT NULL,
  violationType VARCHAR(50) NOT NULL,
  severity ENUM('HIGH', 'MEDIUM', 'LOW') NOT NULL,
  escalated BOOLEAN DEFAULT FALSE,
  createdAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (adjusterId) REFERENCES adjusters(id)
);
```

**REST APIs:**
- `GET /api/adjusters` - All adjusters
- `GET /api/adjusters/{id}` - Adjuster details
- `GET /api/sla-violations` - All SLA violations
- `POST /api/sla-violations` - Report SLA violation

**Key Feature:** Calls NotificationService when SLA threshold crossed → alerts CLAIMS_MANAGER role

---

### 2.9 Analytics Report Service (Port 8084) - WebMVC

**Database Schema:**
```sql
CREATE TABLE reports (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  reportName VARCHAR(255) NOT NULL,
  scope VARCHAR(50) NOT NULL,
  scopeValue VARCHAR(255),
  generatedDate TIMESTAMP,
  generatedBy VARCHAR(255),
  createdAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE report_data (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  reportId BIGINT NOT NULL,
  dataJson LONGTEXT NOT NULL,
  createdAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (reportId) REFERENCES reports(id)
);
```

**REST APIs:**
- `GET /api/reports` - List all reports
- `POST /api/reports` - Generate new report
- `GET /api/reports/{id}` - Get report by ID

**Key Feature:** Export as CSV (client-side via JavaScript Blob)

---

### 2.10 Notification Service (Port 8088) - WebMVC

**Database Schema:**
```sql
CREATE TABLE notifications (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  userId BIGINT NOT NULL,
  title VARCHAR(255) NOT NULL,
  message TEXT NOT NULL,
  category ENUM('RISK', 'DENIAL', 'COST', 'PERFORMANCE', 'AGING', 'SYSTEM') NOT NULL,
  referenceId VARCHAR(50),
  status ENUM('UNREAD', 'READ', 'ARCHIVED') DEFAULT 'UNREAD',
  createdAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE users (
  userId BIGINT PRIMARY KEY,
  role ENUM('ADMIN', 'ANALYST', 'MANAGER', 'FRAUD_ANALYST', 'ACTUARY', 'OPS_EXEC') NOT NULL,
  isActive BOOLEAN DEFAULT TRUE
);
```

**Notification Dispatch DTO:**
```java
NotificationDispatchRequestDTO {
    Set<Long> targetUserIds;        // Explicit recipients
    Set<String> targetRoles;        // Role-based: ADMIN, ANALYST, etc.
    String title;
    String message;
    String category;                // RISK, DENIAL, COST, PERFORMANCE, AGING, SYSTEM
    String referenceId;             // claimId for deep linking
}
```

**Role-Based Resolution Logic:**
```java
List<User> recipients = userRepository.findByRoleIn(request.getTargetRoles());
for (User user : recipients) {
    Notification notif = new Notification();
    notif.setUserId(user.getUserId());
    notif.setTitle(request.getTitle());
    notif.setMessage(request.getMessage());
    notificationRepository.save(notif);
}
```

**REST APIs:**
- `GET /api/notifications/user/{userId}` - Get user notifications
- `GET /api/notifications/unread-count/{userId}` - Unread count
- `POST /api/notifications/dispatch` - Dispatch notification (called by other services)
- `POST /api/notifications/users/sync` - Sync new users (called by gateway after registration)
- `PUT /api/notifications/{id}/read` - Mark as read

**Known Bug (Fixed):**
- Originally `POST /api/notifications/users/sync` endpoint was missing
- Newly registered users weren't mirrored into NotificationService's users table
- Result: New users invisible to role-based dispatch
- Fix: Implement idempotent sync endpoint

**Error Envelope (Unique to this service):**
```json
{
  "success": false,
  "message": "User not found",
  "data": null
}
```
(Other 8 services use `{timestamp, status, error, message}`)

---

## III. FRONTEND ARCHITECTURE (React 18 + TypeScript)

### Technology Stack

```json
{
  "react": "18.2.0",
  "typescript": "5.2.2",
  "vite": "5.0.8",
  "@reduxjs/toolkit": "1.9.7",
  "react-redux": "8.1.3",
  "react-router-dom": "6.20.0",
  "axios": "1.6.5",
  "recharts": "2.10.3",
  "lucide-react": "0.294.0",
  "date-fns": "2.30.0",
  "@headlessui/react": "1.7.17",
  "@types/react": "18.2.43",
  "@types/react-dom": "18.2.17",
  "eslint": "8.56.0"
}
```

### Project Structure

```
claiminsight360-frontend-v3/
├── src/
│   ├── components/           # Reusable UI components (8+)
│   │   ├── Button.tsx
│   │   ├── Card.tsx
│   │   ├── Badge.tsx
│   │   ├── Alert.tsx
│   │   ├── LoadingSpinner.tsx
│   │   ├── Table.tsx
│   │   ├── Form.tsx
│   │   └── Layout.tsx (sidebar + header)
│   │
│   ├── pages/                # Page components (11+)
│   │   ├── LoginPage.tsx
│   │   ├── DashboardPage.tsx
│   │   ├── ClaimsPage.tsx
│   │   ├── ClaimDetailPage.tsx
│   │   ├── DenialsPage.tsx
│   │   ├── FraudPage.tsx
│   │   ├── ReservesPage.tsx
│   │   ├── ReportsPage.tsx
│   │   ├── AdjustersPage.tsx
│   │   ├── NotificationsPage.tsx
│   │   ├── AdminPage.tsx
│   │   └── NotFoundPage.tsx
│   │
│   ├── services/             # API integration (8+)
│   │   ├── apiClient.ts
│   │   ├── authService.ts
│   │   ├── claimsService.ts
│   │   ├── fraudService.ts
│   │   ├── denialService.ts
│   │   ├── reserveService.ts
│   │   ├── adjusterService.ts
│   │   └── notificationService.ts
│   │
│   ├── store/                # Redux state management
│   │   ├── index.ts
│   │   ├── authSlice.ts (only persisted state)
│   │   └── claimsSlice.ts
│   │
│   ├── types/                # TypeScript definitions
│   │   └── index.ts (50+ interfaces)
│   │
│   ├── hooks/                # Custom React hooks
│   │   ├── useAppState.ts
│   │   ├── useFetch.ts
│   │   ├── useForm.ts
│   │   └── index.ts
│   │
│   ├── middleware/           # Route protection
│   │   ├── ProtectedRoute.tsx
│   │   └── index.ts
│   │
│   ├── utils/                # Helper functions
│   │   └── helpers.ts
│   │
│   ├── styles/               # CSS
│   │   ├── globals.css
│   │   └── components.css
│   │
│   ├── App.tsx               # Routes configuration
│   ├── main.tsx              # React DOM entry point
│   └── vite-env.d.ts
│
├── index.html
├── package.json
├── tsconfig.json
├── vite.config.ts
├── eslint.config.js
└── .env (VITE_API_GATEWAY_URL=http://localhost:8086)
```

### Frontend Statistics

| Metric | Count |
|--------|-------|
| Total Files | 44+ |
| Total LOC | ~2,750 |
| Components | 8+ |
| Pages | 11+ |
| API Services | 8+ |
| Custom Hooks | 4+ |
| TypeScript Types | 50+ |

### Key Features Implemented

**Authentication & Authorization:**
- Login with form validation
- JWT token management (stored in Redux + localStorage)
- Automatic token injection in every request (axios interceptor)
- Protected routes with role-based access control
- Auto-logout on 401 (unauthorized)

**Dashboard & Analytics:**
- 4 KPI cards with delta indicators (% change)
- Aging buckets bar chart
- Top fraud signals (scores ≥70)
- Top denial reasons (codes)
- Time-window filter (7d / 30d / 90d / all)

**Claims Management:**
- Claims list with pagination
- Advanced filtering (status, type, priority, date range)
- Claim detail view
- Claim status tracking (OPEN → UNDER_REVIEW → APPROVED/DENIED → CLOSED)
- Assignment to adjusters

**Fraud Detection:**
- Risk scores per claim (0-100)
- Risk indicators with severity (LOW/MEDIUM/HIGH)
- Filter by score threshold (≥70/≥85/critical)
- Filter by indicator type
- Auto-alerts for high-risk claims

**Denial Analysis:**
- Denial patterns tracking (top codes)
- Leakage estimates ($ lost)
- Trend analysis
- Click pattern → filter related claims

**Financial Management:**
- Reserve tracking & adjustments
- Cost ledger entries
- Aging bucket analysis
- Reserve comparison (month-over-month deltas)

**Adjuster Management:**
- Adjuster directory with performance metrics
- Workload tracking (claims handled)
- Quality score tracking
- Assignment management

**Notifications:**
- Real-time notification bell (30s polling)
- Full notification inbox (paginated)
- Mark as read/unread
- Filter by category (RISK, DENIAL, COST, PERFORMANCE, AGING, SYSTEM)

**Admin Panel:**
- User management (CRUD)
- Role management
- KPI definitions
- Audit log viewing

### Redux State Structure

```typescript
// authSlice.ts
export interface AuthState {
  user: {
    userId: string,
    username: string,
    email: string,
    role: 'ADMIN' | 'ANALYST' | 'MANAGER' | 'FRAUD_ANALYST' | 'ACTUARY' | 'OPS_EXEC',
    isActive: boolean,
  },
  token: string | null,
  isAuthenticated: boolean,
  loading: boolean,
  error?: string,
}

// claimsSlice.ts
export interface ClaimsState {
  claims: Claim[],
  selectedClaim: Claim | null,
  filters: ClaimFilter,
  loading: boolean,
  error?: string,
  pagination: { page: number, pageSize: number },
}
```

### API Integration Pattern (All Services)

```typescript
// src/services/claimsService.ts
import apiClient from './apiClient';

export const claimsApi = {
  getAll: async (filters?: ClaimFilter) => {
    const { data } = await apiClient.get('/api/kpis', { params: filters });
    return data;
  },

  getById: async (id: string) => {
    const { data } = await apiClient.get(`/api/kpis/${id}`);
    return data;
  },

  create: async (dto: CreateClaimDTO) => {
    const { data } = await apiClient.post('/api/kpis', dto);
    return data;
  },

  update: async (id: string, dto: UpdateClaimDTO) => {
    const { data } = await apiClient.put(`/api/kpis/${id}`, dto);
    return data;
  },

  delete: async (id: string) => {
    await apiClient.delete(`/api/kpis/${id}`);
  },
};
```

### Axios Interceptor Configuration

```typescript
// Request interceptor: Add JWT to all requests
axiosInstance.interceptors.request.use((config) => {
  const token = localStorage.getItem('token');
  if (token && !config.url.includes('/auth/')) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// Response interceptor: Handle 401 + normalize errors
axiosInstance.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      store.dispatch(clearCredentials());
      window.location.href = '/login';
    }
    error.userMessage = getApiErrorMessage(error);
    return Promise.reject(error);
  }
);

// Error message extraction (handles both envelope types)
function getApiErrorMessage(err: AxiosError<any>) {
  const d = err.response?.data;
  if (typeof d === 'string') return d;
  if (typeof d.message === 'string') return d.message;  // 8 services
  if (typeof d.error === 'string') return d.error;       // NotificationService oddity
  return err.message || 'Something went wrong.';
}
```

---

## IV. KEY ARCHITECTURAL PATTERNS

### 1. Database-Per-Service Pattern

**Principle:** Each microservice owns its MySQL schema. No shared schema. No cross-schema foreign keys.

**Benefits:**
- ✅ Independent deploys (no schema migration blocking)
- ✅ Independent scaling (one service's DB can scale separately)
- ✅ No schema coupling deadlocks
- ✅ Technology flexibility (could use PostgreSQL for one service, MySQL for another)

**Tradeoff:**
- ❌ No transactional consistency across services
- ❌ Cross-service relationships by ID only (string matching, no FK enforcement)
- ❌ Eventual consistency (claim exists in `data_ingestion.claim_raw` but may not yet exist in `fraud_risk.risk_scores`)

**Example:**
```
data_ingestion.claim_raw:
  claimId: "CLM-001"
  feedId: 1
  payloadJson: "..."

fraud_risk.risk_scores:
  claimId: "CLM-001"  # Same ID, no FK
  score: 82
  
# These are related only by claimId string matching, not database foreign key
```

### 2. Service Discovery (Eureka + Spring Cloud LoadBalancer)

**How it works:**
1. Each service registers with Eureka on startup (via `@EnableDiscoveryClient`)
2. Gateway and Feign clients resolve service by name
3. Spring Cloud LoadBalancer picks an instance (round-robin)

**Gateway routing:**
```yaml
routes:
  - uri: lb://data-ingestion-service  # lb:// = load balanced
    predicates:
      - Path=/api/feeds/**
```

**Feign client:**
```java
@FeignClient(name = "NotificationService")  # Resolved via Eureka
public interface NotificationServiceClient {
    @PostMapping("/api/notifications/dispatch")
    void dispatchNotification(@RequestBody NotificationDispatchRequestDTO req);
}
```

### 3. Inter-Service Communication Patterns

**Pattern 1: Synchronous Feign (WebMVC services)**
```java
try {
    notificationClient.dispatchNotification(dto);
} catch (FeignException e) {
    log.warn("Failed to notify (non-blocking)", e);
    // Continue; notification failure doesn't block business operation
}
```

**Pattern 2: Reactive WebClient (WebFlux gateway)**
```java
WebClient.builder()
    .baseUrl("lb://NotificationService")
    .build()
    .post()
    .uri("/api/notifications/dispatch")
    .bodyValue(dto)
    .retrieve()
    .toBodilessEntity()
    .block();
```

**Why two patterns?**
- Gateway is reactive (non-blocking I/O for high concurrency)
- Services are servlet-based (simpler for CRUD logic)

### 4. Caching Strategy

**On hot reads:**
```java
@Cacheable(value = "rawClaims", key = "'all'")
public List<IngestionResponseDTO> getAllRawClaims() {
    return repository.findAll().stream()
        .map(mapper::toDto)
        .collect(Collectors.toList());
}
```

**On writes:**
```java
@CacheEvict(value = {"rawClaims", "feeds"}, allEntries = true)
public IngestionResponseDTO ingestClaim(IngestionRequestDTO req) {
    ClaimRaw saved = repository.save(mapper.toEntity(req));
    return mapper.toDto(saved);
}
```

**Cache Type:** In-process (Caffeine), NOT CDN  
**Why:** Data is authenticated and user-scoped; CDN would expose all users' data

### 5. Role-Based Authorization

**Roles:**
- ADMIN - Full system access
- CLAIMS_ANALYST - View/manage claims
- CLAIMS_MANAGER - Manage adjusters, SLAs
- FRAUD_ANALYST - View fraud indicators, risk scores
- ACTUARY - View denial patterns, loss ratios
- OPERATIONS_EXEC - View operational metrics

**Frontend enforcement (React Router):**
```typescript
function RoleRoute({ allowedRoles, children }) {
  const { user } = useAuth();
  if (!allowedRoles.includes(user.role)) {
    return <Navigate to="/dashboard" />;
  }
  return children;
}

// Usage
<RoleRoute allowedRoles={['ADMIN', 'FRAUD_ANALYST']}>
  <FraudPage />
</RoleRoute>
```

**Backend enforcement (Spring Security):**
```java
@PostMapping("/api/users")
public ResponseEntity<?> createUser(@RequestBody UserDTO dto) {
    String role = jwtUtil.extractRole(request);
    if (!role.equals("ADMIN")) {
        throw new ForbiddenException("Only admins can create users");
    }
    return ResponseEntity.created(...).build();
}
```

### 6. Error Handling

**Standard error envelope (8 services):**
```json
{
  "timestamp": "2026-04-28T10:15:30Z",
  "status": 400,
  "error": "InvalidFeedStatusException",
  "message": "Cannot ingest into feed 3 — status is INACTIVE"
}
```

**NotificationService oddity (historical):**
```json
{
  "success": false,
  "message": "User not found",
  "data": null
}
```

**FE normalization:**
```typescript
function getApiErrorMessage(err: AxiosError<any>) {
  const d = err.response?.data;
  if (typeof d === 'string') return d;
  if (typeof d.message === 'string') return d.message;  // Most common
  if (typeof d.error === 'string') return d.error;      // Fallback
  return err.message || 'Something went wrong.';
}
```

---

## V. DATA FLOW EXAMPLES

### Happy Path: Claim Ingestion → Fraud Detection

```
1. FE: RawClaimsPage
   User inputs: claimId="CLM-001", feedId=1, payloadJson={...}
   Clicks "Ingest Claim"

2. FE → HTTP Request
   POST /api/ingest
   Authorization: Bearer <jwt>
   Body: { claimId, feedId, payloadJson }

3. Gateway (port 8086)
   - Extracts JWT → userId, role
   - Route by Path=/api/ingest/** → lb://data-ingestion-service
   - Forwards request

4. Data Ingestion Service (port 8082)
   POST /api/ingest Handler:
   a) Lookup feed by ID
      └─ 404 if not found
   b) Check feed.status == ACTIVE
      ├─ NO: Emit SYSTEM notification to ADMIN role via Feign
      │       throw 400 InvalidFeedStatusException
      └─ YES: Continue
   c) Save ClaimRaw(claimId="CLM-001", ...)
   d) Update feed.lastSyncDate = NOW
   e) @CacheEvict("rawClaims")
   f) Return 201 with saved DTO

5. FE receives 201
   ├─ Show toast "Claim ingested successfully"
   └─ Refresh claims list (cache was evicted)

6. Async: Fraud Risk Processing
   [Note: Currently NOT automatic; fraud-risk doesn't auto-process ingested claims]
   
   When processing happens:
   a) Fraud Risk Service loads claim
   b) Computes risk score for "CLM-001"
   c) Stores in risk_scores table
   d) If score >= 75:
      └─ Feign call to NotificationService:
         POST /api/notifications/dispatch
         targetRoles: ["FRAUD_ANALYST"]
         title: "High fraud risk"
         message: "Claim CLM-001 scored 87"
         category: "RISK"
         referenceId: "CLM-001"

7. NotificationService (port 8088)
   a) Query: SELECT userId FROM users WHERE role='FRAUD_ANALYST' AND isActive=true
   b) For each user, create Notification row
   c) Return 200

8. FE: Notification Bell (30s polling)
   a) Every 30 seconds: GET /notifications/unread-count/{userId}
   b) If count > 0: Show red badge on bell
   c) User clicks bell → dropdown shows latest notifications
   d) Click notification → navigate to claim CLM-001

9. FRAUD_ANALYST User
   ├─ Sees red bell badge
   ├─ Opens notification inbox
   └─ Navigates to claim CLM-001 detail page
```

### Dashboard Load (5 Parallel Calls via Promise.allSettled)

```typescript
useEffect(() => {
  Promise.allSettled([
    ingestApi.getAll(),                    // Raw claims → count, aging
    claimsApi.getByMetric('TAT'),          // Avg turnaround time
    claimsApi.getByMetric('LOSS_RATIO'),   // Loss ratio
    riskScoresApi.getAll(),                // Top fraud signals
    denialPatternsApi.getAll(),            // Top denial reasons
  ]).then((results) => {
    results.forEach((result, index) => {
      if (result.status === 'fulfilled') {
        // Use the data
        setDashboardData(index, result.value);
      } else {
        // Service failed; show partial dashboard
        log.warn(`Dashboard call ${index} failed`, result.reason);
      }
    });
  });
}, []);
```

**Why allSettled?** If fraud-risk-service is down, dashboard still shows claims + denials. (With Promise.all, one failure = entire dashboard fails)

---

## VI. KNOWN ISSUES & FIXES

### Issue 1: Mojibake UTF-8 in Denial Reasons

**Problem:** Denial reasons displayed as garbled characters (e.g., `\xE2\x80\x93` rendered as separate Latin-1 chars)

**Root Cause:** During database seed, MySQL connection charset was set to DOS code page (e.g., `cp850`) instead of UTF-8. UTF-8 bytes stored as separate Latin-1 characters.

**Fix:**
```sql
-- Convert column back to binary, then reinterpret as UTF-8
ALTER TABLE denial_patterns MODIFY COLUMN reason BINARY(255);
ALTER TABLE denial_patterns MODIFY COLUMN reason VARCHAR(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Replace known mojibake patterns
UPDATE denial_patterns SET reason = REPLACE(reason, '\xE2\x80\x93', '–') 
WHERE reason LIKE '%\xE2%';
```

**Prevention:**
```properties
# application.properties
spring.datasource.url=jdbc:mysql://localhost:3306/db?characterEncoding=utf8mb4&serverTimezone=UTC
```

### Issue 2: NotificationService User Sync Missing

**Problem:** Newly registered users were invisible to role-based notification dispatch

**Root Cause:** `POST /api/notifications/users/sync` endpoint didn't exist. Gateway called it after user registration, but got 404. Error was non-blocking, so request succeeded but user wasn't mirrored into NotificationService.

**Fix:**
```java
@PostMapping("/api/notifications/users/sync")
public ResponseEntity<?> syncUser(@RequestBody UserSyncRequestDTO req) {
    // Idempotent: INSERT ... ON DUPLICATE KEY UPDATE
    User existing = repository.findById(req.getUserId()).orElse(null);
    
    if (existing != null) {
        existing.setRole(req.getRole());
        repository.save(existing);
    } else {
        User newUser = new User();
        newUser.setUserId(req.getUserId());
        newUser.setRole(req.getRole());
        newUser.setIsActive(true);
        repository.save(newUser);
    }
    return ResponseEntity.ok("User synced").build();
}
```

### Issue 3: Two Different Error Envelopes

**Problem:** NotificationService uses `{success, message, data}`, but 8 services use `{timestamp, status, error, message}`

**Current Workaround:** FE normalizes both via `getApiErrorMessage()`

**Better Solution:** Standardize on one envelope from day one

---

## VII. SECURITY GAPS & RECOMMENDATIONS

### Gap 1: JWT Secret in Plain Text

**Location:** `api-gateway/src/main/resources/application.yml`

```yaml
application:
  security:
    jwt:
      secret-key: 5367566B59703373367639792F423F4528482B4D6251655468576D5A71347437  # EXPOSED
```

**Risk:** Secret exposed in version control; any developer can forge tokens

**Fix:**
```yaml
application:
  security:
    jwt:
      secret-key: ${JWT_SECRET_KEY}  # Read from environment
```

**Set via:**
```bash
export JWT_SECRET_KEY="your-secure-key-here"
# Or in .env for dev
```

### Gap 2: No Per-User Authorization on Endpoints

**Example (VULNERABLE):**
```java
@GetMapping("/api/notifications/user/{userId}")
public List<Notification> getNotifications(@PathVariable Long userId) {
    return service.getNotifications(userId);  // Any user can request ANY userId
}
```

**Attack:** Authenticated user with role ANALYST requests `/api/notifications/user/999` → sees admin's notifications

**Fix:**
```java
@GetMapping("/api/notifications/user/{userId}")
public List<Notification> getNotifications(
    @PathVariable Long userId,
    HttpServletRequest request) {
    
    Long tokenUserId = jwtUtil.extractUserId(request);
    if (!tokenUserId.equals(userId)) {
        throw new ForbiddenException("Cannot access other users' notifications");
    }
    return service.getNotifications(userId);
}
```

### Gap 3: CORS Too Permissive

**Current:**
```yaml
globalcors:
  cors-configurations:
    '[/**]':
      allowedOrigins: "*"  # Any domain can make requests
```

**Risk:** Vulnerable to CSRF attacks from malicious sites

**Fix:**
```yaml
globalcors:
  cors-configurations:
    '[/**]':
      allowedOrigins:
        - "https://app.claiminsight360.com"
        - "https://staging.claiminsight360.com"
      allowedMethods: ["GET", "POST", "PUT", "DELETE", "OPTIONS"]
      allowedHeaders: ["Authorization", "Content-Type"]
      allowCredentials: true
```

### Gap 4: No Audit Logs for Sensitive Operations

**Missing:**
- Who changed user roles?
- Who deleted claims?
- Who exported reports?

**Fix:**
```java
@PostMapping("/api/users/{userId}/role")
public ResponseEntity<?> updateUserRole(
    @PathVariable Long userId,
    @RequestParam String newRole) {
    
    Long actorId = jwtUtil.extractUserId(request);
    String oldRole = userRepository.findById(userId).getRole();
    
    auditService.log(AuditEvent.builder()
        .actor(actorId)
        .action("ROLE_CHANGE")
        .target("User:" + userId)
        .oldValue(oldRole)
        .newValue(newRole)
        .timestamp(Instant.now())
        .build());
    
    userRepository.updateRole(userId, newRole);
    return ResponseEntity.ok().build();
}
```

---

## VIII. PERFORMANCE BOTTLENECKS & SOLUTIONS

| Bottleneck | Current State | Solution | Effort |
|-----------|---|---|---|
| Notification dispatch | Sync Feign (blocking) | Async Kafka topic | 3 days |
| Bell notification | 30s polling | WebSocket/SSE | 2 days |
| Database reads | Single primary | Read replicas | 1 day |
| Cache scalability | In-process memory | Redis shared cache | 1 day |
| Frontend bundle | ~250KB | Code splitting, lazy loading | 1 day |
| Avatar rendering | Regenerated per render | Memoization | 2 hours |

---

## IX. TESTING COVERAGE

**Currently Implemented:**
- ✅ Unit tests (service layer)
- ✅ Integration tests (controller + repository)
- ✅ Test DB: H2 in-memory

**Missing:**
- ❌ Contract tests (FE ↔ BE DTO validation)
- ❌ Full E2E tests (Playwright/Cypress)
- ❌ Load/stress tests (JMeter)
- ❌ Security tests (OWASP top 10)

---

## X. PRODUCTION READINESS CHECKLIST

- [ ] JWT secret in environment variable (not in code)
- [ ] CORS whitelist specific domains (not *)
- [ ] Database credentials in secrets manager
- [ ] Audit logs for all sensitive operations
- [ ] Per-user authorization checks on all endpoints
- [ ] Structured logging (JSON) for log aggregation
- [ ] Prometheus metrics export
- [ ] Circuit breakers for inter-service calls (Resilience4j)
- [ ] Request rate limiting
- [ ] API versioning strategy
- [ ] Database backups automated
- [ ] Rollback strategy for failed deployments
- [ ] Blue-green deployment capability
- [ ] Performance monitoring (APM)
- [ ] Security scanning (SAST/DAST)

---

## XI. IMPORTANT NUMBERS & METRICS

| Metric | Value |
|--------|-------|
| Total backend services | 10 (1 gateway + 9 business) |
| Frontend total LOC | ~2,750 |
| Backend total LOC | ~2,750 |
| User roles | 6 |
| Notification categories | 6 (RISK, DENIAL, COST, PERF, AGING, SYSTEM) |
| JWT expiration | 24 hours (86400000ms) |
| Bell poll interval | 30 seconds |
| Database per service | 8 (MySQL) |
| Service ports | 8082-8090 (range) |
| Eureka port | 8761 |
| API Gateway port | 8086 |
| Frontend port (dev) | 3000 |

---

## XII. KEY ARCHITECTURAL DECISIONS & TRADEOFFS

| Decision | Why | Tradeoff |
|----------|-----|----------|
| WebFlux gateway + WebMVC services | Gateway scales I/O; services simpler CRUD | Two paradigms to maintain |
| Database-per-service | Independent deploys, independent scaling | Eventual consistency, no cross-service transactions |
| JWT issued by gateway only | No per-service auth DB lookup; minimal latency | Token revocation is hard; rely on short expiry |
| Role-based notification dispatch | Eventually consistent role membership | Sync hop on every register; if NotificationService down during register, new user invisible until next sync |
| Standard error envelope | Predictable shape across 8 services | NotificationService oddity; FE must normalize |
| 30s bell polling | Simple, no WebSocket infra needed | Up to 30s latency for new alerts |
| No PDF generation server-side | CSV is enough; keeps BE stateless | Can't export branded PDFs |
| Per-service MySQL | Independent migrations, independent scaling | No cross-schema joins; eventual consistency |

---

## XIII. DEPLOYMENT CHECKLIST

### Prerequisites
- Java 17+ installed
- Maven 3.8+ installed
- MySQL 8.0+ running
- Node 16+ installed
- npm/yarn installed

### Startup Order
```bash
# Step 1: Start Eureka (FIRST!)
cd eureka-server && mvn spring-boot:run

# Step 2: Start API Gateway
cd api-gateway && mvn spring-boot:run

# Step 3: Start all business services (parallel)
cd data-ingestion-service && mvn spring-boot:run &
cd claims-metrics-service && mvn spring-boot:run &
cd fraud-risk-service && mvn spring-boot:run &
cd denial-leakage-service && mvn spring-boot:run &
cd cost-reserve-service && mvn spring-boot:run &
cd AdjusterAndOperations && mvn spring-boot:run &
cd NotificationService && mvn spring-boot:run &
cd analytics-report-service && mvn spring-boot:run &

# Step 4: Start frontend
cd claiminsight360-frontend-v3 && npm run dev

# Step 5: Access Eureka UI
open http://localhost:8761/

# Step 6: Access application
open http://localhost:3000/
```

### Health Checks
```bash
curl http://localhost:8761/              # Eureka UI
curl http://localhost:8086/actuator/health  # Gateway
curl http://localhost:8082/actuator/health  # Data Ingestion
# ... repeat for other services
```

---

## XIV. INTERVIEW Q&A GENERATION GUIDE

**Use this document with Claude to generate:**

### Easy Questions (Warm-up)
- What is ClaimInsight360?
- How many microservices?
- What's the tech stack?
- How does authentication work?
- What's a KPI?
- Explain the database schema for Claims Metrics
- What are the roles in the system?

### Intermediate Questions (Architecture & Design)
- Explain the happy path: claim ingestion → fraud detection
- How does service-to-service communication work?
- Describe the database-per-service pattern
- How is caching implemented?
- Explain role-based authorization
- How does the notification dispatch system work?
- Why two communication patterns (Feign vs WebClient)?

### Hard Questions (Deep dives & Edge cases)
- Walk through the Mojibake UTF-8 bug in detail
- How would you scale fraud-risk-service to handle 10x volume?
- Design a multi-tenant version (one DB per insurance company)
- Implement real-time WebSocket alerts (vs. 30s polling)
- How do you handle eventual consistency across services?
- Design comprehensive E2E tests for the ingest → fraud flow
- What observability would you add for production?
- Explain Feign error handling when NotificationService is down

### Situational Questions (Behavioral)
- Tell me about defending an architectural decision
- How do you approach learning an unfamiliar codebase?
- Describe your most complex technical challenge
- How do you prioritize technical debt?
- What would you change if you started over?

---

## XV. QUICK REFERENCE COMMANDS

```bash
# Start all services
./START_ALL_SERVICES.ps1  # Or on macOS/Linux: bash START_ALL_SERVICES.sh

# Stop all services
./STOP_ALL_SERVICES.ps1

# Check specific service health
curl http://localhost:8086/actuator/health

# View logs
tail -f eureka-server/logs/boot.out.log
tail -f api-gateway/logs/boot.out.log

# Build everything
mvn clean install

# Build frontend
cd claiminsight360-frontend-v3 && npm run build

# Run frontend dev server
cd claiminsight360-frontend-v3 && npm run dev
```

---

## XVI. DIRECTORY STRUCTURE

```
C:\Users\2478140\Downloads\cali\
├── pom.xml (Maven aggregator)
├── eureka-server/
├── api-gateway/
├── data-ingestion-service/
├── claims-metrics-service/
├── fraud-risk-service/
├── denial-leakage-service/
├── cost-reserve-service/
├── AdjusterAndOperations/
├── NotificationService/
├── analytics-report-service/
├── claiminsight360-frontend-v3/
├── DESIGN_SPEC.md
├── INTERVIEW_GUIDE.md
├── PROMPT.md
├── CLAUDE_PROJECT_CONTEXT.md (THIS FILE)
└── ... other files ...
```

---

## XVII. NEXT STEPS FOR CLAUDE

**Copy this entire document and give to Claude with this prompt:**

```
I have a comprehensive project context file for ClaimInsight360, an enterprise insurance 
claims analytics platform with 10 microservices (Spring Boot) and a React frontend.

Using this complete context, please generate:

1. 50+ detailed interview questions covering EASY → INTERMEDIATE → HARD levels
2. For each question:
   - Provide detailed answer with code examples
   - Explain why this matters from interviewer perspective
   - List common mistakes candidates make
   - Suggest follow-up questions

Cover these areas:
- Microservices Architecture
- Spring Boot & Java Backend
- React & TypeScript Frontend
- Database Design & Per-Service Pattern
- Authentication & Security
- Service Discovery & Communication
- Caching & Performance
- Error Handling & Resilience
- Data Flows & Complex Scenarios
- Production Readiness
- Known Issues & Debugging
```

---

**END OF COMPREHENSIVE PROJECT CONTEXT FILE**

This file contains every technical detail needed to generate interview questions covering 
the entire ClaimInsight360 system from easy to hard difficulty levels.


