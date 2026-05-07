# API Gateway - Quick Reference & Visual Summary

## One-Liner Summary
**The API Gateway (port 8086) is the single authentication & authorization gatekeeper that validates JWTs, enforces role-based access, and routes all requests to 8 downstream microservices while auditing every meaningful action.**

---

## Request Processing Pipeline (Visual)

```
INCOMING REQUEST
    ↓
    └─→ [JwtSecurityContextRepository.load()]
        └─→ Is there a Bearer token or ?token param?
            ├─ YES: extractToken()
            │    └─→ [JwtAuthManager.authenticate()]
            │        └─→ [JwtService.isTokenValid()]
            │            ├─ Verify HMAC-SHA256 signature
            │            ├─ Check expiry (exp > now)
            │            └─ Extract username & role from claims
            │        └─→ Return UsernamePasswordAuthenticationToken
            │             (principal=username, authorities=[role])
            │
            └─ NO: Return empty Mono (no auth context)
    ↓
    └─→ [SecurityConfig.authorizeExchange()]
        └─→ Is endpoint protected? 
            ├─ /api/auth/** → PERMIT_ALL (public)
            ├─ /api/users/** → requires ROLE_ADMIN only
            ├─ /api/risk-scores/** → requires ROLE_FRAUD_ANALYST or ROLE_ADMIN
            ├─ ... (other role checks)
            └─ .anyExchange() → AUTHENTICATED (any valid JWT)
        
        └─→ Authorization check:
            ├─ User has required role?
            │  ├─ YES → continue ✓
            │  └─ NO → 403 Forbidden ✗
            └─ No auth context & endpoint protected?
               ├─ YES → 401 Unauthorized ✗
               └─ NO → continue (public endpoint) ✓
    ↓
    └─→ [AuthHeaderForwardFilter.filter()]
        └─→ If authenticated:
            ├─ Read username from SecurityContext
            ├─ Read role from SecurityContext
            └─ Mutate request, add headers:
               ├─ X-Auth-Username: john.doe
               └─ X-Auth-Role: ROLE_FRAUD_ANALYST
        └─→ Forward to next filter
    ↓
    └─→ [AuditFilter.filter()]
        └─→ Is this action loggable?
            ├─ POST /api/ingest → "CREATE Claim" ✓
            ├─ GET /api/risk-scores → "VIEW Risk Scores" (skip if not sensitive) ✗
            ├─ PUT /api/users/5 → "UPDATE User" ✓
            └─ DELETE /api/feeds/3 → "DELETE Feed" ✓
        
        └─→ If loggable, extract metadata:
            ├─ username (from SecurityContext)
            ├─ action (inferred from method + path)
            ├─ resource (the full path)
            ├─ method (POST, GET, etc.)
            ├─ status (response code)
            ├─ ip (from X-Forwarded-For or remote address)
            └─ user-agent
        
        └─→ Log asynchronously:
            ├─ Check 500ms fast-dedup cache (skip if duplicate)
            ├─ For non-mutations, check 30-min upsert window:
            │  ├─ If same (user, action, resource) found → UPDATE timestamp
            │  └─ Else → INSERT new row
            └─ For mutations, always INSERT fresh row
    ↓
    └─→ [Gateway Routing]
        └─→ Match path to downstream service via load balancer:
            ├─ /api/ingest/** → lb://data-ingestion-service:8082
            ├─ /api/risk-scores/** → lb://fraud-risk-service:8090
            ├─ /api/kpis/** → lb://claims-metrics-service:8083
            └─ ... etc
    ↓
    └─→ DOWNSTREAM SERVICE
        └─→ Reads X-Auth-Username and X-Auth-Role headers
        └─→ Executes business logic
        └─→ Returns response
    ↓
    └─→ Gateway forwards response to client
    ↓
RESPONSE SENT TO CLIENT
```

---

## Authentication Flow: Login → Token Refresh → Logout

