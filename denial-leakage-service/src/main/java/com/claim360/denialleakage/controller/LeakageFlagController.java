package com.claim360.denialleakage.controller;

import com.claim360.denialleakage.dto.LeakageFlagRequest;
import com.claim360.denialleakage.dto.LeakageFlagResponse;
import com.claim360.denialleakage.enums.LeakageType;
import com.claim360.denialleakage.service.LeakageFlagService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/leakage-flags")
@RequiredArgsConstructor
public class LeakageFlagController {

    private final LeakageFlagService leakageFlagService;


    // Create a new leakage flag
    @PostMapping
    public ResponseEntity<LeakageFlagResponse> createLeakageFlag(
            @Valid @RequestBody LeakageFlagRequest request) {
        LeakageFlagResponse response = leakageFlagService.createLeakageFlag(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }


    // Get all leakage flags
    @GetMapping
    public ResponseEntity<List<LeakageFlagResponse>> getAllLeakageFlags() {
        List<LeakageFlagResponse> response = leakageFlagService.getAllLeakageFlags();
        return ResponseEntity.ok(response);
    }


    // Get a leakage flag by ID
    @GetMapping("/{id}")
    public ResponseEntity<LeakageFlagResponse> getLeakageFlagById(
            @PathVariable Long id) {
        LeakageFlagResponse response = leakageFlagService.getLeakageFlagById(id);
        return ResponseEntity.ok(response);
    }


    // Get all leakage flags for a specific claim
    @GetMapping("/claim/{claimId}")
    public ResponseEntity<List<LeakageFlagResponse>> getLeakageFlagsByClaimId(
            @PathVariable String claimId) {
        List<LeakageFlagResponse> response = leakageFlagService
                .getLeakageFlagsByClaimId(claimId);
        return ResponseEntity.ok(response);
    }


    // Get all leakage flags by leakage type
    @GetMapping("/type/{leakageType}")
    public ResponseEntity<List<LeakageFlagResponse>> getLeakageFlagsByLeakageType(
            @PathVariable LeakageType leakageType) {
        List<LeakageFlagResponse> response = leakageFlagService
                .getLeakageFlagsByLeakageType(leakageType);
        return ResponseEntity.ok(response);
    }


    // Get all leakage flags with estimated loss above amount
    @GetMapping("/loss/{amount}")
    public ResponseEntity<List<LeakageFlagResponse>> getLeakageFlagsByEstimatedLoss(
            @PathVariable Double amount) {
        List<LeakageFlagResponse> response = leakageFlagService
                .getLeakageFlagsByEstimatedLoss(amount);
        return ResponseEntity.ok(response);
    }


    // Get all leakage flags by claim and leakage type
    @GetMapping("/claim/{claimId}/type/{leakageType}")
    public ResponseEntity<List<LeakageFlagResponse>> getLeakageFlagsByClaimIdAndLeakageType(
            @PathVariable String claimId,
            @PathVariable LeakageType leakageType) {
        List<LeakageFlagResponse> response = leakageFlagService
                .getLeakageFlagsByClaimIdAndLeakageType(claimId, leakageType);
        return ResponseEntity.ok(response);
    }


    // Update a leakage flag by ID
    @PutMapping("/{id}")
    public ResponseEntity<LeakageFlagResponse> updateLeakageFlag(
            @PathVariable Long id,
            @Valid @RequestBody LeakageFlagRequest request) {
        LeakageFlagResponse response = leakageFlagService.updateLeakageFlag(id, request);
        return ResponseEntity.ok(response);
    }


    // Delete a leakage flag by ID
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteLeakageFlag(@PathVariable Long id) {
        leakageFlagService.deleteLeakageFlag(id);
        return ResponseEntity.ok("LeakageFlag with ID " + id + " deleted successfully.");
    }

    /**
     * Returns a dashboard-ready leakage rollup: total flag count, total estimated
     * loss, and a per-{@link LeakageType} breakdown. Backed by a single GROUP BY
     * aggregate query — suitable for direct consumption by the analytics dashboard.
     */
    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> getLeakageSummaryByType() {
        return ResponseEntity.ok(leakageFlagService.getLeakageSummaryByType());
    }
}