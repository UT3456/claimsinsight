package com.claims.controller;

import com.claims.dto.request.ClaimReserveRequest;
import com.claims.dto.response.ClaimReserveResponse;
import com.claims.service.ClaimReserveServiceImpl;
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
@RequestMapping("/api/reserves")
@RequiredArgsConstructor
public class ClaimReserveController {

    private final ClaimReserveServiceImpl claimReserveServiceImpl;



    @PostMapping
    public ResponseEntity<ClaimReserveResponse> createClaimReserve(
            @Valid @RequestBody ClaimReserveRequest request) {
        ClaimReserveResponse response = claimReserveServiceImpl.createClaimReserve(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }




    @GetMapping("/{reserveId}")
    public ResponseEntity<ClaimReserveResponse> getClaimReserveById(@PathVariable Long reserveId) {
        return ResponseEntity.ok(claimReserveServiceImpl.getClaimReserveById(reserveId));
    }




    @GetMapping
    public ResponseEntity<List<ClaimReserveResponse>> getAllClaimReserves() {
        return ResponseEntity.ok(claimReserveServiceImpl.getAllClaimReserves());
    }



    @PutMapping("/{reserveId}")
    public ResponseEntity<ClaimReserveResponse> updateClaimReserve(
            @PathVariable Long reserveId,
            @Valid @RequestBody ClaimReserveRequest request) {
        return ResponseEntity.ok(claimReserveServiceImpl.updateClaimReserve(reserveId, request));
    }




    @DeleteMapping("/{reserveId}")
    public ResponseEntity<Map<String, String>> deleteClaimReserve(@PathVariable Long reserveId) {
        claimReserveServiceImpl.deleteClaimReserve(reserveId);
        return ResponseEntity.ok(Map.of(
                "message", "ClaimReserve deleted successfully",
                "reserveId", String.valueOf(reserveId)
        ));
    }




    @GetMapping("/claim/{claimId}")
    public ResponseEntity<List<ClaimReserveResponse>> getReservesByClaimId(
            @PathVariable String claimId) {
        return ResponseEntity.ok(claimReserveServiceImpl.getReservesByClaimId(claimId));
    }




    @GetMapping("/date-range")
    public ResponseEntity<List<ClaimReserveResponse>> getReservesByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(claimReserveServiceImpl.getReservesByDateRange(startDate, endDate));
    }




    @GetMapping("/claim/{claimId}/latest")
    public ResponseEntity<ClaimReserveResponse> getLatestReserveForClaim(
            @PathVariable String claimId) {
        return ResponseEntity.ok(claimReserveServiceImpl.getLatestReserveForClaim(claimId));
    }



    @GetMapping("/analytics/total")
    public ResponseEntity<Map<String, Object>> getTotalReserveAmount() {
        BigDecimal total = claimReserveServiceImpl.getTotalReserveAmount();
        return ResponseEntity.ok(Map.of("totalReserveAmount", total));
    }


    @GetMapping("/claim/{claimId}/history")
    public ResponseEntity<List<ClaimReserveResponse>> getReserveHistory(
            @PathVariable String claimId) {
        return ResponseEntity.ok(claimReserveServiceImpl.getReserveHistoryByClaimId(claimId));
    }



    @GetMapping("/analytics/summary")
    public ResponseEntity<Map<String, BigDecimal>> getLatestReserveSummaryAllClaims() {
        return ResponseEntity.ok(claimReserveServiceImpl.getLatestReserveSummaryAllClaims());
    }




    @GetMapping("/claim/{claimId}/trend/monthly")
    public ResponseEntity<Map<String, BigDecimal>> getMonthlyReserveTrend(
            @PathVariable String claimId) {
        return ResponseEntity.ok(claimReserveServiceImpl.getMonthlyReserveTrendByClaimId(claimId));
    }




    @GetMapping("/claim/{claimId}/adequacy")
    public ResponseEntity<Map<String, Object>> getReserveAdequacy(
            @PathVariable String claimId,
            @RequestParam BigDecimal totalCost) {
        return ResponseEntity.ok(claimReserveServiceImpl.getReserveAdequacyForClaim(claimId, totalCost));
    }
}