```
STEP 1: LOGIN
┌──────────────────────────────────────────────┐
│ POST /api/auth/login                         │
│ {                                            │
│   "username": "john.doe",                    │
│   "password": "SecurePass123"                │
│ }                                            │
└──────────────────────────────────────────────┘
        ↓
    [AuthService.login()]
        ├─ LoginAttemptService.checkLocked("john.doe")
        │  └─ Is account locked? NO ✓
        │
        ├─ UserRepository.findByUsernameOrEmail("john.doe")
        │  └─ Found: User(enabled=true, role=ROLE_FRAUD_ANALYST) ✓
        │
        ├─ PasswordEncoder.matches(inputPassword, storedHash)
        │  └─ Correct? YES ✓
        │
        ├─ LoginAttemptService.recordSuccess("john.doe")
        │  └─ Reset fail counter to 0
        │
        ├─ JwtService.generateToken("john.doe", "ROLE_FRAUD_ANALYST", userId=1)
        │  └─ Create JWT:
        │     Header: {alg: HS256, typ: JWT}
        │     Payload: {sub: john.doe, role: ROLE_FRAUD_ANALYST, userId: 1, iat: now, exp: now+24h}
        │     Signature: HMAC-SHA256(header.payload, secret)
        │     Result: eyJhbGc...
        │
        ├─ issueRefreshToken("john.doe")
        │  ├─ Revoke all old refresh tokens
        │  ├─ Generate UUID: a1b2c3d4-...
        │  └─ Save to DB: RefreshToken(token=a1b2c3d4-..., expiresAt=now+7d, revoked=false)
        │
        └─ Return:
           {
             "accessToken": "eyJhbGc...",
             "refreshToken": "a1b2c3d4-...",
             "tokenType": "Bearer",
             "expiresIn": 86400,
             "username": "john.doe",
             "role": "ROLE_FRAUD_ANALYST"
           }

STEP 2: USE ACCESS TOKEN (1-24 hours)
┌──────────────────────────────────────────────┐
│ GET /api/risk-scores                         │
│ Authorization: Bearer eyJhbGc...             │
└──────────────────────────────────────────────┘
        ↓
    [JwtSecurityContextRepository.load()]
        ├─ Extract "eyJhbGc..." from Authorization header
        └─ [JwtAuthManager.authenticate()]
           ├─ [JwtService.isTokenValid()]
           │  ├─ Verify signature: HMAC-SHA256(header.payload, secret) == signature ✓
           │  ├─ Check expiry: exp (now+24h) > current time ✓
           │  └─ Return true
           │
           ├─ Extract username: "john.doe"
           ├─ Extract role: "ROLE_FRAUD_ANALYST"
           └─ Return Authentication(principal=john.doe, role=ROLE_FRAUD_ANALYST)
    ↓
    [SecurityConfig.authorizeExchange()]
        ├─ Path /api/risk-scores requires: ROLE_FRAUD_ANALYST or ROLE_ADMIN
        └─ User has ROLE_FRAUD_ANALYST? YES ✓ → Continue
    ↓
    [AuthHeaderForwardFilter]
        └─ Add X-Auth-Username: john.doe
        └─ Add X-Auth-Role: ROLE_FRAUD_ANALYST
    ↓
    [AuditFilter]
        └─ GET request + non-sensitive read → SKIP logging
    ↓
    [Route to fraud-risk-service]
        └─ GET /api/risk-scores with X-Auth-* headers
        └─ Returns [{ id: 1, score: 87 }, ...]
    ↓
    HTTP 200 OK
    Response sent to client

STEP 3: TOKEN EXPIRES (after 24 hours)
┌──────────────────────────────────────────────┐
│ GET /api/risk-scores                         │
│ Authorization: Bearer eyJhbGc... (EXPIRED)   │
└──────────────────────────────────────────────┘
        ↓
    [JwtSecurityContextRepository.load()]
        └─ [JwtAuthManager.authenticate()]
           └─ [JwtService.isTokenValid()]
              ├─ Verify signature ✓
              ├─ Check expiry: exp < current time ✗
              └─ Return false
        ↓
    [SecurityConfig.authorizeExchange()]
        └─ No auth context + endpoint requires auth
        └─ 401 Unauthorized
    ↓
    Frontend receives 401:
        ├─ Save currentRequest
        └─ POST /api/auth/refresh
           {
             "refreshToken": "a1b2c3d4-..."
           }

STEP 4: REFRESH TOKEN
┌──────────────────────────────────────────────┐
│ POST /api/auth/refresh                       │
│ {                                            │
│   "refreshToken": "a1b2c3d4-..."            │
│ }                                            │
└──────────────────────────────────────────────┘
        ↓
    [AuthService.refresh()]
        ├─ Lookup RefreshToken row by token="a1b2c3d4-..."
        │  └─ Found ✓
        │
        ├─ Is it revoked?
        │  └─ NO ✓
        │
        ├─ Is it expired (expiresAt < now)?
        │  └─ NO ✓
        │
        ├─ ROTATION: Revoke old token
        │  └─ UPDATE refresh_tokens SET revoked=true WHERE token="a1b2c3d4-..."
        │
        ├─ Issue new accessToken (24h from now)
        │  └─ JwtService.generateToken(...) → eyJhbGc...NEW
        │
        ├─ Issue new refreshToken (7d from now)
        │  └─ Generate UUID: x9y8z7w6-...
        │  └─ Save to DB: RefreshToken(token=x9y8z7w6-..., expiresAt=now+7d, revoked=false)
        │
        └─ Return:
           {
             "accessToken": "eyJhbGc...NEW",
             "refreshToken": "x9y8z7w6-...",
             "tokenType": "Bearer",
             "expiresIn": 86400,
             "username": "john.doe",
             "role": "ROLE_FRAUD_ANALYST"
           }
    ↓
    Frontend:
        ├─ Update localStorage with new tokens
        ├─ Update Redux auth state
        └─ Retry original request with new accessToken
    ↓
    HTTP 200 OK (original request now succeeds)

STEP 5: LOGOUT
┌──────────────────────────────────────────────┐
│ POST /api/auth/logout                        │
│ Authorization: Bearer eyJhbGc...             │
└──────────────────────────────────────────────┘
        ↓
    [AuthService.logout()]
        └─ RefreshTokenRepository.revokeAllByUsername("john.doe")
           └─ UPDATE refresh_tokens SET revoked=true WHERE username="john.doe"
    ↓
    AuditService.log("john.doe", ..., "LOGOUT", ...)
        └─ INSERT into audit_logs
    ↓
    HTTP 200 OK {message: "Logged out successfully"}
    ↓
    Frontend:
        ├─ Clear localStorage (accessToken, refreshToken)
        ├─ Clear Redux auth
        └─ Redirect to /login
    ↓
    If user tries to use old tokens:
        ├─ accessToken: exp < now → invalid JWT → 401
        └─ refreshToken: if somehow used → revoked=true → "Refresh token has been revoked"
```

