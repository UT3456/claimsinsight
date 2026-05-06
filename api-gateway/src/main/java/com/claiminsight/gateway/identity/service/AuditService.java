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

    // ── Upsert window ───────────────────────────────────────────────────────
    // If the same (username, action, resource) triple was logged within this
    // window, we BUMP the existing row's timestamp instead of inserting a new
    // row. This collapses noisy repeats like:
    //   - SSE notification stream reconnecting every few minutes
    //   - User refreshing the same page repeatedly
    //   - Polling endpoints firing on a schedule
    //
    // Mutations (CREATE / UPDATE / DELETE / REGISTER / LOGIN_FAILED / USER_*
    // / AUDIT_CLEARED / AUDIT_PURGED) never get upserted — each occurrence is
    // a discrete event with security/forensic value, and merging them would
    // hide brute-force patterns or repeated administrative changes.
    private static final long UPSERT_WINDOW_MINUTES = 30L;

    // ── In-memory rapid-fire drop (sub-200ms window) ───────────────────────
    // Catches React StrictMode double-fires and warmup-ping-vs-real-call
    // races without hitting the DB.
    private static final long FAST_DEDUP_WINDOW_MS = 500L;
    private final ConcurrentHashMap<String, Long> recent = new ConcurrentHashMap<>();

    // -------------------------------------------------------------------------
    // Write (async — fire-and-forget, never blocks the request thread)
    // -------------------------------------------------------------------------

    @Async
    @Transactional
    public void log(String username, Long userId, String action, String resource, String metadata) {
        // Markers written from inside other @Transactional methods must always persist.
        boolean isMarker = "AUDIT_CLEARED".equals(action) || "AUDIT_PURGED".equals(action);

        if (!isMarker) {
            // 1. Fast in-memory drop for rapid-fire bursts (StrictMode, warmup race)
            String key  = username + "|" + action + "|" + resource;
            long   now  = System.currentTimeMillis();
            Long   prev = recent.put(key, now);
            if (prev != null && (now - prev) < FAST_DEDUP_WINDOW_MS) {
                return;
            }
            if (recent.size() > 2_000) {
                recent.entrySet().removeIf(e -> (now - e.getValue()) > FAST_DEDUP_WINDOW_MS);
            }
        }

        try {
            // 2. Upsert path — for non-mutation events, find an existing row in
            //    the last UPSERT_WINDOW_MINUTES and bump its timestamp + metadata
            //    instead of inserting a new row.
            if (!isMarker && shouldUpsert(action)) {
                LocalDateTime cutoff = LocalDateTime.now().minusMinutes(UPSERT_WINDOW_MINUTES);
                AuditLog existing = auditLogRepository
                        .findFirstByUsernameAndActionAndResourceAndTimestampAfterOrderByTimestampDesc(
                                username, action, resource, cutoff)
                        .orElse(null);
                if (existing != null) {
                    existing.setTimestamp(LocalDateTime.now());
                    if (metadata != null) existing.setMetadata(metadata);
                    auditLogRepository.save(existing);
                    return;
                }
            }

            // 3. Insert fresh row (first occurrence in window OR mutation event)
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

    /**
     * Returns true if this action type can safely be upserted (collapsed) within
     * the time window. Mutations and security-sensitive failures always insert
     * fresh rows so the audit history captures every distinct occurrence.
     */
    private static boolean shouldUpsert(String action) {
        if (action == null) return true;
        // Security-relevant — must keep separate rows
        if (action.equals("LOGIN_FAILED"))   return false;
        if (action.equals("REGISTER"))       return false;
        if (action.equals("USER_DELETED"))   return false;
        if (action.equals("TOKEN_REFRESH"))  return false; // each refresh is a token rotation
        // Mutation prefixes — each occurrence is a distinct change to data
        if (action.startsWith("CREATE "))    return false;
        if (action.startsWith("DELETE "))    return false;
        // NOTE: MARK ALL *, USER_UPDATED, UPDATE * are now upserted because
        // they are idempotent in intent — clicking "Mark all read" four times
        // in a row, or hitting save twice on the same user-edit form, should
        // not produce four audit rows. The 30 min upsert window collapses
        // them; a separate edit half an hour later still creates a new row.
        return true;
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
