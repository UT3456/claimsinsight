package com.claiminsight.gateway.filter;

import com.claiminsight.gateway.identity.service.AuditService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Logs every inbound request to the audit_logs table after the response is sent.
 * Runs after AuthHeaderForwardFilter so that the security context is already populated.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuditFilter implements GlobalFilter, Ordered {

    private final AuditService auditService;
    private final ObjectMapper objectMapper;

    /** Maps the first URL segment (after /api/) to a human-readable resource name. */
    private static final Map<String, String> RESOURCE_NAMES = Map.ofEntries(
        Map.entry("feeds",           "Feed"),
        Map.entry("kpis",            "Claim KPI"),
        Map.entry("sla-violations",  "SLA Violation"),
        Map.entry("adjusters",       "Adjuster"),
        Map.entry("risk-scores",     "Risk Score"),
        Map.entry("denial-patterns", "Denial Pattern"),
        Map.entry("leakage-flags",   "Leakage Flag"),
        Map.entry("costs",           "Claim Cost"),
        Map.entry("reserves",        "Reserve"),
        Map.entry("aging",           "Aging Record"),
        Map.entry("reports",         "Report"),
        Map.entry("notifications",   "Notification"),
        Map.entry("audit",           "Audit Log")
    );

    @Override
    public int getOrder() {
        // Run after security filters (order 0) but before routing
        return 1;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {

        ServerHttpRequest request = exchange.getRequest();
        String resource = request.getPath().value();
        String method   = request.getMethod().name();
        String clientIp = resolveClientIp(request);

        // ── Skip rules ───────────────────────────────────────────────────────
        // 1. Internal noise (actuator, eureka)
        // 2. /api/auth/* — AuthService writes its own richer rows
        if (resource.startsWith("/actuator")
                || resource.startsWith("/eureka")
                || resource.startsWith("/api/auth/")) {
            return chain.filter(exchange);
        }

        // 3. SKIP read-only GET requests for non-sensitive endpoints.
        //    Reasons:
        //    - Every page load fires 6-12 GETs (Dashboard parallel calls,
        //      warmup pings, notification polling) → audit table fills
        //      with "VIEW Risk Scores" noise inside seconds.
        //    - Audit logs should track ACTIONS WITH CONSEQUENCES (mutations)
        //      and SENSITIVE READS (user list, audit trail itself), not
        //      every dashboard widget that loads.
        //    Sensitive reads we DO keep auditing:
        //    - /api/users           (admins listing accounts)
        //    - /api/audit           (admins viewing the trail itself)
        if ("GET".equals(method) && !isSensitiveRead(resource)) {
            return chain.filter(exchange);
        }

        return ReactiveSecurityContextHolder.getContext()
                .map(ctx -> ctx.getAuthentication())
                .defaultIfEmpty(new AnonymousAuthentication())
                .flatMap(auth -> {
                    String username = auth.isAuthenticated() ? auth.getName() : "anonymous";

                    return chain.filter(exchange)
                            .doFinally(signal -> {
                                int status = exchange.getResponse().getStatusCode() != null
                                        ? exchange.getResponse().getStatusCode().value()
                                        : 0;

                                String action   = resolveAction(method, resource);
                                String metadata = buildMetadata(method, status, clientIp,
                                        request.getHeaders().getFirst("User-Agent"));

                                auditService.log(username, null, action, resource, metadata);
                            });
                });
    }

    /**
     * Derives a human-readable action label from the HTTP method and request path.
     * Examples:
     *   POST   /api/risk-scores        → "CREATE Risk Score"
     *   GET    /api/kpis               → "VIEW Claim KPIs"
     *   DELETE /api/feeds/5            → "DELETE Feed"
     *   PATCH  /api/notifications/3/status → "UPDATE Notification"
     *   POST   /api/auth/login         → "LOGIN"
     */
    private String resolveAction(String method, String path) {
        // Auth sub-paths get their own descriptive labels
        if (path.startsWith("/api/auth/")) {
            String sub = path.substring("/api/auth/".length()).split("[/?]")[0];
            switch (sub) {
                case "login":    return "LOGIN";
                case "register": return "REGISTER";
                case "logout":   return "LOGOUT";
                case "validate": return "VALIDATE TOKEN";
                default:         return "AUTH " + sub.toUpperCase();
            }
        }

        // Strip leading /api/
        String stripped = path.startsWith("/api/") ? path.substring(5) : path;
        String[] parts  = stripped.split("/");
        if (parts.length == 0 || parts[0].isBlank()) return method + " RESOURCE";

        String segment      = parts[0].toLowerCase();
        String resourceName = RESOURCE_NAMES.getOrDefault(segment, segment.toUpperCase());

        // Special sub-operations that override the simple verb mapping
        if (path.contains("/mark-all-read"))  return "MARK ALL READ " + resourceName + "s";
        if (path.contains("/analytics/"))     return "VIEW " + resourceName + " Analytics";
        if (path.contains("/summary"))        return "VIEW " + resourceName + " Summary";
        if (path.contains("/distribution"))   return "VIEW " + resourceName + " Distribution";
        if (path.contains("/escalation"))     return "VIEW Escalation Candidates";

        // Determine whether the call targets a specific record (numeric ID in path)
        boolean hasId = parts.length > 1 && parts[1].matches("\\d+");

        switch (method) {
            case "POST":   return "CREATE " + resourceName;
            case "PUT":    return "UPDATE " + resourceName;
            case "PATCH":  return "UPDATE " + resourceName;
            case "DELETE": return "DELETE " + resourceName;
            case "GET":    return hasId ? "VIEW " + resourceName : "VIEW " + resourceName + "s";
            default:       return method + " " + resourceName;
        }
    }

    /**
     * Endpoints whose READ access is itself a sensitive event worth recording —
     * an admin pulling up the user list or scanning the audit trail.
     * Everything else (dashboards, KPIs, claims listings) is too noisy and
     * not security-relevant.
     */
    private boolean isSensitiveRead(String path) {
        return path.startsWith("/api/users")
            || path.startsWith("/api/audit");
    }

    private String resolveClientIp(ServerHttpRequest request) {
        String forwarded = request.getHeaders().getFirst("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        var addr = request.getRemoteAddress();
        return addr != null ? addr.getAddress().getHostAddress() : "unknown";
    }

    /**
     * Builds audit metadata as properly escaped JSON using Jackson.
     * The old String.format approach broke on User-Agent strings that contain
     * quotes, backslashes, or other JSON-special characters.
     */
    private String buildMetadata(String method, int status, String ip, String userAgent) {
        // LinkedHashMap preserves insertion order for readable audit rows
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("method",    method);
        meta.put("status",    status);
        meta.put("ip",        ip);
        meta.put("userAgent", userAgent != null ? userAgent : "");
        try {
            return objectMapper.writeValueAsString(meta);
        } catch (JsonProcessingException e) {
            // Fallback — should never happen with a simple map of strings/ints
            return "{\"method\":\"" + method + "\",\"status\":" + status + "}";
        }
    }

    /** Placeholder authentication for unauthenticated (public) requests. */
    private static class AnonymousAuthentication implements org.springframework.security.core.Authentication {
        @Override public java.util.Collection<? extends org.springframework.security.core.GrantedAuthority> getAuthorities() { return java.util.List.of(); }
        @Override public Object getCredentials() { return null; }
        @Override public Object getDetails() { return null; }
        @Override public Object getPrincipal() { return "anonymous"; }
        @Override public boolean isAuthenticated() { return false; }
        @Override public void setAuthenticated(boolean b) {}
        @Override public String getName() { return "anonymous"; }
    }
}
