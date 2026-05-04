package com.claiminsight.gateway.filter;

import com.claiminsight.gateway.identity.service.AuditService;
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

        // Skip:
        //  - /actuator and /eureka — internal noise, not useful in audit
        //  - /api/auth/* — AuthService writes its own richer audit rows
        //    (LOGIN_SUCCESS, LOGIN_FAILED with reason, REGISTER with role +
        //    real userId). Without this skip, every login produced 2 rows:
        //    one from AuthService (rich) + one from this filter (coarse).
        if (resource.startsWith("/actuator")
                || resource.startsWith("/eureka")
                || resource.startsWith("/api/auth/")) {
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

    private String resolveClientIp(ServerHttpRequest request) {
        String forwarded = request.getHeaders().getFirst("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        var addr = request.getRemoteAddress();
        return addr != null ? addr.getAddress().getHostAddress() : "unknown";
    }

    private String buildMetadata(String method, int status, String ip, String userAgent) {
        return String.format(
                "{\"method\":\"%s\",\"status\":%d,\"ip\":\"%s\",\"userAgent\":\"%s\"}",
                method, status, ip, userAgent != null ? userAgent.replace("\"", "'") : ""
        );
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
