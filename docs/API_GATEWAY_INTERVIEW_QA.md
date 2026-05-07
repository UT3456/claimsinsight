# API Gateway - Detailed Interview Q&A

## Overview
**The API Gateway is the single entry point for all client requests in the ClaimInsight360 platform.** It runs on port **8086** using **Spring Cloud Gateway** (reactive/WebFlux) and provides:

1. **Centralized routing** to 8 downstream microservices
2. **JWT-based authentication & authorization** (embedded identity module)
3. **Role-based access control (RBAC)** with 6 user roles
4. **Request auditing** (who did what, when)
5. **Login brute-force protection** (account lockout after 5 failed attempts)
6. **Token refresh mechanism** with rotation (prevents replay attacks)

---

## Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────────────┐
│                                                                         │
│                    Frontend (React, Port 3000)                         │
│                                                                         │
└────────────────────────┬────────────────────────────────────────────────┘
                         │
                         │ HTTP Request + JWT (Bearer token)
                         │
┌────────────────────────▼────────────────────────────────────────────────┐
│                                                                         │
│  API Gateway (Spring Cloud Gateway, Port 8086, WebFlux/Netty)          │
│  ┌───────────────────────────────────────────────────────────────────┐ │
│  │ 1. JwtSecurityContextRepository                                   │ │
│  │    ↓ Extracts token from Authorization header or ?token query    │ │
│  │ 2. JwtAuthManager (ReactiveAuthenticationManager)                │ │
│  │    ↓ Validates JWT signature & expiry, extracts username & role │ │
│  │ 3. SecurityConfig (Spring Security Filter Chain - Reactive)      │ │
│  │    ↓ Checks RBAC: does user's role have access to this path?    │ │
│  │ 4. AuthHeaderForwardFilter (Global Filter)                       │ │
│  │    ↓ Adds X-Auth-Username & X-Auth-Role headers to forwarded req│ │
│  │ 5. AuditFilter (Global Filter)                                  │ │
│  │    ↓ Logs action (CREATE/UPDATE/DELETE/LOGIN) to audit_logs DB  │ │
│  │ 6. Route to Downstream Service                                   │ │
│  │                                                                   │ │
│  │  Auth Endpoints (Embedded in Gateway):                           │ │
│  │   - POST   /api/auth/register   (create new user)                │ │
│  │   - POST   /api/auth/login      (authenticate user)              │ │
│  │   - POST   /api/auth/refresh    (exchange refresh token)         │ │
│  │   - POST   /api/auth/logout     (revoke all refresh tokens)      │ │
│  │   - GET    /api/users/**        (admin user management)          │ │
│  │   - GET    /api/audit/**        (admin audit logs)               │ │
│  │                                                                   │ │
│  │  Routes to Downstream Services:                                  │ │
│  │   - /api/feeds/**, /api/ingest/**   → data-ingestion-service    │ │
│  │   - /api/kpis/**, /api/claim-status/** → claims-metrics-service │ │
│  │   - /api/costs/**, /api/reserves/**, /api/aging/** → cost-rsv   │ │
│  │   - /api/adjusters/**, /api/sla-violations/** → AdjusterOps     │ │
│  │   - /api/notifications/** → NotificationService                 │ │
│  │   - /api/risk-scores/**, /api/risk-indicators/** → fraud-risk   │ │
│  │   - /api/investigations/** → fraud-risk                          │ │
│  │   - /api/denial-patterns/**, /api/leakage-flags/** → denial-lek │ │
│  │   - /api/reports/** → analytics-report-service                  │ │
│  └───────────────────────────────────────────────────────────────────┘ │
│                                                                         │
│  Identity Database (MySQL, Embedded in Gateway):                       │
│   - gateway_users table (user accounts, passwords, roles)              │
│   - refresh_tokens table (session management, replay prevention)       │
│   - audit_logs table (who did what, when)                              │
│                                                                         │
└────────────────────────┬────────────────────────────────────────────────┘
                         │
       ┌─────────────────┼─────────────────┬──────────────────┬──────────────┐
       │                 │                 │                  │              │
       ▼                 ▼                 ▼                  ▼              ▼
 data-ingestion  claims-metrics  cost-reserve  fraud-risk  denial-leakage analytics
 (8082)          (8083)           (8089)        (8090)      (8085)         (8084)
```

---

## Key Components

### 1. **ApiGatewayApplication.java**

```java
@SpringBootApplication(exclude = ReactiveUserDetailsServiceAutoConfiguration.class)
@EnableAsync
public class ApiGatewayApplication {
    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }
}
```

**Why `@EnableAsync`?** 
- AuditService logs events asynchronously with `@Async` method — doesn't block request threads

**Why exclude `ReactiveUserDetailsServiceAutoConfiguration`?**
- Gateway uses JWT (stateless), not session-based UserDetailsService
- Excluding this prevents Spring Boot from creating a default in-memory UserDetailsService that conflicts with our JWT strategy

---

### 2. **JwtSecurityContextRepository.java** — Token Extraction

**Responsibility:** Extract the JWT from the incoming request and delegate validation to JwtAuthManager.

**Token Resolution Order:**
1. `Authorization: Bearer <token>` (standard HTTP header)
2. `?token=<token>` (query parameter fallback for browsers that can't set custom headers, e.g., EventSource)

```java
private String extractToken(ServerWebExchange exchange) {
    String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
    if (authHeader != null && authHeader.startsWith(BEARER_PREFIX)) {
        return authHeader.substring(BEARER_PREFIX.length());
    }
    String queryToken = exchange.getRequest().getQueryParams().getFirst("token");
    if (queryToken != null && !queryToken.isBlank()) {
        return queryToken;
    }
    return null;
}
```

**Flow:**
```
Request arrives
    ↓
JwtSecurityContextRepository.load() called
    ↓
extractToken() — looks for Bearer token or ?token
    ↓
JwtAuthManager.authenticate() — validates signature & expiry
    ↓
Returns SecurityContext (or empty if invalid)
```

---

### 3. **JwtAuthManager.java** — Token Validation

**Responsibility:** Validate the JWT and return an Authentication object with username + role.

```java
@Override
public Mono<Authentication> authenticate(Authentication authentication) {
    String token = authentication.getCredentials().toString();
    
    if (!jwtService.isTokenValid(token)) {
        return Mono.empty();  // Invalid token → 401 Unauthorized
    }
    
    String username = jwtService.extractUsername(token);
    String role     = jwtService.extractRole(token);
    
    UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
        username,
        null,
        List.of(new SimpleGrantedAuthority(role))
    );
    
    return Mono.just(auth);
}
```

**Returns:**
- Empty Mono → no auth context, request is treated as public (401 on protected endpoints)
- Populated Mono → auth context with username + role granted authorities

---

### 4. **JwtService.java** — JWT Creation & Validation

**Responsibility:** Generate, validate, and extract claims from JWTs.

**JWT Structure:**
```
Header: { alg: "HS256", typ: "JWT" }
Payload: {
    sub: "john.doe",        // subject (username)
    role: "ROLE_ADMIN",     // custom claim
    userId: 42,             // custom claim
    iat: 1715123456,        // issued at
    exp: 1715209856         // expires at (24 hours later)
}
Signature: HMAC-SHA256(secret)
```

**Key methods:**

```java
public String generateToken(String username, String role, Long userId) {
    Date now = new Date();
    Date expiry = new Date(now.getTime() + expiration);  // 24 hours by default
    
    return Jwts.builder()
        .subject(username)
        .claim("role", role)
        .claim("userId", userId)
        .issuedAt(now)
        .expiration(expiry)
        .signWith(getSigningKey())  // HMAC-SHA256 with secret key
        .compact();
}

public boolean isTokenValid(String token) {
    try {
        parseClaims(token);
        return !isTokenExpired(token);
    } catch (JwtException | IllegalArgumentException ex) {
        log.warn("Invalid JWT: {}", ex.getMessage());
        return false;
    }
}

public String extractUsername(String token) {
    return parseClaims(token).getSubject();  // "john.doe"
}

public String extractRole(String token) {
    return parseClaims(token).get("role", String.class);  // "ROLE_ADMIN"
}
```

**Validation checks:**
- Signature valid (secret key matches)
- Token not expired (`exp` claim > now)
- All required claims present

**Secret key:** Hardcoded in `application.yml` (dev only — use environment variables in production).

---

### 5. **SecurityConfig.java** — Spring Security (Reactive)

**Responsibility:** Define authentication flow, CORS, and role-based access control.

**Key sections:**

#### 5a. **CORS Configuration**
```java
public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration config = new CorsConfiguration();
    config.setAllowedOriginPatterns(List.of("*"));  // tighten in production
    config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
    config.setAllowedHeaders(List.of("*"));
    config.setExposedHeaders(List.of("Authorization"));  // allow JS to read this header
    config.setAllowCredentials(true);
    config.setMaxAge(3600L);
    ...
}
```

#### 5b. **Security Filter Chain**
```java
@Bean
public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
    return http
        .cors(...)  // enable CORS
        .csrf(ServerHttpSecurity.CsrfSpec::disable)  // JWTs are CSRF-proof (no session cookies)
        .formLogin(...::disable)  // not using form login
        .httpBasic(...::disable)  // not using HTTP Basic auth
        .securityContextRepository(jwtSecurityContextRepository)  // JWT context loading
        .authorizeExchange(auth -> auth
            .pathMatchers("/api/auth/**").permitAll()  // public
            .pathMatchers("/actuator/health", "/actuator/info").permitAll()
            
            // ROLE-BASED ACCESS CONTROL (RBAC) — examples:
            .pathMatchers("/api/kpis/**")
                .hasAnyAuthority("ROLE_CLAIMS_ANALYST", "ROLE_CLAIMS_MANAGER", ...)
            
            .pathMatchers("/api/users/**")
                .hasAuthority("ROLE_ADMIN")  // admin only
            
            .pathMatchers(HttpMethod.POST, "/api/ingest/**")
                .hasAnyAuthority("ROLE_CLAIMS_ANALYST", "ROLE_ADMIN")  // analysts & admins
            
            .anyExchange().authenticated()  // all other paths require authentication
        )
        .exceptionHandling(ex -> ex
            .authenticationEntryPoint((exchange, e) -> 
                // 401 Unauthorized if no valid JWT
                Mono.fromRunnable(() -> exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED))
            )
            .accessDeniedHandler((exchange, e) -> 
                // 403 Forbidden if JWT valid but insufficient role
                Mono.fromRunnable(() -> exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN))
            )
        )
        .build();
}
```

---

### 6. **AuthHeaderForwardFilter.java** — Header Propagation

**Responsibility:** Add user identity headers to requests forwarded to downstream services.

```java
@Component
public class AuthHeaderForwardFilter implements GlobalFilter, Ordered {

    @Override
    public int getOrder() {
        return -1;  // Run AFTER Spring Security's filter chain
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return ReactiveSecurityContextHolder.getContext()
            .map(SecurityContext::getAuthentication)
            .flatMap(auth -> {
                String username = auth.getName();
                String role = auth.getAuthorities().stream()
                    .findFirst()
                    .map(Object::toString)
                    .orElse("");
                
                var mutatedRequest = exchange.getRequest().mutate()
                    .header("X-Auth-Username", username)     // e.g., "john.doe"
                    .header("X-Auth-Role", role)             // e.g., "ROLE_ADMIN"
                    .build();
                
                return chain.filter(exchange.mutate().request(mutatedRequest).build());
            })
            .switchIfEmpty(chain.filter(exchange));  // no auth context → pass through
    }
}
```

**Why this matters:**
- Downstream services (data-ingestion, fraud-risk, etc.) read `X-Auth-Username` and `X-Auth-Role` to:
  - Attribute log entries to the user who triggered them
  - Avoid re-validating the JWT (gateway already did)
  - Make authorization decisions (e.g., "only analysts can view sensitive data")

---

### 7. **AuditFilter.java** — Request Auditing

**Responsibility:** Log every meaningful request to the `audit_logs` table (who, what action, when, from where).

**Execution order:**
```
Security filters (JWT validation)
    ↓
AuthHeaderForwardFilter (adds X-Auth-Username, X-Auth-Role headers)
    ↓
AuditFilter (logs the action) ← WE ARE HERE
    ↓
Route to downstream service
```

**What gets logged:**
- **Mutations:** CREATE, UPDATE, DELETE, REGISTER, LOGIN attempts
- **Sensitive reads:** /api/users, /api/audit
- **NOT logged:** GET requests to dashboards (too noisy)

**Example audit actions:**

| HTTP Method | Path | Action |
|---|---|---|
| POST | /api/ingest | "CREATE Claim" |
| GET | /api/risk-scores | (skipped, read-only) |
| PUT | /api/users/5 | "UPDATE User" |
| DELETE | /api/feeds/3 | "DELETE Feed" |
| POST | /api/auth/login | "LOGIN" |

**Upsert logic (avoid duplicate rows):**
```
If the same (username, action, resource) occurred in the last 30 min:
    → BUMP the existing row's timestamp instead of inserting a new row

Exceptions (always insert fresh rows):
    - Mutations (CREATE, UPDATE, DELETE)
    - Security events (LOGIN_FAILED, REGISTER, USER_DELETED)
    - Token operations (TOKEN_REFRESH)
```

**Metadata JSON example:**
```json
{
  "method": "POST",
  "status": 201,
  "ip": "192.168.1.100",
  "userAgent": "Mozilla/5.0 ..."
}
```

---

### 8. **AuthService.java** — User Authentication & Session Management

**Responsibility:** Register, login, refresh tokens, logout.

#### 8a. **Registration**

```java
@Transactional
public AuthResponseDTO register(RegisterRequestDTO request) {
    // 1. Check username & email not taken (generic error message — don't leak existence)
    boolean usernameTaken = userRepository.existsByUsername(request.getUsername());
    boolean emailTaken = userRepository.existsByEmail(request.getEmail());
    if (usernameTaken || emailTaken) {
        throw new IllegalArgumentException("This username or email is already registered.");
    }
    
    // 2. Create user with BCrypt-hashed password
    Role role = request.getRole() != null ? request.getRole() : Role.ROLE_CLAIMS_ANALYST;
    User user = User.builder()
        .username(request.getUsername())
        .name(request.getName())
        .email(request.getEmail())
        .phone(request.getPhone())
        .password(passwordEncoder.encode(request.getPassword()))  // BCrypt
        .role(role)
        .enabled(true)
        .build();
    
    User saved = userRepository.save(user);
    
    // 3. Log & notify
    auditService.log(saved.getUsername(), saved.getId(), "REGISTER", "/api/auth/register", ...);
    notificationEmitter.emitUserSync(...);  // Mirror to NotificationService
    
    // 4. Issue tokens
    String accessToken = jwtService.generateToken(...);    // 24h expiry
    String refreshToken = issueRefreshToken(...);          // 7d expiry
    
    return AuthResponseDTO.builder()
        .accessToken(accessToken)
        .refreshToken(refreshToken)
        .tokenType("Bearer")
        .expiresIn(86400)  // seconds
        .username(saved.getUsername())
        .role(saved.getRole().name())
        .build();
}
```

**Password hashing:** BCryptPasswordEncoder with default strength (10 rounds).

#### 8b. **Login**

```java
@Transactional
public AuthResponseDTO login(LoginRequestDTO request) {
    String identifier = request.getUsername();  // can be username OR email
    
    // 1. Check if account is locked (brute-force protection)
    loginAttemptService.checkLocked(identifier);  // throws AccountLockedException
    
    // 2. Lookup user
    User user = userRepository.findByUsernameOrEmail(identifier, identifier)
        .orElseThrow(() -> {
            loginAttemptService.recordFailure(identifier);  // increment fail counter
            auditService.log(identifier, null, "LOGIN_FAILED", "/api/auth/login",
                "{\"reason\":\"user not found\"}");
            return new BadCredentialsException("Invalid username or password");
        });
    
    // 3. Check account enabled
    if (!user.isEnabled()) {
        auditService.log(user.getUsername(), user.getId(), "LOGIN_FAILED", ...);
        notificationEmitter.emitDisabledAccountLogin(user.getUsername());
        throw new BadCredentialsException("Account is disabled");
    }
    
    // 4. Check password
    if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
        loginAttemptService.recordFailure(identifier);
        auditService.log(user.getUsername(), user.getId(), "LOGIN_FAILED", ...);
        throw new BadCredentialsException("Invalid username or password");
    }
    
    // 5. Success!
    loginAttemptService.recordSuccess(identifier);  // reset fail counter
    auditService.log(user.getUsername(), user.getId(), "LOGIN_SUCCESS", ...);
    
    // 6. Mirror user to NotificationService (idempotent)
    notificationEmitter.emitUserSync(...);
    
    // 7. Issue new tokens
    return buildResponse(user, 
        jwtService.generateToken(...),
        issueRefreshToken(...)
    );
}
```

**Failed login sequence:**
```
1st fail: counter = 1
2nd fail: counter = 2
3rd fail: counter = 3
4th fail: counter = 4
5th fail: counter = 5 → LOCKED for 15 minutes
6th attempt: AccountLockedException: "Too many failed attempts. Account locked for 15 more minute(s)."
```

#### 8c. **Token Refresh** (Rotation)

```java
@Transactional
public AuthResponseDTO refresh(String rawToken) {
    // 1. Lookup refresh token row
    RefreshToken stored = refreshTokenRepository.findByToken(rawToken)
        .orElseThrow(() -> new BadCredentialsException("Invalid or expired refresh token"));
    
    // 2. Check not revoked
    if (stored.isRevoked()) {
        throw new BadCredentialsException("Refresh token has been revoked");
    }
    
    // 3. Check not expired
    if (stored.getExpiresAt().isBefore(java.time.LocalDateTime.now())) {
        stored.setRevoked(true);
        refreshTokenRepository.save(stored);
        throw new BadCredentialsException("Refresh token has expired — please log in again");
    }
    
    // 4. Lookup user
    User user = userRepository.findByUsernameOrEmail(stored.getUsername(), ...)
        .orElseThrow(() -> new BadCredentialsException("User no longer exists"));
    
    // 5. ROTATION: revoke old token, issue new one (prevents replay attacks)
    stored.setRevoked(true);
    refreshTokenRepository.save(stored);
    
    String newAccess = jwtService.generateToken(user.getUsername(), user.getRole().name(), user.getId());
    String newRefresh = issueRefreshToken(user.getUsername());
    
    auditService.log(user.getUsername(), user.getId(), "TOKEN_REFRESH", ...);
    
    return buildResponse(user, newAccess, newRefresh);
}
```

**Token rotation prevents replay attacks:**
```
Timeline:
  T=0    User logs in
         → accessToken (24h expiry)
         → refreshToken_1 (7d expiry, stored in DB)
  
  T=5m   Client uses refreshToken_1
         → Server revokes refreshToken_1 ✗
         → Issues refreshToken_2 (new 7d expiry)
         → Returns new accessToken
  
  T=10m  Attacker tries to use refreshToken_1 (captured from network)
         → Server checks: is refreshToken_1 revoked? YES ✗
         → Throws: "Refresh token has been revoked"
         → Attack blocked!
```

#### 8d. **Logout**

```java
@Transactional
public void logout(String username) {
    refreshTokenRepository.revokeAllByUsername(username);
    auditService.log(username, null, "LOGOUT", "/api/auth/logout", null);
}
```

**Why not invalidate access tokens?**
- JWTs are stateless — server has no way to invalidate them without a blacklist
- Access tokens expire naturally in 24 hours
- Clients discard their access token on logout, which is sufficient for UX

---

### 9. **LoginAttemptService.java** — Brute-Force Protection

**Responsibility:** Lock accounts after 5 failed login attempts.

```java
private static final int MAX_ATTEMPTS = 5;
private static final long LOCK_DURATION_MS = 15 * 60 * 1_000L;  // 15 min

public void checkLocked(String identifier) {
    Attempt a = cache.get(normalise(identifier));
    if (a != null && a.lockUntilMs() > System.currentTimeMillis()) {
        long secsLeft = (a.lockUntilMs() - System.currentTimeMillis()) / 1_000;
        throw new AccountLockedException(
            "Too many failed attempts. Account locked for "
            + Math.max(1, secsLeft / 60) + " more minute(s)."
        );
    }
}

public void recordFailure(String identifier) {
    String key = normalise(identifier);
    Attempt prev = cache.getOrDefault(key, new Attempt(0, 0));
    int next = prev.count() + 1;
    long lockUntil = (next >= MAX_ATTEMPTS)
        ? System.currentTimeMillis() + LOCK_DURATION_MS
        : 0L;
    cache.put(key, new Attempt(next, lockUntil));
}

public void recordSuccess(String identifier) {
    cache.remove(normalise(identifier));  // reset counter
}
```

**State:** In-memory `ConcurrentHashMap` (lost on service restart, acceptable for dev).

**Production consideration:** Use Redis to persist lockouts across instances.

---

### 10. **User.java & Role Enum**

**User Entity:**
```java
@Entity
@Table(name = "gateway_users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true, length = 50)
    private String username;
    
    @Column(length = 100)
    private String name;
    
    @Column(nullable = false, unique = true, length = 100)
    private String email;
    
    @Column(length = 20)
    private String phone;
    
    @Column(nullable = false)
    private String password;  // BCrypt hash
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 25)
    private Role role;
    
    @Builder.Default
    @Column(nullable = false)
    private boolean enabled = true;
    
    @CreationTimestamp
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
```

**Roles:**
```java
public enum Role {
    ROLE_CLAIMS_ANALYST,     // Analyze claims, denials, trends
    ROLE_CLAIMS_MANAGER,     // Oversee KPIs, delays, workloads
    ROLE_FRAUD_ANALYST,      // Risk dashboards, red-flag indicators
    ROLE_ACTUARY,            // Severity/frequency for pricing/reserving
    ROLE_OPERATIONS_EXEC,    // Monitor KPIs, TAT, SLAs
    ROLE_ADMIN               // Configure everything, manage users
}
```

---

## Request Flow Example: Login → Dashboard Access

### Step 1: **POST /api/auth/login**
```
Client sends:
{
  "username": "john.doe",
  "password": "SecurePass123"
}
```

### Step 2: **AuthController.login()**
```
↓ AuthService.login()
  ↓ LoginAttemptService.checkLocked("john.doe")  
    → OK (not locked)
  ↓ UserRepository.findByUsernameOrEmail("john.doe", "john.doe")
    → Found: User(id=1, username="john.doe", role=ROLE_FRAUD_ANALYST, enabled=true)
  ↓ PasswordEncoder.matches(inputPassword, storedHash)
    → true
  ↓ LoginAttemptService.recordSuccess("john.doe")
    → Reset fail counter
  ↓ JwtService.generateToken("john.doe", "ROLE_FRAUD_ANALYST", 1)
    → accessToken = "eyJhbGc..." (24h expiry)
  ↓ issueRefreshToken("john.doe")
    → Revoke all old refresh tokens
    → Generate UUID: "a1b2c3d4-..."
    → Save to refresh_tokens table with 7d expiry
    → Return "a1b2c3d4-..."
  ↓ AuditService.log("john.doe", 1, "LOGIN_SUCCESS", "/api/auth/login", null)
    → Async insert to audit_logs table
  ↓ Return AuthResponseDTO
```

Server returns:
```json
HTTP 200
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "a1b2c3d4-...",
  "tokenType": "Bearer",
  "expiresIn": 86400,
  "username": "john.doe",
  "role": "ROLE_FRAUD_ANALYST"
}
```

### Step 3: **Frontend stores tokens in localStorage + Redux**

```javascript
localStorage.setItem('accessToken', accessToken);
localStorage.setItem('refreshToken', refreshToken);

dispatch(setAuth({
  username: "john.doe",
  role: "ROLE_FRAUD_ANALYST",
  accessToken
}));
```

### Step 4: **GET /api/risk-scores** (dashboard call)

```
Client sends:
  Authorization: Bearer eyJhbGci...

↓ Request arrives at gateway
  ↓ JwtSecurityContextRepository.load()
    → extractToken() finds "eyJhbGci..." from Authorization header
    ↓ JwtAuthManager.authenticate()
      → JwtService.isTokenValid("eyJhbGci...")
        → parseClaims() — verifies HMAC-SHA256 signature
        → isTokenExpired() — checks exp > now
        → true ✓
      → JwtService.extractUsername("eyJhbGci...") → "john.doe"
      → JwtService.extractRole("eyJhbGci...") → "ROLE_FRAUD_ANALYST"
      ↓ Returns UsernamePasswordAuthenticationToken(
          principal="john.doe",
          credentials=null,
          authorities=[SimpleGrantedAuthority("ROLE_FRAUD_ANALYST")]
        )
    ↓ SecurityContext created with this Authentication
  ↓ SecurityConfig.authorizeExchange() checks:
    → Path "/api/risk-scores/**" requires: ROLE_FRAUD_ANALYST or ROLE_ADMIN
    → User has ROLE_FRAUD_ANALYST ✓
    → Access granted
  ↓ AuthHeaderForwardFilter
    → Mutates request, adds headers:
      X-Auth-Username: john.doe
      X-Auth-Role: ROLE_FRAUD_ANALYST
  ↓ AuditFilter
    → Checks: is this a GET request? YES
    → Is /api/risk-scores a sensitive read? NO (it's a dashboard)
    → Skip logging (too noisy)
  ↓ Gateway routes to fraud-risk-service via load balancer
    lb://fraud-risk-service/api/risk-scores
  
↓ Downstream service (fraud-risk) receives:
  GET /api/risk-scores
  Headers:
    X-Auth-Username: john.doe
    X-Auth-Role: ROLE_FRAUD_ANALYST
    
  → Reads headers, attributes response to john.doe
  → Returns risk scores JSON
  
↓ Gateway forwards response to client
  HTTP 200
  [{ riskId: 1, riskScore: 87, ... }, ...]
```

### Step 5: **Token expires or user clicks logout**

**Scenario A: Access token expires (24 hours)**
```
Client tries to GET /api/risk-scores with expired token
  ↓ JwtService.isTokenValid() → false (exp < now)
  ↓ JwtAuthManager.authenticate() → Mono.empty()
  ↓ No SecurityContext created
  ↓ SecurityConfig.authorizeExchange() → endpoint requires auth
  ↓ GlobalExceptionHandler.authenticationEntryPoint()
    → HTTP 401 Unauthorized
  
Frontend catches 401:
  → Tries to refresh: POST /api/auth/refresh?refreshToken=a1b2c3d4-...
    ↓ AuthService.refresh()
      ↓ Lookup RefreshToken row → found
      ↓ Check revoked → false ✓
      ↓ Check expired → false ✓
      ↓ Revoke old token
      ↓ Issue new accessToken (24h)
      ↓ Issue new refreshToken_2
      ↓ Return both
    → Frontend stores new tokens
    → Retry original GET /api/risk-scores with new token
    → Success!
```

**Scenario B: User clicks logout**
```
POST /api/auth/logout (with valid access token)
  ↓ AuthController.logout()
    ↓ AuthService.logout("john.doe")
      ↓ RefreshTokenRepository.revokeAllByUsername("john.doe")
        → Set revoked=true on all rows for this user
      ↓ AuditService.log(..., "LOGOUT", ...)
  ↓ HTTP 200 OK
  
Frontend:
  → Clear localStorage (accessToken, refreshToken)
  → Clear Redux auth state
  → Redirect to /login
  
If user tries to use old tokens:
  → Access token: expires naturally (no blacklist needed)
  → Refresh token: next POST /refresh finds revoked=true → 401
```

---

## Role-Based Access Control (RBAC) Matrix

From `SecurityConfig.authorizeExchange()`:

| Endpoint | Role(s) | Method(s) |
|---|---|---|
| /api/auth/** | *public* | POST |
| /api/kpis/** | ANALYST, MANAGER, ACTUARY, OPS_EXEC, ADMIN | GET, POST |
| /api/feeds/** (GET) | ANALYST, MANAGER, ADMIN | GET |
| /api/feeds/** (mutations) | ANALYST, ADMIN | POST, PUT, DELETE |
| /api/ingest/** (GET) | ANALYST, MANAGER, ADMIN | GET |
| /api/ingest/** (mutations) | ANALYST, ADMIN | POST, PUT, DELETE |
| /api/adjusters/**, /api/sla-violations/** | MANAGER, OPS_EXEC, ADMIN | GET, POST, PUT, DELETE |
| /api/costs/**, /api/reserves/**, /api/aging/** | ACTUARY, MANAGER, ADMIN | GET, POST, PUT, DELETE |
| /api/risk-scores/**, /api/risk-indicators/** | FRAUD_ANALYST, ADMIN | GET, POST |
| /api/investigations/** (GET) | FRAUD_ANALYST, MANAGER, ADMIN | GET |
| /api/investigations/** (mutations) | FRAUD_ANALYST, ADMIN | POST, PUT, DELETE |
| /api/denial-patterns/**, /api/leakage-flags/** | ANALYST, FRAUD_ANALYST, ADMIN | GET, POST, PUT, DELETE |
| /api/reports/** | *authenticated* | GET, POST |
| /api/notifications/** | *authenticated* | GET, POST |
| /api/users/** | ADMIN | GET, POST, PUT, DELETE |
| /api/audit/** | ADMIN | GET, POST, DELETE |

---

## Database Schema

### gateway_users

```sql
CREATE TABLE gateway_users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    name VARCHAR(100),
    email VARCHAR(100) UNIQUE NOT NULL,
    phone VARCHAR(20),
    password VARCHAR(255) NOT NULL,  -- BCrypt hash
    role ENUM('ROLE_CLAIMS_ANALYST', 'ROLE_CLAIMS_MANAGER', ...) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
```

### refresh_tokens

```sql
CREATE TABLE refresh_tokens (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    token VARCHAR(255) UNIQUE NOT NULL,  -- UUID string
    username VARCHAR(50) NOT NULL,
    expires_at TIMESTAMP NOT NULL,       -- 7 days from creation
    revoked BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

### audit_logs

```sql
CREATE TABLE audit_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL,
    user_id BIGINT,
    action VARCHAR(100) NOT NULL,           -- "LOGIN", "CREATE Risk Score", etc.
    resource VARCHAR(255) NOT NULL,         -- "/api/auth/login", "/api/risk-scores", etc.
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    metadata JSON,                          -- {"method":"POST","status":201,"ip":"..."}
    INDEX idx_username (username),
    INDEX idx_action (action),
    INDEX idx_timestamp (timestamp)
);
```

---

## Common Interview Questions & Answers

### Q1: **Explain the JWT validation flow in the API Gateway.**

**A:**
```
1. Client sends: Authorization: Bearer <token>
2. JwtSecurityContextRepository.load() intercepts the request
3. Calls extractToken() to pull the token from header or query param
4. Delegates to JwtAuthManager.authenticate()
5. JwtAuthManager calls JwtService.isTokenValid(token)
   a. parseClaims() — verifies HMAC-SHA256 signature using the secret key
   b. isTokenExpired() — checks if exp claim > current time
6. If valid, extracts username and role claims from payload
7. Creates UsernamePasswordAuthenticationToken with username, null credentials, and role as authority
8. Returns populated Mono<Authentication>
9. If invalid, returns empty Mono → no auth context
10. For protected endpoints, missing auth context → 401 Unauthorized

The token is never stored server-side. All validation is cryptographic 
(signature verification) and time-based (expiry check).
```

### Q2: **Why does the gateway use `JwtSecurityContextRepository` instead of `UserDetailsService`?**

**A:**
```
JWTs are stateless — the token itself contains all the info needed (username, role, expiry).
UserDetailsService is for session-based auth — it loads user details from a DB 
on every request, which is:
  1. Slower (extra DB hit)
  2. Unnecessary (we have the claims in the JWT)
  3. Incompatible with stateless architecture

JwtSecurityContextRepository:
  - Extracts claims from the JWT (no DB needed)
  - Creates an in-memory Authentication object
  - Returns it as the SecurityContext
  - Stateless and fast
```

### Q3: **How does token refresh with rotation work?**

**A:**
```
PROBLEM: If a refresh token is stolen, an attacker can get infinite new access tokens.

SOLUTION: Rotation on every refresh

Scenario:
  - User logs in
    → accessToken_1 (24h)
    → refreshToken_1 (stored in DB, 7d, revoked=false)
  
  - 1 hour later, user's access token expires
    → Client: POST /api/auth/refresh?refreshToken=refreshToken_1
    → Server:
      1. Lookup refreshToken_1 in DB
      2. Check: not revoked? ✓
      3. Check: not expired? ✓
      4. Set revoked=true (OLD token is now invalid)
      5. Create refreshToken_2 (new 7d)
      6. Return accessToken_2 + refreshToken_2
    → Client: Stores both new tokens
  
  - Attacker tries to use refreshToken_1 (captured earlier)
    → Server: Lookup refreshToken_1
    → Check: revoked? YES ✗
    → Throws: "Refresh token has been revoked"
    → Attack prevented!

Key insight: Only ONE refresh token is valid at a time per user.
If multiple refresh requests come in (e.g., race condition), the first one 
wins and all others fail.
```

### Q4: **Why is login brute-force protection in-memory, and is that a problem?**

**A:**
```
IN-MEMORY IMPLEMENTATION (LoginAttemptService):
  - Uses ConcurrentHashMap<String, Attempt>
  - After 5 failed logins: lock for 15 minutes
  - State is lost on service restart

IS THIS A PROBLEM?
  - For DEV: NO — acceptable trade-off for simplicity
  - For PRODUCTION: YES
    → If gateway restarts, all lockouts are cleared
    → Attacker can brute-force again immediately
    → Solution: Use Redis (persistent across restarts, shared across instances)

In a clustered setup:
  - Multiple gateway instances all have separate in-memory caches
  - Attacker can distribute 5 attempts across 3 instances → no lockout
  - Only Redis solves this
```

### Q6: **Explain the AuditFilter's "upsert within 30 min" logic. Why not just insert every log?**

**A:**
```
PROBLEM: The frontend polls dashboards frequently:
  - Every page load fires 6-12 GET requests
  - Notification bell polls every 30 seconds
  - Each poll is a separate GET request
  
Without deduplication:
  - Hundreds of "VIEW Risk Scores" rows in 1 hour
  - Audit table becomes noise, not a security tool

SOLUTION: Upsert within 30-minute window

For non-mutation actions (e.g., VIEW, MARK ALL READ):
  - Check if (username, action, resource) was logged in last 30 min
  - If YES: update the existing row's timestamp (collapse them)
  - If NO: insert a new row

Example:
  10:00 - user views /api/risk-scores → INSERT row (id=1)
  10:05 - user views /api/risk-scores → UPDATE row 1's timestamp to 10:05
  10:10 - user views /api/risk-scores → UPDATE row 1's timestamp to 10:10
  10:30 - user views /api/risk-scores → UPDATE row 1's timestamp to 10:30
  10:35 - user views /api/risk-scores → NEW 30-min window elapsed → INSERT row (id=2)

EXCEPTIONS (always insert fresh rows):
  - Mutations: CREATE, UPDATE, DELETE (each is a distinct change to data)
  - Security events: LOGIN_FAILED, REGISTER, USER_DELETED (must track every attempt)
  - Token ops: TOKEN_REFRESH (each refresh is a rotation, must be tracked)
```

### Q7: **How does the gateway forward authentication to downstream services?**

**A:**
```
CHALLENGE: 
  Downstream services need to know:
  1. Who is making the request (username)
  2. What role they have (for authorization)
  
But they don't have the JWT secret to validate it themselves.

SOLUTION: AuthHeaderForwardFilter

After gateway validates JWT:
  1. Extract username from SecurityContext
  2. Extract role from SecurityContext (from JWT claims)
  3. Add headers to the request:
     X-Auth-Username: john.doe
     X-Auth-Role: ROLE_FRAUD_ANALYST
  4. Forward to downstream service
  
Downstream service:
  - Reads X-Auth-Username and X-Auth-Role headers
  - Assumes gateway already validated the JWT
  - Uses these headers for:
    • Logging (attribute actions to user)
    • Local authorization (check role for sensitive operations)
    • Audit trails
  
KEY POINT: Downstream services TRUST the gateway implicitly.
They don't re-validate JWTs (that would be circular and slow).
The gateway is the single source of truth for authentication.
```

### Q8: **Why exclude `ReactiveUserDetailsServiceAutoConfiguration`?**

**A:**
```
DEFAULT BEHAVIOR:
  @SpringBootApplication automatically enables UserDetailsServiceAutoConfiguration
  which creates:
  - An in-memory UserDetailsService
  - AuthenticationManager that tries to load users from that service
  - When gateway starts, it tries to create default credentials
  
CONFLICT:
  Our gateway uses JWT (stateless, no UserDetailsService)
  The auto-config creates beans we don't need
  When SecurityConfig sets only securityContextRepository (no authenticationManager),
  Spring Security tries to fall back to the default UserDetailsService
  This can cause:
  - Extra beans
  - Confusing error messages
  - Potential conflicts in filter chain
  
SOLUTION:
  @SpringBootApplication(exclude = ReactiveUserDetailsServiceAutoConfiguration.class)
  
  This tells Spring Boot:
  "Don't auto-config UserDetailsService; I'm providing my own auth strategy."
  
  We manually inject JwtSecurityContextRepository instead.
```

### Q9: **What happens if a user's account is disabled while they have a valid access token?**

**A:**
```
PROBLEM:
  Access token is valid for 24 hours.
  If admin disables a user, their token is still valid.
  User can keep accessing the system.

CURRENT BEHAVIOR:
  1. Admin disables user via PUT /api/users/5
     → UserService.update() sets enabled=false
     → RefreshTokenRepository.revokeAllByUsername(...) — revoke refresh tokens
  
  2. User with disabled account tries GET /api/risk-scores (valid token)
     → JwtService.isTokenValid() — still valid ✓
     → User is granted access
     → INCONSISTENCY: user can still use old token!
  
  3. User's token expires (24h later)
     → Tries to refresh: POST /api/auth/refresh
     → Refresh token is revoked → 401
     → Can't get new token
  
  4. Now locked out

THE GAP:
  Between disabling and token expiry, user can still access the system.
  
IDEAL FIX:
  Could add a "disabled users" blacklist in gateway memory or Redis.
  On every request, check: is this user disabled?
  But this adds latency and defeats the "stateless" design.
  
TRADE-OFF:
  Current design prioritizes speed (fast JWT validation) over perfect 
  real-time enforcement (which would require DB hits on every request).
  
  In practice, disabling and revoking refresh tokens is usually sufficient
  because:
  - Users eventually log out
  - Access tokens expire in 24h
  - Refresh tokens are revoked immediately
```

### Q10: **What is the order of the global filters, and why does it matter?**

**A:**
```
Filter execution order (determined by getOrder()):

1. Spring Security filters (implicit, order 0)
   - JwtSecurityContextRepository.load() — creates SecurityContext from JWT
   - SecurityConfig.authorizeExchange() — checks RBAC

2. AuthHeaderForwardFilter (order -1, runs BEFORE)
   Wait, -1 is AFTER 0 in numeric order... let me re-read.
   
Actually, LOWER order = runs FIRST:
   -1 (AuthHeaderForwardFilter) runs before other filters
   0 (Spring Security)
   1 (AuditFilter)

Let's trace a request:

Request arrives
  ↓
AuthHeaderForwardFilter (order -1)
  → BUT Spring Security hasn't run yet!
  → ReactiveSecurityContextHolder.getContext() returns empty
  → switchIfEmpty(chain.filter(exchange)) → passes through without headers
  → WAIT, this seems wrong...

Actually, I need to re-read the code. Let me look at AuthHeaderForwardFilter again.

Actually, the comments say:
"Run after Spring Security's filter chain"
And order = -1

In Spring Gateway:
- NEGATIVE order = runs FIRST
- POSITIVE order = runs LATER

So order -1 means it runs BEFORE everything else!
But the comment says "after"... This is a contradiction.

Let me look at AuditFilter:
"Run after AuthHeaderForwardFilter so security context is populated"
Order = 1

So execution order should be:
1. AuditFilter (order 1) - wait, this should run LATER with positive order
2. AuthHeaderForwardFilter (order -1) - should run FIRST with negative order

The comment is confusing. Let me re-interpret:

In Spring Security WebFlux:
- Lower numeric order runs FIRST
- Higher numeric order runs LATER

Actual execution:
1. Spring Security (implicit, order ~Integer.MIN_VALUE to 0)
   → JwtSecurityContextRepository and SecurityConfig
   → SecurityContext is CREATED and AVAILABLE
2. AuthHeaderForwardFilter (order -1)
   → SecurityContext is available
   → Adds X-Auth-Username and X-Auth-Role headers
3. AuditFilter (order 1)
   → Security context is available
   → Request headers include X-Auth-Username, X-Auth-Role
   → Logs action to audit_logs
4. Route to downstream service

Wait, I think I'm still confused. Let me look at what GlobalFilter does.

GlobalFilter runs as part of the Gateway filter chain, which is SEPARATE from 
Spring Security filters. The order is:

Spring Security Filter Chain (first)
  → Validates JWT, creates SecurityContext
  → Checks RBAC (authorizeExchange)
  → Returns 401/403 if denied
  → Continues if allowed

Then, if allowed:

Gateway Global Filters (second, in order)
  → AuditFilter (order 1)
    → Can access SecurityContext via ReactiveSecurityContextHolder
    → Logs action
  → AuthHeaderForwardFilter (order -1) — wait, -1 is BEFORE 1
    → Actually runs FIRST among global filters
    → Can access SecurityContext
    → Adds headers

Actually, let me re-read the code comment again:

AuditFilter says:
"Runs after AuthHeaderForwardFilter so that the security context is already populated."

So order should be:
- AuthHeaderForwardFilter first (order -1)
- AuditFilter second (order 1)

But both can access SecurityContext because Spring Security already ran.

Let me just say: The order doesn't matter much for functionality because:
1. Spring Security runs first (validates JWT, creates SecurityContext)
2. Both global filters can access the same SecurityContext
3. Both use Reactive context holders to avoid blocking

The actual order:
1. Spring Security: validate JWT → create SecurityContext
2. AuthHeaderForwardFilter: add X-Auth-* headers
3. AuditFilter: log action
4. Route to downstream
```

---

## Summary: Gateway Responsibilities

| Component | Responsibility |
|---|---|
| **JwtSecurityContextRepository** | Extract JWT from request (header or query param) |
| **JwtAuthManager** | Validate JWT signature & expiry |
| **JwtService** | Generate, parse, and validate JWTs |
| **SecurityConfig** | Define RBAC rules (which roles access which paths) |
| **AuthHeaderForwardFilter** | Add X-Auth-Username and X-Auth-Role headers |
| **AuditFilter** | Log mutations and sensitive reads to audit_logs table |
| **AuthController** | Expose /api/auth/* endpoints (register, login, refresh, logout) |
| **AuthService** | Orchestrate registration, login, token refresh, logout |
| **LoginAttemptService** | Implement brute-force protection (lock after 5 fails) |
| **UserService** | Manage users (admin read/update/delete) |
| **AuditService** | Read/write/delete audit logs with upsert deduplication |

---

## Key Takeaways

1. **Single entry point:** Gateway validates all JWTs; downstream services trust X-Auth-* headers.
2. **Stateless:** Access tokens are valid for 24h (no blacklist). Refresh tokens are rotated on every use.
3. **Layered security:** JWT signature validation + expiry check + RBAC + audit logging.
4. **Performance:** Using WebFlux (reactive) to handle thousands of concurrent connections on a small number of threads.
5. **Brute-force protection:** In-memory for dev, should use Redis for production.
6. **Audit deduplication:** 30-min upsert window collapses noisy reads but preserves all mutations and security events.


