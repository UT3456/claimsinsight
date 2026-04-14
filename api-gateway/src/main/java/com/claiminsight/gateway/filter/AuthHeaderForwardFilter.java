package com.claiminsight.gateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Adds authenticated user info as request headers before forwarding to downstream services.
 * Downstream services can read X-Auth-Username and X-Auth-Role without re-validating the JWT.
 */
@Component
public class AuthHeaderForwardFilter implements GlobalFilter, Ordered {

    @Override
    public int getOrder() {
        // Run after Spring Security's filter chain
        return -1;
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
                            .header("X-Auth-Username", username)
                            .header("X-Auth-Role", role)
                            .build();

                    return chain.filter(exchange.mutate().request(mutatedRequest).build());
                })
                .switchIfEmpty(chain.filter(exchange));
    }
}
