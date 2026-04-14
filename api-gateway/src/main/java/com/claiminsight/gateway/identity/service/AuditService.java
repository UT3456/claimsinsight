package com.claiminsight.gateway.identity.service;

import com.claiminsight.gateway.identity.model.AuditLog;
import com.claiminsight.gateway.identity.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

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
}
