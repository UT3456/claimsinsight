package com.claims.controller;

import com.claims.dto.request.AgingRecordRequest;
import com.claims.dto.response.AgingRecordResponse;
import com.claims.enums.AgingBucket;
import com.claims.service.AgingRecordServiceImpl;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/aging")
@RequiredArgsConstructor
public class AgingRecordController {

    private final AgingRecordServiceImpl agingRecordServiceImpl;





    @PostMapping
    public ResponseEntity<AgingRecordResponse> createAgingRecord(
            @Valid @RequestBody AgingRecordRequest request) {
        AgingRecordResponse response = agingRecordServiceImpl.createAgingRecord(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }



    @GetMapping("/{agingId}")
    public ResponseEntity<AgingRecordResponse> getAgingRecordById(@PathVariable Long agingId) {
        return ResponseEntity.ok(agingRecordServiceImpl.getAgingRecordById(agingId));
    }




    @GetMapping
    public ResponseEntity<List<AgingRecordResponse>> getAllAgingRecords() {
        return ResponseEntity.ok(agingRecordServiceImpl.getAllAgingRecords());
    }



    @PutMapping("/{agingId}")
    public ResponseEntity<AgingRecordResponse> updateAgingRecord(
            @PathVariable Long agingId,
            @Valid @RequestBody AgingRecordRequest request) {
        return ResponseEntity.ok(agingRecordServiceImpl.updateAgingRecord(agingId, request));
    }



    @DeleteMapping("/{agingId}")
    public ResponseEntity<Map<String, String>> deleteAgingRecord(@PathVariable Long agingId) {
        agingRecordServiceImpl.deleteAgingRecord(agingId);
        return ResponseEntity.ok(Map.of(
                "message", "AgingRecord deleted successfully",
                "agingId", String.valueOf(agingId)
        ));
    }



    @GetMapping("/claim/{claimId}")
    public ResponseEntity<List<AgingRecordResponse>> getAgingRecordsByClaimId(
            @PathVariable String claimId) {
        return ResponseEntity.ok(agingRecordServiceImpl.getAgingRecordsByClaimId(claimId));
    }



    @GetMapping("/bucket/{agingBucket}")
    public ResponseEntity<List<AgingRecordResponse>> getAgingRecordsByBucket(
            @PathVariable AgingBucket agingBucket) {
        return ResponseEntity.ok(agingRecordServiceImpl.getAgingRecordsByBucket(agingBucket));
    }





    @GetMapping("/escalation")
    public ResponseEntity<List<AgingRecordResponse>> getClaimsAgedBeyond(
            @RequestParam Integer days) {
        return ResponseEntity.ok(agingRecordServiceImpl.getClaimsAgedBeyond(days));
    }




    @GetMapping("/analytics/distribution")
    public ResponseEntity<Map<String, Long>> getAgingBucketDistribution() {
        return ResponseEntity.ok(agingRecordServiceImpl.getAgingBucketDistribution());
    }



    @GetMapping("/claim/{claimId}/summary")
    public ResponseEntity<Map<String, Object>> getAgingSummaryForClaim(
            @PathVariable String claimId) {
        return ResponseEntity.ok(agingRecordServiceImpl.getAgingSummaryForClaim(claimId));
    }




    @GetMapping("/analytics/portfolio-health")
    public ResponseEntity<Map<String, Object>> getPortfolioAgingHealth() {
        return ResponseEntity.ok(agingRecordServiceImpl.getPortfolioAgingHealth());
    }
}