---

## Role-Based Access Control (RBAC) Quick Reference

```
PUBLIC (No JWT required)
├─ POST /api/auth/register
├─ POST /api/auth/login
├─ GET /actuator/health
└─ GET /actuator/info

CLAIMS_ANALYST (Data analysts)
├─ GET /api/kpis/**
├─ GET /api/feeds/**, POST /api/feeds/**
├─ GET /api/ingest/**, POST /api/ingest/**
└─ GET /api/denial-patterns/**, POST /api/denial-patterns/**

CLAIMS_MANAGER (Operations managers)
├─ GET /api/kpis/**
├─ GET /api/feeds/**, /api/ingest/** (read-only)
├─ GET /api/adjusters/**, POST /api/adjusters/**
├─ GET /api/sla-violations/**
├─ GET /api/investigations/** (read-only)
└─ GET /api/reports/**

FRAUD_ANALYST (Risk specialists)
├─ GET /api/risk-scores/**, POST /api/risk-scores/**
├─ GET /api/risk-indicators/**
├─ POST /api/investigations/**
└─ GET /api/denial-patterns/**, POST /api/denial-patterns/**

ACTUARY (Pricing & reserving)
├─ GET /api/kpis/**
├─ GET /api/costs/**, POST /api/costs/**
├─ GET /api/reserves/**, POST /api/reserves/**
├─ GET /api/aging/**, POST /api/aging/**
└─ GET /api/reports/**

OPERATIONS_EXEC (Compliance & SLA)
├─ GET /api/kpis/**
├─ GET /api/adjusters/**, POST /api/adjusters/**
├─ GET /api/sla-violations/**
└─ GET /api/reports/**

ADMIN (Full access)
├─ ALL endpoints
├─ GET /api/users/**, POST /api/users/**, PUT /api/users/**, DELETE /api/users/**
└─ GET /api/audit/**, DELETE /api/audit/**

AUTHENTICATED (Any valid JWT with any role)
├─ GET /api/reports/**
└─ GET /api/notifications/**
```

