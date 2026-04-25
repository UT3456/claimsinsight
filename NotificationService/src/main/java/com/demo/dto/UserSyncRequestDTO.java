package com.demo.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Upsert payload used by the api-gateway to mirror its authoritative user
 * into NotificationService's mock {@code users} table. Identity (userId) is
 * assigned by the gateway — this service honors it verbatim so role-based
 * dispatch fan-out reaches the same userId the gateway signs into JWTs.
 *
 * <p>Role strings must match the server-side {@code UserRole} enum values:
 * ANALYST, MANAGER, FRAUD, ACTUARY, EXECUTIVE, ADMIN.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserSyncRequestDTO {

    @NotNull
    private Long userId;

    @NotBlank
    private String name;

    @Email
    @NotBlank
    private String email;

    /** Must be one of the UserRole enum names (ANALYST, MANAGER, FRAUD, ACTUARY, EXECUTIVE, ADMIN). */
    @NotBlank
    private String role;

    /** Nullable — defaults to true on the server side. */
    private Boolean isActive;
}
