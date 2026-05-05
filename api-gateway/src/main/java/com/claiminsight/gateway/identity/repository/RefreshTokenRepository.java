package com.claiminsight.gateway.identity.repository;

import com.claiminsight.gateway.identity.model.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByToken(String token);

    /** Revoke all active tokens for a user (called on logout). */
    @Modifying
    @Query("UPDATE RefreshToken r SET r.revoked = true WHERE r.username = :username AND r.revoked = false")
    void revokeAllByUsername(String username);

    /** Housekeeping — delete expired tokens so the table doesn't grow unbounded. */
    @Modifying
    @Query("DELETE FROM RefreshToken r WHERE r.expiresAt < :now")
    void deleteExpiredBefore(LocalDateTime now);
}