---

## Database Tables (in api-gateway MySQL)

### gateway_users
```sql
-- Stores user accounts
id             BIGINT PRIMARY KEY AUTO_INCREMENT
username       VARCHAR(50) UNIQUE NOT NULL    -- login username
name           VARCHAR(100)                   -- full display name
email          VARCHAR(100) UNIQUE NOT NULL   -- email address
phone          VARCHAR(20)                    -- contact number
password       VARCHAR(255) NOT NULL          -- BCrypt hash (never plain text!)
role           ENUM(ROLE_*)                   -- ROLE_ADMIN, ROLE_FRAUD_ANALYST, etc.
enabled        BOOLEAN DEFAULT true           -- can disable without deleting
created_at     TIMESTAMP                      -- when account created
updated_at     TIMESTAMP                      -- when account last modified
```

### refresh_tokens
```sql
-- Stores opaque refresh tokens for session rotation
id             BIGINT PRIMARY KEY AUTO_INCREMENT
token          VARCHAR(255) UNIQUE NOT NULL   -- UUID string (non-JWT)
username       VARCHAR(50) NOT NULL           -- which user owns this token
expires_at     TIMESTAMP NOT NULL             -- when token expires (7 days from creation)
revoked        BOOLEAN DEFAULT false          -- marked invalid on use (rotation)
created_at     TIMESTAMP                      -- when token was issued
```

### audit_logs
```sql
-- Logs all meaningful actions (mutations, sensitive reads, auth events)
id             BIGINT PRIMARY KEY AUTO_INCREMENT
username       VARCHAR(50) NOT NULL           -- who did it
user_id        BIGINT                         -- optional user_id reference
action         VARCHAR(100) NOT NULL          -- "LOGIN", "CREATE Risk Score", "UPDATE User", etc.
resource       VARCHAR(255) NOT NULL          -- "/api/auth/login", "/api/risk-scores", etc.
timestamp      TIMESTAMP DEFAULT NOW()        -- when action occurred
metadata       JSON                           -- {"method":"POST","status":201,"ip":"..."}
INDEX idx_username (username)
INDEX idx_action (action)
INDEX idx_timestamp (timestamp)
```

---

## Login Brute-Force Protection

```
LOGIN ATTEMPTS TRACKING (in-memory ConcurrentHashMap)

User attempts:
  1st fail   → counter = 1
  2nd fail   → counter = 2
  3rd fail   → counter = 3
  4th fail   → counter = 4
  5th fail   → counter = 5
             → LOCKED until (now + 15 minutes)
  
  6th attempt (within 15 min)
             → AccountLockedException
             → "Too many failed attempts. Account locked for 15 more minute(s)."
             → HTTP 429 Too Many Requests
  
  At 15m + 1s → counter cleared, can try again

LOGIC:
- checkLocked(identifier)
  └─ Before password check
  └─ If locked: throw AccountLockedException
  
- recordFailure(identifier)
  └─ Increment counter
  └─ If counter >= 5: set lockUntil = now + 15min
  
- recordSuccess(identifier)
  └─ Remove entry from cache
  └─ Reset to 0 next login

CASE-INSENSITIVE:
- "John.Doe", "john.doe", "JOHN.DOE" are treated as same identifier
- Prevents attacker from trying "John" then "john" to get 10 attempts

LIMITATIONS:
- In-memory only (lost on restart, not shared across instances)
- PRODUCTION: Use Redis for persistence
```

