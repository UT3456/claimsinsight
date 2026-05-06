package com.claiminsight.gateway.identity.repository;

import com.claiminsight.gateway.identity.model.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.Optional;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    Page<AuditLog> findByUsername(String username, Pageable pageable);

    Page<AuditLog> findByTimestampBetween(LocalDateTime from, LocalDateTime to, Pageable pageable);

    Page<AuditLog> findByAction(String action, Pageable pageable);

    /**
     * Returns the most-recent existing audit row matching the (username, action, resource)
     * tuple whose timestamp is within the upsert window. Used by AuditService.log() to
     * collapse repeated identical events into a single row whose timestamp is bumped.
     */
    Optional<AuditLog> findFirstByUsernameAndActionAndResourceAndTimestampAfterOrderByTimestampDesc(
            String username, String action, String resource, LocalDateTime cutoff);

    /** Bulk delete entries older than the cutoff. Returns the number of rows removed. */
    @Modifying
    @Query("DELETE FROM AuditLog a WHERE a.timestamp < :cutoff")
    int deleteOlderThan(LocalDateTime cutoff);

    /** Bulk delete by username (e.g. when scrubbing a deleted user's trail). */
    @Modifying
    @Query("DELETE FROM AuditLog a WHERE a.username = :username")
    int deleteByUsername(String username);

    /** Bulk delete by action label. */
    @Modifying
    @Query("DELETE FROM AuditLog a WHERE a.action = :action")
    int deleteByAction(String action);
}
