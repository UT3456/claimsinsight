package com.claiminsight.gateway.security;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.web.server.context.ServerSecurityContextRepository;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Extracts the Bearer token from the Authorization header and delegates
 * validation to {@link JwtAuthManager}.
 */
@Component
@RequiredArgsConstructor
public class JwtSecurityContextRepository implements ServerSecurityContextRepository {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtAuthManager jwtAuthManager;

    @Override
    public Mono<Void> save(ServerWebExchange exchange, SecurityContext context) {
        // Stateless — no session to save
        return Mono.empty();
    }

    @Override
    public Mono<SecurityContext> load(ServerWebExchange exchange) {
        String token = extractToken(exchange);
        if (token == null) {
            return Mono.empty();
        }
        return jwtAuthManager
                .authenticate(new UsernamePasswordAuthenticationToken(token, token))
                .map(SecurityContextImpl::new);
    }

    /**
     * Resolution order:
     *   1. Authorization: Bearer <token>   (standard)
     *   2. ?token=<token>                   (fallback for EventSource and
     *      other browser APIs that cannot set custom headers)
     *
     * The query-param path is intentionally NOT a standalone security path —
     * it just lets clients that physically can't send a header still attach
     * their JWT. The token is still validated identically by JwtAuthManager.
     */
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
}
