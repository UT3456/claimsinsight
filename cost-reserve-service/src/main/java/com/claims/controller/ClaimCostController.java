package com.claims.controller;

import com.claims.dto.request.ClaimCostRequest;
import com.claims.dto.response.ClaimCostResponse;
import com.claims.enums.CostType;
import com.claims.service.ClaimCostServiceImpl;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/costs")
@RequiredArgsConstructor
public class ClaimCostController {

    private final ClaimCostServiceImpl claimCostServiceImpl;




    @PostMapping
    public ResponseEntity<ClaimCostResponse> createClaimCost(
            @Valid @RequestBody ClaimCostRequest request) {
        ClaimCostResponse response = claimCostServiceImpl.createClaimCost(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }


    @GetMapping("/{costId}")
    public ResponseEntity<ClaimCostResponse> getClaimCostById(@PathVariable Long costId) {
        return ResponseEntity.ok(claimCostServiceImpl.getClaimCostById(costId));
    }


    @GetMapping
    public ResponseEntity<List<ClaimCostResponse>> getAllClaimCosts() {
        return ResponseEntity.ok(claimCostServiceImpl.getAllClaimCosts());
    }




    @PutMapping("/{costId}")
    public ResponseEntity<ClaimCostResponse> updateClaimCost(
            @PathVariable Long costId,
            @Valid @RequestBody ClaimCostRequest request) {
        return ResponseEntity.ok(claimCostServiceImpl.updateClaimCost(costId, request));
    }



    @DeleteMapping("/{costId}")
    public ResponseEntity<Map<String, String>> deleteClaimCost(@PathVariable Long costId) {
        claimCostServiceImpl.deleteClaimCost(costId);
        return ResponseEntity.ok(Map.of(
                "message", "ClaimCost deleted successfully",
                "costId", String.valueOf(costId)
        ));
    }



    @GetMapping("/claim/{claimId}")
    public ResponseEntity<List<ClaimCostResponse>> getCostsByClaimId(@PathVariable String claimId) {
        return ResponseEntity.ok(claimCostServiceImpl.getCostsByClaimId(claimId));
    }




    @GetMapping("/type/{costType}")
    public ResponseEntity<List<ClaimCostResponse>> getCostsByCostType(
            @PathVariable CostType costType) {
        return ResponseEntity.ok(claimCostServiceImpl.getCostsByCostType(costType));
    }



    @GetMapping("/claim/{claimId}/type/{costType}")
    public ResponseEntity<List<ClaimCostResponse>> getCostsByClaimIdAndType(
            @PathVariable String claimId,
            @PathVariable CostType costType) {
        return ResponseEntity.ok(claimCostServiceImpl.getCostsByClaimIdAndType(claimId, costType));
    }




    @GetMapping("/date-range")
    public ResponseEntity<List<ClaimCostResponse>> getCostsByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(claimCostServiceImpl.getCostsByDateRange(startDate, endDate));
    }




    @GetMapping("/claim/{claimId}/total")
    public ResponseEntity<Map<String, Object>> getTotalCostByClaimId(@PathVariable String claimId) {
        BigDecimal total = claimCostServiceImpl.getTotalCostByClaimId(claimId);
        return ResponseEntity.ok(Map.of(
                "claimId", claimId,
                "totalCost", total
        ));
    }


    @GetMapping("/claim/{claimId}/breakdown")
    public ResponseEntity<Map<String, BigDecimal>> getCostBreakdownByType(
            @PathVariable String claimId) {
        return ResponseEntity.ok(claimCostServiceImpl.getCostBreakdownByTypeForClaim(claimId));
    }




    @GetMapping("/summary/by-type")
    public ResponseEntity<Map<String, BigDecimal>> getOverallCostSummaryByType() {
        return ResponseEntity.ok(claimCostServiceImpl.getOverallCostSummaryByType());
    }



    @GetMapping("/claim/{claimId}/trend/monthly")
    public ResponseEntity<Map<String, BigDecimal>> getMonthlyCostTrend(
            @PathVariable String claimId) {
        return ResponseEntity.ok(claimCostServiceImpl.getMonthlyCostTrendByClaimId(claimId));
    }




    @GetMapping("/analytics/highest-cost-claim")
    public ResponseEntity<Map<String, Object>> getHighestCostClaim() {
        return ResponseEntity.ok(claimCostServiceImpl.getHighestCostClaim());
    }

    @PostMapping("/auto-initialize")
    public ResponseEntity<java.util.Map<String, Object>> autoInitialize(
            @RequestBody com.claims.dto.request.AutoInitializeRequest request) {
        java.util.Map<String, Object> result = claimCostServiceImpl.autoInitializeClaim(
                request.getClaimId(), request.getPayloadJson());
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }
}

