package com.claim360.denialleakage.controller;

import com.claim360.denialleakage.dto.AutoAnalyzeRequest;
import com.claim360.denialleakage.dto.DenialPatternRequest;
import com.claim360.denialleakage.dto.DenialPatternResponse;
import com.claim360.denialleakage.service.DenialPatternService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/denial-patterns")
@RequiredArgsConstructor
public class DenialPatternController {

    private final DenialPatternService denialPatternService;

    // Create
    @PostMapping
    public ResponseEntity<DenialPatternResponse> createDenialPattern(
            @Valid @RequestBody DenialPatternRequest request) {
        DenialPatternResponse response = denialPatternService.createDenialPattern(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    // Get all denial patterns
    @GetMapping
    public ResponseEntity<List<DenialPatternResponse>> getAllDenialPatterns() {
        List<DenialPatternResponse> response = denialPatternService.getAllDenialPatterns();
        return ResponseEntity.ok(response);
    }


    // Get a denial pattern by ID
    @GetMapping("/{id}")
    public ResponseEntity<DenialPatternResponse> getDenialPatternById(
            @PathVariable Long id) {
        DenialPatternResponse response = denialPatternService.getDenialPatternById(id);
        return ResponseEntity.ok(response);
    }


    // Get all denial patterns for a specific claim
    @GetMapping("/claim/{claimId}")
    public ResponseEntity<List<DenialPatternResponse>> getDenialPatternsByClaimId(
            @PathVariable String claimId) {
        List<DenialPatternResponse> response = denialPatternService
                .getDenialPatternsByClaimId(claimId);
        return ResponseEntity.ok(response);
    }


    // Get all denial patterns by denial code
    @GetMapping("/code/{denialCode}")
    public ResponseEntity<List<DenialPatternResponse>> getDenialPatternsByDenialCode(
            @PathVariable String denialCode) {
        List<DenialPatternResponse> response = denialPatternService
                .getDenialPatternsByDenialCode(denialCode);
        return ResponseEntity.ok(response);
    }


    // Search denial patterns by reason keyword
    @GetMapping("/search/{keyword}")
    public ResponseEntity<List<DenialPatternResponse>> getDenialPatternsByReasonKeyword(
            @PathVariable String keyword) {
        List<DenialPatternResponse> response = denialPatternService
                .getDenialPatternsByReasonKeyword(keyword);
        return ResponseEntity.ok(response);
    }


    // Get all denial patterns by claim and denial code
    @GetMapping("/claim/{claimId}/code/{denialCode}")
    public ResponseEntity<List<DenialPatternResponse>> getDenialPatternsByClaimIdAndDenialCode(
            @PathVariable String claimId,
            @PathVariable String denialCode) {
        List<DenialPatternResponse> response = denialPatternService
                .getDenialPatternsByClaimIdAndDenialCode(claimId, denialCode);
        return ResponseEntity.ok(response);
    }


    // Update a denial pattern by ID
    @PutMapping("/{id}")
    public ResponseEntity<DenialPatternResponse> updateDenialPattern(
            @PathVariable Long id,
            @Valid @RequestBody DenialPatternRequest request) {
        DenialPatternResponse response = denialPatternService.updateDenialPattern(id, request);
        return ResponseEntity.ok(response);
    }


    // Delete a denial pattern by ID
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteDenialPattern(@PathVariable Long id) {
        denialPatternService.deleteDenialPattern(id);
        return ResponseEntity.ok("DenialPattern with ID " + id + " deleted successfully.");
    }

    // Auto-analyze denial patterns and leakage (called by data-ingestion-service)
    @PostMapping("/auto-analyze")
    public ResponseEntity<java.util.Map<String, Object>> autoAnalyze(
            @RequestBody AutoAnalyzeRequest request) {
        java.util.Map<String, Object> result = denialPatternService.autoAnalyzeDenial(
                request.getClaimId(), request.getPayloadJson());
        return ResponseEntity.status(org.springframework.http.HttpStatus.OK).body(result);
    }
}