---

## Common Error Responses

### 400 Bad Request (Validation)
```json
{
  "timestamp": "2026-05-07T14:30:00",
  "status": 400,
  "error": "Bad Request",
  "message": "username: must not be blank; password: must be at least 8 characters"
}
```

### 401 Unauthorized (No JWT or Invalid JWT)
```json
{
  "timestamp": "2026-05-07T14:30:00",
  "status": 401,
  "error": "Unauthorized",
  "message": "Invalid or expired JWT token"
}
```

### 403 Forbidden (Insufficient Role)
```json
{
  "timestamp": "2026-05-07T14:30:00",
  "status": 403,
  "error": "Forbidden",
  "message": "Access denied"
}
```

### 409 Conflict (Username/Email Taken)
```json
{
  "timestamp": "2026-05-07T14:30:00",
  "status": 409,
  "error": "Conflict",
  "message": "This username or email is already registered."
}
```

### 429 Too Many Requests (Account Locked)
```json
{
  "timestamp": "2026-05-07T14:30:00",
  "status": 429,
  "error": "Too Many Requests",
  "message": "Too many failed attempts. Account locked for 15 more minute(s)."
}
```

---

## Technology Stack

| Component | Technology | Version |
|---|---|---|
| Framework | Spring Boot | 3.4.5 |
| Gateway | Spring Cloud Gateway | 2024.0.1 |
| Reactive Runtime | Project Reactor | (from Spring Cloud) |
| Web Server | Netty | (via WebFlux) |
| Security | Spring Security | 6.x (reactive) |
| JWT Library | JJWT | 0.12.6 |
| Database | MySQL | 8.0 |
| ORM | Spring Data JPA + Hibernate | 6.x |
| Service Registry | Netflix Eureka Client | (Spring Cloud) |
| Password Encoding | BCrypt | (Spring Security) |
| Java | Java 21 | (records, sealed classes, virtual threads) |

---

## Deployment Topology

```
┌─────────────────────────────────────────────┐
│        Eureka Service Registry              │
│        (localhost:8761)                      │
│                                              │
│  Registered services:                        │
│  - api-gateway (8086)                        │
│  - data-ingestion-service (8082)            │
│  - claims-metrics-service (8083)            │
│  - analytics-report-service (8084)          │
│  - denial-leakage-service (8085)            │
│  - api-gateway (8086)                       │
│  - AdjusterAndOperations (8087)             │
│  - NotificationService (8088)               │
│  - cost-reserve-service (8089)              │
│  - fraud-risk-service (8090)                │
└─────────────────────────────────────────────┘

┌─────────────────────────────────────────────┐
│   API Gateway                               │
│   localhost:8086                            │
│                                              │
│   [Embedded Identity DB]                    │
│   - gateway_users                           │
│   - refresh_tokens                          │
│   - audit_logs                              │
└─────────────────────────────────────────────┘
         ↓ (routes via load balancer)
    ┌─────────────┬────────────┬──────────────┐
    ↓             ↓            ↓              ↓
data-ingestion claims-metrics fraud-risk  cost-reserve
(8082)         (8083)         (8090)       (8089)
```

---

## Key Formulas & Constants

```java
// JWT Expiration
ACCESS_TOKEN_EXPIRY = 86400000 ms = 24 hours
REFRESH_TOKEN_EXPIRY = 604800000 ms = 7 days

// Brute Force
MAX_LOGIN_ATTEMPTS = 5
LOCKOUT_DURATION = 15 * 60 * 1000 ms = 15 minutes

// Audit Deduplication
FAST_DEDUP_WINDOW = 500 ms          // Catches React StrictMode double-fires
UPSERT_WINDOW = 30 minutes          // Collapse noisy reads

// Password Hashing
BCRYPT_STRENGTH = 10 rounds         // Default, secure for 2024

// JWT Signature
ALGORITHM = HMAC-SHA256
SECRET_LENGTH = 256 bits            // Base64 encoded in config
```

---

## Testing Checklist

