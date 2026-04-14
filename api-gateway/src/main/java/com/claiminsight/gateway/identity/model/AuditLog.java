package com.claiminsight.gateway.identity.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Audit trail record — every authenticated API call and auth event is captured here.
 * Corresponds to the AuditLog entity in the Identity & Access Management module.
 */
@Entity
@Table(name = "audit_logs", indexes = {
    @Index(name = "idx_audit_user_id",   columnList = "user_id"),
    @Index(name = "idx_audit_timestamp", columnList = "timestamp")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "audit_id")
    private Long auditId;

    /** The gateway user who performed the action. Null for unauthenticated requests. */
    @Column(name = "user_id")
    private Long userId;

    /** Username string kept for readability even if user is later deleted. */
    @Column(name = "username", length = 50)
    private String username;

    /** High-level action name: LOGIN, REGISTER, API_ACCESS, AUTH_FAILURE. */
    @Column(name = "action", nullable = false, length = 50)
    private String action;

    /** The HTTP path that was accessed, e.g. /api/kpis, /api/feeds. */
    @Column(name = "resource", nullable = false, length = 255)
    private String resource;

    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;

    /**
     * JSON string with extra context: HTTP method, response status, client IP, user-agent.
     * Stored as TEXT to avoid schema migrations when fields are added.
     */
    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;
}
