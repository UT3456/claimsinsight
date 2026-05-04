package com.demo.controller;

import com.demo.dto.AutoGenerateSlaRequest;
import com.demo.dto.SLAViolationDTO;
import com.demo.service.SLAViolationService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;

/**
 * REST Controller for managing SLA (Service Level Agreement) Violations.
 */
@RestController
@RequestMapping("/api/sla-violations")
public class SLAViolationController {

    private final SLAViolationService service;

    public SLAViolationController(SLAViolationService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<SLAViolationDTO>> getAllViolations() {
        List<SLAViolationDTO> list = service.getAllViolations();
        return ResponseEntity.ok(list);
    }

    /**
     * Auto-generates a LOW-severity SLA tracking violation for a claim that was
     * just ingested via data-ingestion-service. Called by that service's Feign
     * client — not intended for manual use from the UI.
     */
    @PostMapping("/auto-generate")
    public ResponseEntity<SLAViolationDTO> autoGenerate(@RequestBody AutoGenerateSlaRequest request) {
        SLAViolationDTO result = service.autoGenerateSlaViolation(
                request.getClaimRef(),
                request.getAdjusterId(),
                request.getActualDays(),
                request.getSlaTargetDays());
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @PostMapping
    public ResponseEntity<String> recordViolation(@Valid @RequestBody SLAViolationDTO dto) {
        SLAViolationDTO savedDto = service.recordSLAViolation(dto);
        if (savedDto != null) {
            return new ResponseEntity<>("SLA Violation Recorded Successfully", HttpStatus.CREATED);
        } else {
            return new ResponseEntity<>("SLA Violation Recording Failed", HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("/adjuster/{id}")
    public ResponseEntity<List<SLAViolationDTO>> getByAdjuster(@PathVariable Long id) {
        List<SLAViolationDTO> list = service.getSLAViolationsByAdjuster(id);
        if (!list.isEmpty()) {
            return new ResponseEntity<>(list, HttpStatus.OK);
        } else {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping("/adjuster/{id}/date-range")
    public ResponseEntity<List<SLAViolationDTO>> getByAdjusterAndDateRange(@PathVariable Long id, @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date start, @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date end) {
        List<SLAViolationDTO> list = service.getSLAViolationsByAdjusterAndDateRange(id, start, end);
        if (!list.isEmpty()) {
            return new ResponseEntity<>(list, HttpStatus.OK);
        } else {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }
    }

    @GetMapping("/adjuster/{id}/escalations")
    public ResponseEntity<List<SLAViolationDTO>> getEscalations(@PathVariable Long id) {
        List<SLAViolationDTO> list = service.getEscalationCandidatesByAdjuster(id);
        if (!list.isEmpty()) {
            return new ResponseEntity<>(list, HttpStatus.OK);
        } else {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }
    }

    @GetMapping("/adjuster/{id}/count")
    public ResponseEntity<Long> countByAdjusterAndPeriod(
            @PathVariable Long id,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date start,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date end) {
        Long count = service.countViolationsByAdjusterAndPeriod(id, start, end);
        if (count != null && count >= 0) {
            return new ResponseEntity<>(count, HttpStatus.OK);
        } else {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping("/claim/{claimId}")
    public ResponseEntity<List<SLAViolationDTO>> getByClaim(@PathVariable Long claimId) {
        List<SLAViolationDTO> list = service.getSLAViolationsByClaim(claimId);
        if (!list.isEmpty()) {
            return new ResponseEntity<>(list, HttpStatus.OK);
        } else {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping("/claim/{claimId}/total-overdue")
    public ResponseEntity<Integer> getTotalOverdue(@PathVariable Long claimId) {
        Integer total = service.getTotalDaysOverdueByClaim(claimId);
        if (total != null) {
            return new ResponseEntity<>(total, HttpStatus.OK);
        } else {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping("/type")
    public ResponseEntity<List<SLAViolationDTO>> getByType(@RequestParam String violationType) {
        List<SLAViolationDTO> list = service.getViolationsByType(violationType);
        if (!list.isEmpty()) {
            return new ResponseEntity<>(list, HttpStatus.OK);
        } else {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }
    }

    @GetMapping("/severity")
    public ResponseEntity<List<SLAViolationDTO>> getBySeverity(@RequestParam String level) {
        List<SLAViolationDTO> list = service.getViolationsBySeverity(level);
        if (!list.isEmpty()) {
            return new ResponseEntity<>(list, HttpStatus.OK);
        } else {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }
    }

    @GetMapping("/overdue")
    public ResponseEntity<List<SLAViolationDTO>> getByDaysOverdue(@RequestParam int days) {
        List<SLAViolationDTO> list = service.getViolationsByDaysOverdueGreaterThan(days);
        if (!list.isEmpty()) {
            return new ResponseEntity<>(list, HttpStatus.OK);
        } else {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }
    }

    @GetMapping("/date-range")
    public ResponseEntity<List<SLAViolationDTO>> getByDateRange(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date start,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date end) {
        List<SLAViolationDTO> list = service.getViolationsByDateRange(start, end);
        if (!list.isEmpty()) {
            return new ResponseEntity<>(list, HttpStatus.OK);
        } else {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }
    }

    @PutMapping("/{violationId}")
    public ResponseEntity<SLAViolationDTO> updateViolation(@PathVariable Long violationId, @Valid @RequestBody SLAViolationDTO dto) {
        SLAViolationDTO updated = service.updateSLAViolation(violationId, dto);
        if (updated != null) {
            return ResponseEntity.ok(updated);
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @PatchMapping("/{violationId}/escalate")
    public ResponseEntity<SLAViolationDTO> toggleEscalation(@PathVariable Long violationId, @RequestParam boolean escalated) {

        SLAViolationDTO updated = service.updateEscalationStatus(violationId, escalated);
        return ResponseEntity.ok(updated);
    }

    @PatchMapping("/{violationId}/severity")
    public ResponseEntity<SLAViolationDTO> updateSeverity(@PathVariable Long violationId, @RequestParam String newLevel) {

        SLAViolationDTO updated = service.changeViolationSeverity(violationId, newLevel);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{violationId}")
    public ResponseEntity<String> deleteViolation(@PathVariable Long violationId) {
        try {
            service.deleteViolation(violationId);
            return new ResponseEntity<>("SLA Violation Deleted Successfully", HttpStatus.ACCEPTED);
        } catch (Exception e) {
            return new ResponseEntity<>("SLA Violation Deletion Failed", HttpStatus.NOT_FOUND);
        }
    }
}