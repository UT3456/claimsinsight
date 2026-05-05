package com.claim360.fraudrisk.controller;

import com.claim360.fraudrisk.dto.CreateInvestigationRequestDTO;
import com.claim360.fraudrisk.dto.InvestigationDTO;
import com.claim360.fraudrisk.dto.UpdateInvestigationRequestDTO;
import com.claim360.fraudrisk.enums.InvestigationStatus;
import com.claim360.fraudrisk.service.InvestigationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * SIU Investigation REST endpoints.
 *
 *   POST   /api/investigations          → escalate a claim (creates row + notifies fraud team)
 *   GET    /api/investigations          → list all (or ?status=NEW)
 *   GET    /api/investigations/{id}     → single record
 *   GET    /api/investigations/claim/{claimId}
 *   PATCH  /api/investigations/{id}     → update status / assignee / notes
 *   DELETE /api/investigations/{id}     → admin cleanup
 *
 * Path-level auth is enforced at the api-gateway (FRAUD_ANALYST + ADMIN).
 * The "openedBy" attribution comes from the X-Auth-Username header that
 * the gateway injects after JWT validation (see AuthHeaderForwardFilter).
 */
@RestController
@RequestMapping("/api/investigations")
@RequiredArgsConstructor
public class InvestigationController {

    private final InvestigationService investigationService;

    @PostMapping
    public ResponseEntity<InvestigationDTO> open(
            @Valid @RequestBody CreateInvestigationRequestDTO request,
            HttpServletRequest httpRequest) {
        String actor = resolveActor(httpRequest);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(investigationService.open(request, actor));
    }

    @GetMapping
    public ResponseEntity<List<InvestigationDTO>> list(
            @RequestParam(required = false) InvestigationStatus status) {
        List<InvestigationDTO> data = (status != null)
                ? investigationService.findByStatus(status)
                : investigationService.findAll();
        return ResponseEntity.ok(data);
    }

    @GetMapping("/{id}")
    public ResponseEntity<InvestigationDTO> get(@PathVariable Long id) {
        return ResponseEntity.ok(investigationService.findById(id));
    }

    @GetMapping("/claim/{claimId}")
    public ResponseEntity<List<InvestigationDTO>> byClaim(@PathVariable String claimId) {
        return ResponseEntity.ok(investigationService.findByClaim(claimId));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<InvestigationDTO> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateInvestigationRequestDTO request) {
        return ResponseEntity.ok(investigationService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> delete(@PathVariable Long id) {
        investigationService.delete(id);
        return ResponseEntity.ok(Map.of("id", id, "deleted", true));
    }

    /**
     * Pulls the username forwarded by the gateway after JWT auth.
     * Falls back to "unknown" if the header is absent (only happens when
     * the service is hit directly, bypassing the gateway).
     */
    private String resolveActor(HttpServletRequest request) {
        String fromGateway = request.getHeader("X-Auth-Username");
        if (fromGateway != null && !fromGateway.isBlank()) return fromGateway;
        return "unknown";
    }
}
