package com.claiminsight.gateway.identity.notification;

import com.claiminsight.gateway.identity.model.Role;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.Map;
import java.util.Set;

/**
 * Fire-and-forget emitter for auth-lifecycle notifications.
 * <p>
 * The gateway is a WebFlux app, so we call NotificationService with a
 * non-blocking {@link WebClient} instead of the Feign client pattern used by
 * the WebMVC services. Eureka-registered service names resolve via
 * {@code lb://NotificationService}; the spring-cloud-loadbalancer on the
 * classpath (transitively via spring-cloud-starter-gateway) handles resolution.
 * <p>
 * All emits are <b>best-effort</b>: failures are logged and swallowed so that
 * auth flows (register/login) never fail because NotificationService is down.
 */
@Slf4j
@Component
public class NotificationEmitter {

    /**
     * Translates gateway role names into NotificationService's UserRole enum
     * names. Both services deliberately keep their own enums — the gateway's
     * names are Spring-Security-style ({@code ROLE_*}); NotificationService's
     * are business-domain names. Everything on the wire between them uses the
     * NotificationService naming.
     */
    private static final Map<Role, String> ROLE_MAP = Map.of(
            Role.ROLE_CLAIMS_ANALYST,  "ANALYST",
            Role.ROLE_CLAIMS_MANAGER,  "MANAGER",
            Role.ROLE_FRAUD_ANALYST,   "FRAUD",
            Role.ROLE_ACTUARY,         "ACTUARY",
            Role.ROLE_OPERATIONS_EXEC, "EXECUTIVE",
            Role.ROLE_ADMIN,           "ADMIN"
    );

    private final WebClient webClient;

    public NotificationEmitter(WebClient.Builder builder,
                               @Value("${claiminsight.notification-service.url:lb://NotificationService}")
                               String notificationBaseUrl) {
        this.webClient = builder.baseUrl(notificationBaseUrl).build();
    }

    /** Translates a gateway Role → NotificationService UserRole name. */
    public static String toNotificationRole(Role role) {
        return ROLE_MAP.getOrDefault(role, "ANALYST");
    }

    /**
     * Mirrors the gateway user into NotificationService's mock {@code users}
     * table using the gateway's userId. Without this, role-based dispatch
     * fan-out resolves to seeded mock userIds (1–10) that the real frontend
     * user never polls against — so notifications are created but invisible.
     *
     * <p>Idempotent: server uses {@code INSERT ... ON DUPLICATE KEY UPDATE}.
     */
    public void emitUserSync(Long userId, String name, String email, Role role) {
        if (userId == null || role == null) return;
        Map<String, Object> body = Map.of(
                "userId",   userId,
                "name",     name != null ? name : "",
                "email",    email != null ? email : "",
                "role",     toNotificationRole(role),
                "isActive", true
        );
        webClient.post()
                .uri("/api/notifications/users/sync")
                .bodyValue(body)
                .retrieve()
                .toBodilessEntity()
                .timeout(Duration.ofSeconds(3))
                .retryWhen(Retry.backoff(1, Duration.ofMillis(300)))
                .doOnError(err -> log.warn("User sync failed [userId={}]: {}", userId, err.getMessage()))
                .onErrorResume(err -> reactor.core.publisher.Mono.empty())
                .subscribe();
    }

    /**
     * Announces that a new user account has been provisioned. Dispatches a
     * SYSTEM notification to every ADMIN + EXECUTIVE so platform ops have
     * visibility into who is being onboarded.
     */
    public void emitUserRegistered(String username, String role) {
        NotificationDispatchRequest req = NotificationDispatchRequest.builder()
                .targetRoles(Set.of("ADMIN", "EXECUTIVE"))
                .title("New user registered")
                .message("User '" + username + "' was registered with role " + role + ".")
                .category("SYSTEM")
                .referenceId(username)
                .build();
        send(req, "register:" + username);
    }

    /**
     * Raised when someone tries to sign in to a disabled account — useful as
     * an audit signal (the user may have been off-boarded but is still trying
     * to get in). Dispatched to ADMIN only.
     */
    public void emitDisabledAccountLogin(String identifier) {
        NotificationDispatchRequest req = NotificationDispatchRequest.builder()
                .targetRoles(Set.of("ADMIN"))
                .title("Disabled account login attempt")
                .message("A sign-in attempt was made against the disabled account '" + identifier + "'.")
                .category("SYSTEM")
                .referenceId(identifier)
                .build();
        send(req, "disabled-login:" + identifier);
    }

    // -------------------------------------------------------------------------
    // Private
    // -------------------------------------------------------------------------

    private void send(NotificationDispatchRequest req, String tag) {
        webClient.post()
                .uri("/api/notifications/dispatch")
                .bodyValue(req)
                .retrieve()
                .toBodilessEntity()
                .timeout(Duration.ofSeconds(3))
                .retryWhen(Retry.backoff(1, Duration.ofMillis(300)))
                .doOnError(err -> log.warn("Notification dispatch failed [{}]: {}", tag, err.getMessage()))
                .onErrorResume(err -> reactor.core.publisher.Mono.empty())
                .subscribe();
    }
}
