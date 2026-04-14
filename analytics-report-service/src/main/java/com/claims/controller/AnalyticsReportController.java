package com.claims.controller;

import com.claims.client.ClaimsMetricsServiceClient;
import com.claims.client.CostReserveServiceClient;
import com.claims.client.DenialLeakageServiceClient;
import com.claims.client.FraudRiskServiceClient;
import com.claims.client.dto.ClaimKpiDTO;
import com.claims.client.dto.ClaimReserveDTO;
import com.claims.client.dto.FraudRiskScoreDTO;
import com.claims.client.dto.LeakageFlagDTO;
import com.claims.dto.request.AnalyticsReportRequest;
import com.claims.dto.response.AnalyticsReportResponse;
import com.claims.enums.ReportScope;
import com.claims.service.AnalyticsReportServiceImpl;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@Slf4j
public class AnalyticsReportController {

    private final AnalyticsReportServiceImpl analyticsReportService;
    private final ClaimsMetricsServiceClient claimsMetricsServiceClient;
    private final FraudRiskServiceClient fraudRiskServiceClient;
    private final DenialLeakageServiceClient denialLeakageServiceClient;
    private final CostReserveServiceClient costReserveServiceClient;

    // -------------------------------------------------------
    // CRUD Endpoints
    // -------------------------------------------------------

    /**
     * POST /api/reports
     * Create a new analytics report.
     */
    @PostMapping
    public ResponseEntity<AnalyticsReportResponse> createReport(
            @Valid @RequestBody AnalyticsReportRequest request) {
        return new ResponseEntity<>(analyticsReportService.createReport(request), HttpStatus.CREATED);
    }

    /**
     * GET /api/reports/{reportId}
     * Get a single report by ID.
     */
    @GetMapping("/{reportId}")
    public ResponseEntity<AnalyticsReportResponse> getReportById(@PathVariable Long reportId) {
        return ResponseEntity.ok(analyticsReportService.getReportById(reportId));
    }

    /**
     * GET /api/reports
     * Get all reports in the system.
     */
    @GetMapping
    public ResponseEntity<List<AnalyticsReportResponse>> getAllReports() {
        return ResponseEntity.ok(analyticsReportService.getAllReports());
    }

    /**
     * PUT /api/reports/{reportId}
     * Update an existing report.
     */
    @PutMapping("/{reportId}")
    public ResponseEntity<AnalyticsReportResponse> updateReport(
            @PathVariable Long reportId,
            @Valid @RequestBody AnalyticsReportRequest request) {
        return ResponseEntity.ok(analyticsReportService.updateReport(reportId, request));
    }

    /**
     * DELETE /api/reports/{reportId}
     * Delete a report by ID.
     */
    @DeleteMapping("/{reportId}")
    public ResponseEntity<Map<String, String>> deleteReport(@PathVariable Long reportId) {
        analyticsReportService.deleteReport(reportId);
        return ResponseEntity.ok(Map.of(
                "message", "AnalyticsReport deleted successfully",
                "reportId", String.valueOf(reportId)
        ));
    }

    // -------------------------------------------------------
    // Query Endpoints
    // -------------------------------------------------------

    /**
     * GET /api/reports/scope/{scope}
     * Get all reports filtered by scope type.
     * Valid values: PRODUCT / REGION / CLAIM_TYPE / PERIOD
     */
    @GetMapping("/scope/{scope}")
    public ResponseEntity<List<AnalyticsReportResponse>> getReportsByScope(
            @PathVariable ReportScope scope) {
        return ResponseEntity.ok(analyticsReportService.getReportsByScope(scope));
    }

    /**
     * GET /api/reports/scope-value/{scopeValue}
     * Get all reports for a specific scope value (e.g. "North Region").
     */
    @GetMapping("/scope-value/{scopeValue}")
    public ResponseEntity<List<AnalyticsReportResponse>> getReportsByScopeValue(
            @PathVariable String scopeValue) {
        return ResponseEntity.ok(analyticsReportService.getReportsByScopeValue(scopeValue));
    }

    /**
     * GET /api/reports/scope/{scope}/scope-value/{scopeValue}
     * Get all reports matching both scope type and scope value.
     */
    @GetMapping("/scope/{scope}/scope-value/{scopeValue}")
    public ResponseEntity<List<AnalyticsReportResponse>> getReportsByScopeAndScopeValue(
            @PathVariable ReportScope scope,
            @PathVariable String scopeValue) {
        return ResponseEntity.ok(analyticsReportService.getReportsByScopeAndScopeValue(scope, scopeValue));
    }

