package com.claims.client.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

/**
 * Mirrors NotificationService's {@code POST /api/notifications/dispatch} payload.
 * Fan-out form — delivers to every user matching any of {@code targetUserIds}
 * or {@code targetRoles}. Role strings map by name to the server-side
 * {@code UserRole} enum (ANALYST, MANAGER, FRAUD, ACTUARY, EXECUTIVE, ADMIN).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationDispatchRequestDTO {
    private Set<Long> targetUserIds;
    private Set<String> targetRoles;
    private String title;
    private String message;
    private String category;   // RISK, DENIAL, COST, PERFORMANCE, AGING, SYSTEM
    private String referenceId;
}
