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
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    // ── Dedup window ────────────────────────────────────────────────────────
    // Identical (username, action, resource) tuples within this window are
    // collapsed into a single audit row. Catches:
    //   - React StrictMode double-fired useEffect (~100 ms apart)
    //   - Warmup ping + Dashboard's actual API call to same endpoint (~200 ms)
    //   - User triple-clicking the same nav button
    private static final long DEDUP_WINDOW_MS = 3_000L;
    private final ConcurrentHashMap<String, Long> recent = new ConcurrentHashMap<>();

    // -------------------------------------------------------------------------
    // Write (async — fire-and-forget, never blocks the request thread)
    // -------------------------------------------------------------------------

    @Async
    public void log(String username, Long userId, String action, String resource, String metadata) {
        // Dedup: skip if the exact same triple was logged within DEDUP_WINDOW_MS.
        // We DON'T dedup the AUDIT_CLEARED / AUDIT_PURGED markers — those are
        // written from inside @Transactional methods and must always persist.
        if (!"AUDIT_CLEARED".equals(action) && !"AUDIT_PURGED".equals(action)) {
            String key   = username + "|" + action + "|" + resource;
            long   now   = System.currentTimeMillis();
            Long   prev  = recent.put(key, now);
            if (prev != null && (now - prev) < DEDUP_WINDOW_MS) {
                return; // collapse duplicate
            }
            // Bound the map — drop entries that have already aged out.
            if (recent.size() > 2_000) {
                recent.entrySet().removeIf(e -> (now - e.getValue()) > DEDUP_WINDOW_MS);
            }
        }
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
