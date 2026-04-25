package com.claiminsight.gateway.identity.notification;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

/**
 * Outbound payload for {@code POST /api/notifications/dispatch}.
 * Matches NotificationService's {@code NotificationDispatchRequestDTO}.
 * Role strings are serialized to the server-side {@code UserRole} enum by name
 * (ANALYST, MANAGER, FRAUD, ACTUARY, EXECUTIVE, ADMIN).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
class NotificationDispatchRequest {
    private Set<Long> targetUserIds;
    private Set<String> targetRoles;
    private String title;
    private String message;
    private String category;   // RISK, DENIAL, COST, PERFORMANCE, AGING, SYSTEM
    private String referenceId;
}
