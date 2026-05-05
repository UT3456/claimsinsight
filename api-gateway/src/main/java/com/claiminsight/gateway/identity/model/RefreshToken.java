package com.claiminsight.gateway.identity.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Persisted refresh token. One row per active session.
 * ddl-auto: update creates the table automatically on first startup.
 */
@Entity
@Table(name = "refresh_tokens", indexes = {
    @Index(name = "idx_rt_token",    columnList = "token"),
    @Index(name = "idx_rt_username", columnList = "username"),
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Opaque random UUID — stored as-is, never re-parsed as JWT. */
    @Column(nullable = false, unique = true, length = 512)
    private String token;

    @Column(nullable = false, length = 100)
    private String username;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    /** Set to true on logout or when a new token is issued for the same user. */
    @Column(nullable = false)
    @Builder.Default
    private boolean revoked = false;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void onPrePersist() {
        this.createdAt = LocalDateTime.now();
    }
}
