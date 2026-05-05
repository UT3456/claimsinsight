package com.claiminsight.gateway.identity.service;

import com.claiminsight.gateway.identity.model.AuditLog;
import com.claiminsight.gateway.identity.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    // -------------------------------------------------------------------------
    // Write (async — fire-and-forget, never blocks the request thread)
    // -------------------------------------------------------------------------

    @Async
    public void log(String username, Long userId, String action, String resource, String metadata) {
        try {
            AuditLog entry = AuditLog.builder()
                    .username(username)
                    .userId(userId)
                    .action(action)
                    .resource(resource)
                    .timestamp(LocalDateTime.now())
                    .metadata(metadata)
                    .build();
            auditLogRepository.save(entry);
        } catch (Exception ex) {
            log.error("Failed to save audit log for action={} resource={}: {}", action, resource, ex.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // Read (blocking — called from reactive context via Schedulers.boundedElastic)
    // -------------------------------------------------------------------------

    public Page<AuditLog> findAll(Pageable pageable) {
        return auditLogRepository.findAll(pageable);
    }

    public Page<AuditLog> findByUsername(String username, Pageable pageable) {
        return auditLogRepository.findByUsername(username, pageable);
    }

    public Page<AuditLog> findByAction(String action, Pageable pageable) {
        return auditLogRepository.findByAction(action, pageable);
    }

    public Page<AuditLog> findByDateRange(LocalDateTime from, LocalDateTime to, Pageable pageable) {
        return auditLogRepository.findByTimestampBetween(from, to, pageable);
    }

    // -------------------------------------------------------------------------
    // Delete (admin only — controller is guarded by SecurityConfig)
    // -------------------------------------------------------------------------

    /**
     * Wipes the entire audit_logs table. Then writes a single AUDIT_CLEARED
     * record so the trail of who-did-what is preserved.
     */
    @Transactional
    public long deleteAll(String actor) {
        long count = auditLogRepository.count();
        auditLogRepository.deleteAllInBatch();
        log.warn("[audit] {} cleared all {} audit records", actor, count);
        // Write the marker AFTER deletion so it survives in the table.
        AuditLog marker = AuditLog.builder()
                .username(actor)
                .userId(null)
                .action("AUDIT_CLEARED")
                .resource("/api/audit/logs")
                .timestamp(LocalDateTime.now())
                .metadata("{\"clearedRows\":" + count + ",\"scope\":\"all\"}")
                .build();
        auditLogRepository.save(marker);
        return count;
    }

    /** Removes a single audit row. Returns true if it existed. */
    @Transactional
    public boolean deleteById(Long id, String actor) {
        if (!auditLogRepository.existsById(id)) return false;
        auditLogRepository.deleteById(id);
        log.info("[audit] {} deleted audit record #{}", actor, id);
        return true;
    }

    /** Bulk-deletes entries older than the cutoff date. Returns the row count removed. */
    @Transactional
    public int deleteOlderThan(LocalDateTime cutoff, String actor) {
        int removed = auditLogRepository.deleteOlderThan(cutoff);
        log.warn("[audit] {} purged {} audit records older than {}", actor, removed, cutoff);
        // Marker so the cleanup itself is auditable
        AuditLog marker = AuditLog.builder()
                .username(actor)
                .userId(null)
                .action("AUDIT_PURGED")
                .resource("/api/audit/logs")
                .timestamp(LocalDateTime.now())
                .metadata("{\"removed\":" + removed + ",\"olderThan\":\"" + cutoff + "\"}")
                .build();
        auditLogRepository.save(marker);
        return removed;
    }
}
