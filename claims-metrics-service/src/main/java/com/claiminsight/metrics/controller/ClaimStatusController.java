package com.claiminsight.metrics.controller;

import com.claiminsight.metrics.dto.ClaimStatusRequestDTO;
import com.claiminsight.metrics.dto.ClaimStatusResponseDTO;
import com.claiminsight.metrics.service.ClaimStatusService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST controller for persisting ACTIVE/INACTIVE status per claim_id.
 * Base path: /api/claim-status.
 */
@RestController
@RequestMapping("/api/claim-status")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Claim Status", description = "Persist ACTIVE/INACTIVE status for claims")
public class ClaimStatusController {

    private final ClaimStatusService service;

    /** Returns every known claim_id → status as a flat map. */
    @GetMapping
    @Operation(summary = "Get all claim statuses as a map")
    public ResponseEntity<Map<String, String>> getAll() {
        log.info("GET /api/claim-status");
        return ResponseEntity.ok(service.getAllAsMap());
    }

    /** Returns the stored status for a claim, defaulting to ACTIVE if absent. */
    @GetMapping("/{claimId}")
    @Operation(summary = "Get status for a single claim (defaults to ACTIVE)")
    public ResponseEntity<ClaimStatusResponseDTO> getOne(@PathVariable String claimId) {
        log.info("GET /api/claim-status/{}", claimId);
        return ResponseEntity.ok(service.getOne(claimId));
    }

    /** Upserts the status for a claim. */
    @PutMapping("/{claimId}")
    @Operation(summary = "Upsert the status (ACTIVE/INACTIVE) for a claim")
    public ResponseEntity<ClaimStatusResponseDTO> upsert(
            @PathVariable String claimId,
            @Valid @RequestBody ClaimStatusRequestDTO body) {
        log.info("PUT /api/claim-status/{} — status: {}", claimId, body.getStatus());
        return ResponseEntity.ok(service.upsert(claimId, body.getStatus()));
    }
}
