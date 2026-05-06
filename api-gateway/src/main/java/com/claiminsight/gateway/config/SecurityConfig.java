package com.claiminsight.gateway.config;

import com.claiminsight.gateway.security.JwtSecurityContextRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import reactor.core.publisher.Mono;

import java.util.List;

@Configuration
@EnableWebFluxSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    // NOTE: Do NOT inject JwtAuthManager here — setting authenticationManager()
    // on ServerHttpSecurity creates an extra AuthenticationWebFilter that intercepts
    // ALL requests and returns 403 even on permitAll() paths when no token is present.
    // The JwtSecurityContextRepository already calls JwtAuthManager internally.
    private final JwtSecurityContextRepository jwtSecurityContextRepository;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of("*"));   // tighten in production
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setExposedHeaders(List.of("Authorization"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                // Only set context repository — do NOT call .authenticationManager() here
                .securityContextRepository(jwtSecurityContextRepository)
                .authorizeExchange(auth -> auth
                        // ── Public ──────────────────────────────────────────────────────
                        .pathMatchers("/api/auth/**").permitAll()
                        .pathMatchers("/actuator/health", "/actuator/info").permitAll()
                        .pathMatchers(HttpMethod.GET, "/*/api-docs/**", "/*/swagger-ui/**").permitAll()

                        // ── Claims KPI & Metrics ────────────────────────────────────────
                        // Analysts, Managers, Actuaries, Ops Executives + Admin
                        .pathMatchers("/api/kpis/**").hasAnyAuthority(
                                "ROLE_CLAIMS_ANALYST", "ROLE_CLAIMS_MANAGER",
                                "ROLE_ACTUARY", "ROLE_OPERATIONS_EXEC", "ROLE_ADMIN")

                        // Claim ACTIVE/INACTIVE status — same roles as KPIs
                        .pathMatchers("/api/claim-status/**").hasAnyAuthority(
                                "ROLE_CLAIMS_ANALYST", "ROLE_CLAIMS_MANAGER",
                                "ROLE_ACTUARY", "ROLE_OPERATIONS_EXEC", "ROLE_ADMIN")

                        // ── Data Ingestion / Feeds ──────────────────────────────────────
                        // Read access (GET): Analysts, Managers + Admin
                        // Write access (POST/PUT/PATCH/DELETE): Analysts + Admin only
                        .pathMatchers(HttpMethod.GET, "/api/feeds/**", "/api/ingest/**").hasAnyAuthority(
                                "ROLE_CLAIMS_ANALYST", "ROLE_CLAIMS_MANAGER", "ROLE_ADMIN")
                        .pathMatchers("/api/feeds/**", "/api/ingest/**").hasAnyAuthority(
                                "ROLE_CLAIMS_ANALYST", "ROLE_ADMIN")

                        // ── Adjuster Operations & SLA ───────────────────────────────────
                        // Managers and Ops Executives oversee performance + Admin
                        .pathMatchers("/api/adjusters/**", "/api/sla-violations/**").hasAnyAuthority(
                                "ROLE_CLAIMS_MANAGER", "ROLE_OPERATIONS_EXEC", "ROLE_ADMIN")

                        // ── Cost, Reserve, Aging ────────────────────────────────────────
                        // Actuaries (pricing/reserving), Managers + Admin
                        .pathMatchers("/api/costs/**", "/api/reserves/**", "/api/aging/**").hasAnyAuthority(
                                "ROLE_ACTUARY", "ROLE_CLAIMS_MANAGER", "ROLE_ADMIN")

                        // ── Fraud Risk ──────────────────────────────────────────────────
                        // Fraud Analysts + Admin
                        .pathMatchers("/api/risk-scores/**", "/api/risk-indicators/**").hasAnyAuthority(
                                "ROLE_FRAUD_ANALYST", "ROLE_ADMIN")

                        // ── SIU Investigations ──────────────────────────────────────────
                        // Fraud Analysts open + view, Managers can also view, Admin full
                        .pathMatchers(HttpMethod.GET, "/api/investigations/**").hasAnyAuthority(
                                "ROLE_FRAUD_ANALYST", "ROLE_CLAIMS_MANAGER", "ROLE_ADMIN")
                        .pathMatchers("/api/investigations/**").hasAnyAuthority(
                                "ROLE_FRAUD_ANALYST", "ROLE_ADMIN")

                        // ── Denial & Leakage ────────────────────────────────────────────
                        // Claims Analysts and Fraud Analysts + Admin
                        .pathMatchers("/api/denial-patterns/**", "/api/leakage-flags/**").hasAnyAuthority(
                                "ROLE_CLAIMS_ANALYST", "ROLE_FRAUD_ANALYST", "ROLE_ADMIN")

                        // ── Reports ─────────────────────────────────────────────────────
                        // All authenticated users
                        .pathMatchers("/api/reports/**").authenticated()

                        // ── Notifications ───────────────────────────────────────────────
                        // All authenticated users
                        .pathMatchers("/api/notifications/**").authenticated()

                        // ── Audit Logs ──────────────────────────────────────────────────
                        // ADMIN ONLY — read AND write. The audit trail is too sensitive
                        // to surface to investigators directly; if a Fraud Analyst needs
                        // to see specific entries, an admin should pull them.
                        .pathMatchers("/api/audit/**").hasAuthority("ROLE_ADMIN")

                        // ── KPI definitions (read-only catalog hits /api/kpis) ──────────
                        // Already covered by /api/kpis/** rule above — no extra path needed.

                        // ── User management ─────────────────────────────────────────────
                        // Admin only — list / update role / disable / delete
                        .pathMatchers("/api/users/**").hasAuthority("ROLE_ADMIN")

                        // ── Catch-all ───────────────────────────────────────────────────
                        .anyExchange().authenticated()
                )
                .exceptionHandling(ex -> ex
                        // Return 401 instead of redirect for unauthenticated requests
                        .authenticationEntryPoint((exchange, e) -> Mono.fromRunnable(() ->
                                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED)))
                        // Return 403 for authenticated users lacking the required role
                        .accessDeniedHandler((exchange, e) -> Mono.fromRunnable(() ->
                                exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN)))
                )
                .build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