```
☐ Register new user (POST /api/auth/register)
  ├─ Username taken → 409 Conflict
  ├─ Email taken → 409 Conflict
  ├─ Success → 201 Created, returns accessToken + refreshToken
  └─ New user synced to NotificationService

☐ Login (POST /api/auth/login)
  ├─ Invalid credentials → 401 Unauthorized
  ├─ Account disabled → 401 with reason
  ├─ Brute force (5 fails) → 429 Too Many Requests
  ├─ Success → 200 OK, returns tokens
  └─ Audit log created

☐ Access protected endpoint (GET /api/risk-scores with JWT)
  ├─ Missing JWT → 401
  ├─ Expired JWT → 401
  ├─ Invalid signature → 401
  ├─ Valid JWT, insufficient role → 403
  ├─ Valid JWT, sufficient role → 200 OK
  └─ X-Auth-Username and X-Auth-Role headers added

☐ Token refresh (POST /api/auth/refresh)
  ├─ Invalid refresh token → 401
  ├─ Revoked refresh token → 401
  ├─ Expired refresh token → 401
  ├─ Success → 200 OK, returns new tokens
  └─ Old refresh token is revoked (rotation)

☐ Logout (POST /api/auth/logout)
  ├─ Revokes all refresh tokens
  ├─ Audit log created
  └─ Can't refresh anymore (revoked=true)

☐ Admin user management (PUT /api/users/5)
  ├─ Non-admin → 403
  ├─ Admin can update role
  ├─ Admin can disable user (revokes refresh tokens)
  ├─ Can't delete own account
  └─ Changes audited

☐ Audit log querying (GET /api/audit/logs)
  ├─ Non-admin → 403
  ├─ Admin can list all
  ├─ Admin can filter by username, action, date range
  └─ Mutations have separate rows (not upserted)
```

---

## Production Considerations

```
1. JWT SECRET KEY
   ├─ CURRENT: hardcoded in application.yml (DEV ONLY)
   └─ PRODUCTION: Environment variable or secrets manager (AWS Secrets, HashiCorp Vault)

2. LOGIN BRUTE-FORCE
   ├─ CURRENT: in-memory (lost on restart, not shared across instances)
   └─ PRODUCTION: Move to Redis for persistence & clustering

3. ACCESS TOKEN EXPIRY
   ├─ CURRENT: 24 hours (long)
   └─ PRODUCTION: Reduce to 15 minutes for tighter security
                  (users refresh more often, but attacker window is smaller)

4. HTTPS/TLS
   ├─ All traffic must be encrypted in transit
   ├─ JWT in Bearer header is vulnerable to interception without HTTPS
   └─ Certificate management via Let's Encrypt or corporate CA

5. CORS
   ├─ CURRENT: allowedOrigins: "*" (completely open)
   └─ PRODUCTION: whitelist specific origins (e.g., ["https://claims.company.com"])

6. RATE LIMITING
   ├─ Add per-user request limits (X requests per minute)
   └─ Prevent resource exhaustion attacks

7. AUDIT LOG RETENTION
   ├─ Define retention policy (e.g., keep 6 months, archive older)
   ├─ Implement automated purge or archival
   └─ Ensure compliance with regulations (HIPAA, PCI-DSS)

8. JWT BLACKLIST for revocation
   ├─ Current: Access tokens can't be revoked (by design)
   ├─ If immediate revocation needed (e.g., compromise):
   │  ├─ Add Redis cache of revoked JTIs (JWT ID claim)
   │  ├─ On every request, check: is this token blacklisted?
   │  └─ Trade-off: Extra latency for faster revocation
   └─ Refresh token rotation already handles this case

9. MULTI-INSTANCE DEPLOYMENT
   ├─ SessionID cookies won't work (different instances)
   ├─ JWT is solution (stateless, shared secret)
   ├─ All instances must use SAME JWT secret key
   └─ Use centralized config (e.g., environment vars, ConfigServer)

10. MONITORING & ALERTING
    ├─ Alert on high 401/403 rates (possible attack)
    ├─ Alert on audit log deletes by admins
    ├─ Monitor JWT signature verification failures
    └─ Track login attempt spikes
```