    /**
     * GET /api/reports/date-range?startDate=2024-01-01&endDate=2024-06-30
     * Get all reports generated within a date range.
     */
    @GetMapping("/date-range")
    public ResponseEntity<List<AnalyticsReportResponse>> getReportsByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(analyticsReportService.getReportsByDateRange(startDate, endDate));
    }

    /**
     * GET /api/reports/generated-by/{generatedBy}
     * Get all reports generated by a specific user.
     */
    @GetMapping("/generated-by/{generatedBy}")
    public ResponseEntity<List<AnalyticsReportResponse>> getReportsByGeneratedBy(
            @PathVariable String generatedBy) {
        return ResponseEntity.ok(analyticsReportService.getReportsByGeneratedBy(generatedBy));
    }

    // -------------------------------------------------------
    // Analytics Endpoints
    // -------------------------------------------------------

    /**
     * GET /api/reports/latest?scope=REGION&scopeValue=North Region
     * Get the most recently generated report for a scope and scope value.
     */
    @GetMapping("/latest")
    public ResponseEntity<AnalyticsReportResponse> getLatestReport(
            @RequestParam ReportScope scope,
            @RequestParam String scopeValue) {
        return ResponseEntity.ok(analyticsReportService.getLatestReport(scope, scopeValue));
    }

    /**
     * GET /api/reports/analytics/count-by-scope
     * Get count of reports grouped by scope type.
     * Example: { "PRODUCT": 12, "REGION": 8 }
     */
    @GetMapping("/analytics/count-by-scope")
    public ResponseEntity<Map<String, Long>> getReportCountByScope() {
        return ResponseEntity.ok(analyticsReportService.getReportCountByScope());
    }

    /**
     * GET /api/reports/analytics/count-by-scope-value
     * Get count of reports grouped by scope value.
     * Example: { "North Region": 5, "Auto": 3 }
     */
    @GetMapping("/analytics/count-by-scope-value")
    public ResponseEntity<Map<String, Long>> getReportCountByScopeValue() {
        return ResponseEntity.ok(analyticsReportService.getReportCountByScopeValue());
    }

    /**
     * GET /api/reports/analytics/trend/monthly
     * Get the monthly report generation trend.
     * Example: { "2024-01": 5, "2024-02": 8 }
     */
    @GetMapping("/analytics/trend/monthly")
    public ResponseEntity<Map<String, Long>> getMonthlyReportGenerationTrend() {
        return ResponseEntity.ok(analyticsReportService.getMonthlyReportGenerationTrend());
    }

    /**
     * GET /api/reports/analytics/most-active-generator
     * Get the user who has generated the most reports.
     */
    @GetMapping("/analytics/most-active-generator")
    public ResponseEntity<Map<String, Object>> getMostActiveReportGenerator() {
        return ResponseEntity.ok(analyticsReportService.getMostActiveReportGenerator());
    }

    /**
     * GET /api/reports/analytics/dashboard-summary
     * Get a full dashboard summary — total reports, per scope count,
     * latest date, most active generator. Ideal for executive dashboards.
     */
    @GetMapping("/analytics/dashboard-summary")
    public ResponseEntity<Map<String, Object>> getReportDashboardSummary() {
        return ResponseEntity.ok(analyticsReportService.getReportDashboardSummary());
    }

    /**
     * GET /api/reports/claim-summary/{claimId}
     * Aggregates data from claims-metrics-service, fraud-risk-service,
     * denial-leakage-service and cost-reserve-service into a single claim dashboard.
     * Each section degrades gracefully if the upstream service is unavailable.
     */
    @GetMapping("/claim-summary/{claimId}")
    public ResponseEntity<Map<String, Object>> getClaimSummary(@PathVariable String claimId) {
        log.info("Building claim summary for claimId: {}", claimId);
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("claimId", claimId);

        // KPI metrics from claims-metrics-service
        try {
            List<ClaimKpiDTO> kpis = claimsMetricsServiceClient.getKpisByClaimId(claimId);
            summary.put("kpiMetrics", kpis);
        } catch (Exception ex) {
            log.warn("claims-metrics-service unavailable for claimId {}: {}", claimId, ex.getMessage());
            summary.put("kpiMetrics", Collections.emptyList());
        }

        // Fraud risk scores from fraud-risk-service
        try {
            List<FraudRiskScoreDTO> riskScores = fraudRiskServiceClient.getRiskScoresByClaimId(claimId);
            summary.put("fraudRiskScores", riskScores);
        } catch (Exception ex) {
            log.warn("fraud-risk-service unavailable for claimId {}: {}", claimId, ex.getMessage());
            summary.put("fraudRiskScores", Collections.emptyList());
        }

        // Leakage flags from denial-leakage-service
        try {
            List<LeakageFlagDTO> leakageFlags = denialLeakageServiceClient.getLeakageFlagsByClaimId(claimId);
            summary.put("leakageFlags", leakageFlags);
        } catch (Exception ex) {
            log.warn("denial-leakage-service unavailable for claimId {}: {}", claimId, ex.getMessage());
            summary.put("leakageFlags", Collections.emptyList());
        }

        // Reserve history from cost-reserve-service
        try {
            List<ClaimReserveDTO> reserves = costReserveServiceClient.getReserveHistoryByClaimId(claimId);
            summary.put("reserveHistory", reserves);
        } catch (Exception ex) {
            log.warn("cost-reserve-service unavailable for claimId {}: {}", claimId, ex.getMessage());
            summary.put("reserveHistory", Collections.emptyList());
        }

        return ResponseEntity.ok(summary);
    }
}
