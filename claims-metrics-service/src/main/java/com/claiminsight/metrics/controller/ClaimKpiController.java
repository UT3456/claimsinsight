package com.claiminsight.metrics.controller;

import com.claiminsight.metrics.dto.ClaimKpiRequestDTO;
import com.claiminsight.metrics.dto.ClaimKpiResponseDTO;
import com.claiminsight.metrics.dto.KpiSummaryDTO;
import com.claiminsight.metrics.service.ClaimKpiService;
import com.claiminsight.metrics.service.KpiCalculationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/** REST controller for recording and querying KPI metrics for insurance claims. Base path: /api/kpis. */
@RestController
@RequestMapping("/api/kpis")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Claims Metrics Engine", description = "Record and query KPI metrics for insurance claims")
public class ClaimKpiController {
    private final ClaimKpiService claimKpiService;
    private final KpiCalculationService kpiCalculationService;
    
    /** Records a new KPI metric. Returns 201 on success. */
    @PostMapping
    @Operation(summary = "Record a new KPI metric for a claim")
    public ResponseEntity<ClaimKpiResponseDTO> createKpi(
            @Valid @RequestBody ClaimKpiRequestDTO requestDTO) {
        log.info("POST /api/kpis — claimId: {}, metric: {}",
                requestDTO.getClaimId(), requestDTO.getMetricName());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(claimKpiService.createKpi(requestDTO));
    }
    
    /** Returns all KPI records. */
    @GetMapping
    @Operation(summary = "Get all KPI records")
    public ResponseEntity<List<ClaimKpiResponseDTO>> getAllKpis() {
        log.info("GET /api/kpis");
        return ResponseEntity.ok(claimKpiService.getAllKpis());
    }
    
    /** Returns a KPI record by its ID. Returns 404 if not found. */
    @GetMapping("/{kpiId}")
    @Operation(summary = "Get a KPI record by its ID")
    public ResponseEntity<ClaimKpiResponseDTO> getKpiById(@PathVariable Long kpiId) {
        log.info("GET /api/kpis/{}", kpiId);
        return ResponseEntity.ok(claimKpiService.getKpiById(kpiId));
    }
    
    /** Returns all KPIs for a specific claim ID. */
    @GetMapping("/claim/{claimId}")
    @Operation(summary = "Get all KPI records for a specific claim")
    public ResponseEntity<List<ClaimKpiResponseDTO>> getKpisByClaimId(
            @PathVariable String claimId) {
        log.info("GET /api/kpis/claim/{}", claimId);
        return ResponseEntity.ok(claimKpiService.getKpisByClaimId(claimId));
    }
    
    /** Returns all KPIs of a specific metric type. */
    @GetMapping("/metric/{metricName}")
    @Operation(summary = "Get all KPI records for a specific metric type")
    public ResponseEntity<List<ClaimKpiResponseDTO>> getKpisByMetricName(
            @PathVariable String metricName) {
        log.info("GET /api/kpis/metric/{}", metricName);
        return ResponseEntity.ok(claimKpiService.getKpisByMetricName(metricName));
    }
    
    /** Returns KPIs filtered by both claimId and metric type. */
    @GetMapping("/claim/{claimId}/metric/{metricName}")
    @Operation(summary = "Get KPI records for a specific claim and metric type")
    public ResponseEntity<List<ClaimKpiResponseDTO>> getKpisByClaimIdAndMetricName(
            @PathVariable String claimId,
            @PathVariable String metricName) {
        log.info("GET /api/kpis/claim/{}/metric/{}", claimId, metricName);
        return ResponseEntity.ok(
                claimKpiService.getKpisByClaimIdAndMetricName(claimId, metricName));
    }
    
    /** Returns KPIs whose metric date falls within the given range. */
    @GetMapping("/date-range")
    @Operation(summary = "Get KPI records within a date range (format: YYYY-MM-DD)")
    public ResponseEntity<List<ClaimKpiResponseDTO>> getKpisByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        log.info("GET /api/kpis/date-range?start={}&end={}", start, end);
        return ResponseEntity.ok(claimKpiService.getKpisByDateRange(start, end));
    }
    
    /** Calculates all 5 KPIs for a claim from its raw ingestion data and saves them. */
    @PostMapping("/calculate/{claimId}")
    @Operation(summary = "Calculate TAT, CYCLE_TIME, SEVERITY, FREQUENCY, LOSS_RATIO for a claim")
    public ResponseEntity<KpiSummaryDTO> calculateKpis(@PathVariable String claimId) {
        log.info("POST /api/kpis/calculate/{}", claimId);
        return ResponseEntity.status(HttpStatus.CREATED).body(kpiCalculationService.calculateAndSave(claimId));
    }

    /** Deletes a KPI record by its ID. Returns 204 on success. */
    @DeleteMapping("/{kpiId}")
    @Operation(summary = "Delete a KPI record by its ID")
    public ResponseEntity<Void> deleteKpi(@PathVariable Long kpiId) {
        log.info("DELETE /api/kpis/{}", kpiId);
        claimKpiService.deleteKpi(kpiId);
        return ResponseEntity.noContent().build();
    }
}
