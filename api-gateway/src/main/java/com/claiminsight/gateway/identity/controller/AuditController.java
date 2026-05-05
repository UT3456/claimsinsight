package com.claiminsight.gateway.identity.controller;

import com.claiminsight.gateway.identity.model.AuditLog;
import com.claiminsight.gateway.identity.service.AuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Read-only audit log endpoints.
 * All endpoints are restricted to ROLE_ADMIN via SecurityConfig.
 */
@RestController
@RequestMapping("/api/audit")
@RequiredArgsConstructor
public class AuditController {

    private final AuditService auditService;

    /** GET /api/audit/logs — paginated, newest first */
    @GetMapping("/logs")
    public Mono<ResponseEntity<Page<AuditLog>>> getAllLogs(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {

        PageRequest pageable = PageRequest.of(page, size, Sort.by("timestamp").descending());
        return Mono.fromCallable(() -> auditService.findAll(pageable))
                .subscribeOn(Schedulers.boundedElastic())
                .map(ResponseEntity::ok);
    }

    /** GET /api/audit/logs/user/{username} — audit trail for a specific user */
    @GetMapping("/logs/user/{username}")
    public Mono<ResponseEntity<Page<AuditLog>>> getByUser(
            @PathVariable String username,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {

        PageRequest pageable = PageRequest.of(page, size, Sort.by("timestamp").descending());
        return Mono.fromCallable(() -> auditService.findByUsername(username, pageable))
                .subscribeOn(Schedulers.boundedElastic())
                .map(ResponseEntity::ok);
    }

    /** GET /api/audit/logs/action/{action} — filter by action (LOGIN, REGISTER, API_ACCESS …) */
    @GetMapping("/logs/action/{action}")
    public Mono<ResponseEntity<Page<AuditLog>>> getByAction(
            @PathVariable String action,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {

        PageRequest pageable = PageRequest.of(page, size, Sort.by("timestamp").descending());
        return Mono.fromCallable(() -> auditService.findByAction(action.toUpperCase(), pageable))
                .subscribeOn(Schedulers.boundedElastic())
                .map(ResponseEntity::ok);
    }

    /** GET /api/audit/logs/range?from=…&to=… — date range filter */
    @GetMapping("/logs/range")
    public Mono<ResponseEntity<Page<AuditLog>>> getByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {

        PageRequest pageable = PageRequest.of(page, size, Sort.by("timestamp").descending());
        return Mono.fromCallable(() -> auditService.findByDateRange(from, to, pageable))
                .subscribeOn(Schedulers.boundedElastic())
                .map(ResponseEntity::ok);
    }

    // -------------------------------------------------------------------------
    // Mutations — admin only (enforced in SecurityConfig)
    // -------------------------------------------------------------------------

    /**
     * DELETE /api/audit/logs — clears the entire audit table.
     * Optional ?olderThan=ISO_DATETIME query param to purge only entries older
     * than the given timestamp instead of wiping everything.
     */
    @DeleteMapping("/logs")
    public Mono<ResponseEntity<Map<String, Object>>> clearLogs(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime olderThan,
            Authentication auth) {

        String actor = auth != null ? auth.getName() : "unknown";

        if (olderThan != null) {
            return Mono.fromCallable(() -> auditService.deleteOlderThan(olderThan, actor))
                    .subscribeOn(Schedulers.boundedElastic())
                    .map(removed -> ResponseEntity.ok(Map.of(
                            "scope",    "olderThan",
                            "olderThan", olderThan.toString(),
                            "removed",  removed)));
        }

        return Mono.fromCallable(() -> auditService.deleteAll(actor))
                .subscribeOn(Schedulers.boundedElastic())
                .map(removed -> ResponseEntity.ok(Map.of(
                        "scope",   "all",
                        "removed", removed)));
    }

    /** DELETE /api/audit/logs/{id} — single record removal. */
    @DeleteMapping("/logs/{id}")
    public Mono<ResponseEntity<Map<String, Object>>> deleteLog(
            @PathVariable Long id, Authentication auth) {
        String actor = auth != null ? auth.getName() : "unknown";
        return Mono.fromCallable(() -> auditService.deleteById(id, actor))
                .subscribeOn(Schedulers.boundedElastic())
                .map(removed -> removed
                        ? ResponseEntity.ok(Map.of("id", id, "removed", true))
                        : ResponseEntity.status(404).body(Map.of("id", id, "removed", false, "message", "Audit record not found")));
    }
}
