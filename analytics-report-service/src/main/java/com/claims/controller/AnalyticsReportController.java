package com.claims.controller;

import com.claims.client.AdjusterOperationsClient;
import com.claims.client.ClaimsMetricsServiceClient;
import com.claims.client.CostReserveAgingClient;
import com.claims.client.CostReserveServiceClient;
import com.claims.client.DenialLeakageServiceClient;
import com.claims.client.FraudRiskServiceClient;
import com.claims.client.dto.AdjusterPerformanceDTO;
import com.claims.client.dto.ClaimKpiDTO;
import com.claims.client.dto.ClaimReserveDTO;
import com.claims.client.dto.FraudRiskScoreDTO;
import com.claims.client.dto.LeakageFlagDTO;
import com.claims.dto.request.AnalyticsReportRequest;
import com.claims.dto.response.AnalyticsReportResponse;
import com.claims.enums.ReportScope;
import com.claims.service.AnalyticsReportServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.util.Objects;
import java.util.OptionalDouble;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

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
    private final AdjusterOperationsClient adjusterOperationsClient;
    private final CostReserveAgingClient costReserveAgingClient;
    private final ObjectMapper objectMapper;

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
     */
    @GetMapping("/analytics/count-by-scope")
    public ResponseEntity<Map<String, Long>> getReportCountByScope() {
        return ResponseEntity.ok(analyticsReportService.getReportCountByScope());
    }

    /**
     * GET /api/reports/analytics/count-by-scope-value
     */
    @GetMapping("/analytics/count-by-scope-value")
    public ResponseEntity<Map<String, Long>> getReportCountByScopeValue() {
        return ResponseEntity.ok(analyticsReportService.getReportCountByScopeValue());
    }

    /**
     * GET /api/reports/analytics/trend/monthly
     */
    @GetMapping("/analytics/trend/monthly")
    public ResponseEntity<Map<String, Long>> getMonthlyReportGenerationTrend() {
        return ResponseEntity.ok(analyticsReportService.getMonthlyReportGenerationTrend());
    }

    /**
     * GET /api/reports/analytics/most-active-generator
     */
    @GetMapping("/analytics/most-active-generator")
    public ResponseEntity<Map<String, Object>> getMostActiveReportGenerator() {
        return ResponseEntity.ok(analyticsReportService.getMostActiveReportGenerator());
    }

    /**
     * GET /api/reports/analytics/dashboard-summary
     */
    @GetMapping("/analytics/dashboard-summary")
    public ResponseEntity<Map<String, Object>> getReportDashboardSummary() {
        return ResponseEntity.ok(analyticsReportService.getReportDashboardSummary());
    }

    // -------------------------------------------------------
    // Claim-Level Aggregation
    // -------------------------------------------------------

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

    // -------------------------------------------------------
    // Portfolio Dashboard Endpoints
    // -------------------------------------------------------

    /**
     * GET /api/reports/portfolio/kpi-summary
     * Portfolio-wide KPI overview: total records, distinct claims, average value
     * per metric type (TAT, SEVERITY, SETTLEMENT_TIME).
     * Data sourced live from claims-metrics-service.
     */
    @GetMapping("/portfolio/kpi-summary")
    public ResponseEntity<Map<String, Object>> getPortfolioKpiSummary() {
        log.info("Building portfolio KPI summary");
        Map<String, Object> summary = new LinkedHashMap<>();
        try {
            List<ClaimKpiDTO> allKpis = claimsMetricsServiceClient.getAllKpis();
            summary.put("totalKpiRecords", allKpis.size());
            summary.put("distinctClaims", allKpis.stream()
                    .map(ClaimKpiDTO::getClaimId)
                    .filter(Objects::nonNull)
                    .distinct().count());

            // Average metric value per metric type
            Map<String, Double> avgByMetric = allKpis.stream()
                    .filter(k -> k.getMetricName() != null && k.getMetricValue() != null)
                    .collect(Collectors.groupingBy(
                            ClaimKpiDTO::getMetricName,
                            Collectors.averagingDouble(k -> k.getMetricValue().doubleValue())));
            summary.put("avgValueByMetric", avgByMetric);

            // Record count per metric type
            Map<String, Long> countByMetric = allKpis.stream()
                    .filter(k -> k.getMetricName() != null)
                    .collect(Collectors.groupingBy(ClaimKpiDTO::getMetricName, Collectors.counting()));
            summary.put("countByMetric", countByMetric);
        } catch (Exception ex) {
            log.warn("claims-metrics-service unavailable: {}", ex.getMessage());
            summary.put("error", "claims-metrics-service unavailable");
        }
        return ResponseEntity.ok(summary);
    }

    /**
     * GET /api/reports/portfolio/fraud-risk-summary
     * Portfolio-wide fraud risk overview: total scored, average score, max score,
     * and bucketed risk counts (high / medium / low).
     * Data sourced live from fraud-risk-service.
     */
    @GetMapping("/portfolio/fraud-risk-summary")
    public ResponseEntity<Map<String, Object>> getPortfolioFraudRiskSummary() {
        log.info("Building portfolio fraud risk summary");
        Map<String, Object> summary = new LinkedHashMap<>();
        try {
            List<FraudRiskScoreDTO> allScores = fraudRiskServiceClient.getAllRiskScores();
            summary.put("totalScored", allScores.size());

            if (!allScores.isEmpty()) {
                OptionalDouble avg = allScores.stream()
                        .filter(s -> s.getScoreValue() != null)
                        .mapToDouble(FraudRiskScoreDTO::getScoreValue).average();
                OptionalDouble max = allScores.stream()
                        .filter(s -> s.getScoreValue() != null)
                        .mapToDouble(FraudRiskScoreDTO::getScoreValue).max();

                long highRisk   = allScores.stream().filter(s -> s.getScoreValue() != null && s.getScoreValue() >= 80.0).count();
                long mediumRisk = allScores.stream().filter(s -> s.getScoreValue() != null && s.getScoreValue() >= 60.0 && s.getScoreValue() < 80.0).count();
                long lowRisk    = allScores.size() - highRisk - mediumRisk;

                summary.put("averageScore",           avg.isPresent() ? String.format("%.2f", avg.getAsDouble()) : "N/A");
                summary.put("maxScore",               max.isPresent() ? String.format("%.2f", max.getAsDouble()) : "N/A");
                summary.put("highRiskClaims_80Plus",  highRisk);
                summary.put("mediumRiskClaims_60_80", mediumRisk);
                summary.put("lowRiskClaims",          lowRisk);
            }
        } catch (Exception ex) {
            log.warn("fraud-risk-service unavailable: {}", ex.getMessage());
            summary.put("error", "fraud-risk-service unavailable");
        }
        return ResponseEntity.ok(summary);
    }

    /**
     * GET /api/reports/portfolio/leakage-summary
     * Delegates directly to denial-leakage-service's pre-aggregated GROUP BY summary.
     * Response: { totalFlags, totalEstimatedLoss, breakdown: [{leakageType, count, totalEstimatedLoss}] }
     */
    @GetMapping("/portfolio/leakage-summary")
    public ResponseEntity<Map<String, Object>> getPortfolioLeakageSummary() {
        log.info("Fetching portfolio leakage summary");
        Map<String, Object> result;
        try {
            result = denialLeakageServiceClient.getLeakageSummaryByType();
        } catch (Exception ex) {
            log.warn("denial-leakage-service unavailable: {}", ex.getMessage());
            result = new LinkedHashMap<>();
            result.put("error", "denial-leakage-service unavailable");
        }
        return ResponseEntity.ok(result);
    }

    /**
     * GET /api/reports/portfolio/aging-health
     * Delegates directly to cost-reserve-service's portfolio aging health endpoint.
     * Response: { totalClaims, averageAgingDays, criticalClaims_90Plus, bucketDistributionPercentage }
     */
    @GetMapping("/portfolio/aging-health")
    public ResponseEntity<Map<String, Object>> getPortfolioAgingHealth() {
        log.info("Fetching portfolio aging health");
        Map<String, Object> result;
        try {
            result = costReserveAgingClient.getPortfolioAgingHealth();
        } catch (Exception ex) {
            log.warn("cost-reserve-service unavailable: {}", ex.getMessage());
            result = new LinkedHashMap<>();
            result.put("error", "cost-reserve-service unavailable");
        }
        return ResponseEntity.ok(result);
    }

    /**
     * GET /api/reports/portfolio/adjuster-performance?period=2025-05
     * Portfolio-wide adjuster performance: all records with computed averages.
     * Data sourced live from AdjusterAndOperations service.
     *
     * @param period Optional period label (e.g. "2025-05"). Returns all periods if omitted.
     */
    @GetMapping("/portfolio/adjuster-performance")
    public ResponseEntity<Map<String, Object>> getPortfolioAdjusterPerformance(
            @RequestParam(required = false) String period) {
        log.info("Building portfolio adjuster performance summary, period={}", period);
        Map<String, Object> summary = new LinkedHashMap<>();
        try {
            List<AdjusterPerformanceDTO> performers =
                    adjusterOperationsClient.getAllAdjusterPerformance(period);
            summary.put("totalAdjusters", performers.size());

            if (!performers.isEmpty()) {
                OptionalDouble avgTat = performers.stream()
                        .filter(p -> p.getAvgTat() != null)
                        .mapToDouble(AdjusterPerformanceDTO::getAvgTat).average();
                OptionalDouble avgSlaCompliance = performers.stream()
                        .filter(p -> p.getSlaComplianceRate() != null)
                        .mapToDouble(AdjusterPerformanceDTO::getSlaComplianceRate).average();
                OptionalDouble avgPerformanceIndex = performers.stream()
                        .filter(p -> p.getPerformanceIndex() != null)
                        .mapToDouble(AdjusterPerformanceDTO::getPerformanceIndex).average();

                long trainingRequired = performers.stream()
                        .filter(p -> "TRAINING_REQUIRED".equals(p.getPerformanceFlag())).count();
                long satisfactory = performers.stream()
                        .filter(p -> "SATISFACTORY".equals(p.getPerformanceFlag())).count();

                summary.put("averageAvgTat",           avgTat.isPresent()           ? String.format("%.2f", avgTat.getAsDouble())           : "N/A");
                summary.put("averageSlaComplianceRate", avgSlaCompliance.isPresent() ? String.format("%.1f%%", avgSlaCompliance.getAsDouble()) : "N/A");
                summary.put("averagePerformanceIndex",  avgPerformanceIndex.isPresent() ? String.format("%.2f", avgPerformanceIndex.getAsDouble()) : "N/A");
                summary.put("trainingRequired",         trainingRequired);
                summary.put("satisfactory",             satisfactory);
            }
            summary.put("records", performers);
        } catch (Exception ex) {
            log.warn("AdjusterAndOperations unavailable: {}", ex.getMessage());
            summary.put("error", "AdjusterAndOperations service unavailable");
        }
        return ResponseEntity.ok(summary);
    }

    /**
     * GET /api/reports/portfolio/executive-summary
     * Single endpoint that aggregates live data from all five microservices plus
     * the local report store. Designed for the executive / operations dashboard.
     * Each section degrades gracefully if the upstream service is unavailable.
     */
    @GetMapping("/portfolio/executive-summary")
    public ResponseEntity<Map<String, Object>> getPortfolioExecutiveSummary() {
        log.info("Building portfolio executive summary (parallel fetch)");

        // ── Fan out all 5 upstream calls in parallel ─────────────────────────────
        // Each future catches its own exception and returns a safe fallback map,
        // so a single slow/down service never blocks the others.
        CompletableFuture<Map<String, Object>> kpiFuture = CompletableFuture.supplyAsync(() -> {
            try {
                List<ClaimKpiDTO> kpis = claimsMetricsServiceClient.getAllKpis();
                Map<String, Double> avgByMetric = kpis.stream()
                        .filter(k -> k.getMetricName() != null && k.getMetricValue() != null)
                        .collect(Collectors.groupingBy(ClaimKpiDTO::getMetricName,
                                Collectors.averagingDouble(k -> k.getMetricValue().doubleValue())));
                Map<String, Object> s = new LinkedHashMap<>();
                s.put("totalRecords",   kpis.size());
                s.put("distinctClaims", kpis.stream().map(ClaimKpiDTO::getClaimId).filter(Objects::nonNull).distinct().count());
                s.put("avgByMetric",    avgByMetric);
                return s;
            } catch (Exception ex) {
                log.warn("claims-metrics-service unavailable for executive summary");
                return Map.of("error", "unavailable");
            }
        });

        CompletableFuture<Map<String, Object>> fraudFuture = CompletableFuture.supplyAsync(() -> {
            try {
                List<FraudRiskScoreDTO> scores = fraudRiskServiceClient.getAllRiskScores();
                long highRisk = scores.stream().filter(s -> s.getScoreValue() != null && s.getScoreValue() >= 80.0).count();
                OptionalDouble avg = scores.stream().filter(s -> s.getScoreValue() != null)
                        .mapToDouble(FraudRiskScoreDTO::getScoreValue).average();
                Map<String, Object> s = new LinkedHashMap<>();
                s.put("totalScored",   scores.size());
                s.put("highRiskCount", highRisk);
                s.put("averageScore",  avg.isPresent() ? String.format("%.2f", avg.getAsDouble()) : "N/A");
                return s;
            } catch (Exception ex) {
                log.warn("fraud-risk-service unavailable for executive summary");
                return Map.of("error", "unavailable");
            }
        });

        CompletableFuture<Map<String, Object>> leakageFuture = CompletableFuture.supplyAsync(() -> {
            try {
                return denialLeakageServiceClient.getLeakageSummaryByType();
            } catch (Exception ex) {
                log.warn("denial-leakage-service unavailable for executive summary");
                return Map.of("error", "unavailable");
            }
        });

        CompletableFuture<Map<String, Object>> agingFuture = CompletableFuture.supplyAsync(() -> {
            try {
                return costReserveAgingClient.getPortfolioAgingHealth();
            } catch (Exception ex) {
                log.warn("cost-reserve-service unavailable for executive summary");
                return Map.of("error", "unavailable");
            }
        });

        CompletableFuture<Map<String, Object>> adjFuture = CompletableFuture.supplyAsync(() -> {
            try {
                List<AdjusterPerformanceDTO> performers = adjusterOperationsClient.getAllAdjusterPerformance(null);
                long trainingRequired = performers.stream()
                        .filter(p -> "TRAINING_REQUIRED".equals(p.getPerformanceFlag())).count();
                OptionalDouble avgSla = performers.stream()
                        .filter(p -> p.getSlaComplianceRate() != null)
                        .mapToDouble(AdjusterPerformanceDTO::getSlaComplianceRate).average();
                Map<String, Object> s = new LinkedHashMap<>();
                s.put("totalAdjusters",      performers.size());
                s.put("trainingRequired",     trainingRequired);
                s.put("avgSlaComplianceRate", avgSla.isPresent() ? String.format("%.1f%%", avgSla.getAsDouble()) : "N/A");
                return s;
            } catch (Exception ex) {
                log.warn("AdjusterAndOperations unavailable for executive summary");
                return Map.of("error", "unavailable");
            }
        });

        // Wait for all futures then assemble the response
        CompletableFuture.allOf(kpiFuture, fraudFuture, leakageFuture, agingFuture, adjFuture).join();

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("asOf",               LocalDate.now().toString());
        summary.put("kpiSummary",         kpiFuture.join());
        summary.put("fraudRiskSummary",   fraudFuture.join());
        summary.put("leakageSummary",     leakageFuture.join());
        summary.put("agingHealth",        agingFuture.join());
        summary.put("adjusterPerformance",adjFuture.join());

        // ── Local report store metadata (DB-local, no Feign — cheap) ────────────
        try {
            summary.put("reportStore", analyticsReportService.getReportDashboardSummary());
        } catch (Exception ex) {
            summary.put("reportStore", Map.of("error", "unavailable"));
        }

        return ResponseEntity.ok(summary);
    }

    // -------------------------------------------------------
    // Typed Report Generation
    // -------------------------------------------------------

    /**
     * POST /api/reports/generate/{reportType}?generatedBy=admin&period=2025-05
     *
     * <p>Fetches live data from the appropriate upstream service(s), serialises
     * the result as a JSON snapshot, and persists it as an {@code AnalyticsReport}
     * with {@code scope = PERIOD}.  Returns the saved report record.</p>
     *
     * <p>Supported {@code reportType} values:</p>
     * <ul>
     *   <li>{@code KPI_SUMMARY}          — claims-metrics-service</li>
     *   <li>{@code FRAUD_RISK}           — fraud-risk-service</li>
     *   <li>{@code LEAKAGE}              — denial-leakage-service</li>
     *   <li>{@code AGING_HEALTH}         — cost-reserve-service</li>
     *   <li>{@code ADJUSTER_PERFORMANCE} — AdjusterAndOperations</li>
     *   <li>{@code EXECUTIVE_SUMMARY}    — all five services combined</li>
     * </ul>
     */
    @PostMapping("/generate/{reportType}")
    public ResponseEntity<AnalyticsReportResponse> generateReport(
            @PathVariable String reportType,
            @RequestParam(defaultValue = "SYSTEM") String generatedBy,
            @RequestParam(required = false) String period) {

        log.info("Generating typed report: type={}, generatedBy={}, period={}", reportType, generatedBy, period);
        LocalDate today = LocalDate.now();
        String scopeLabel = (period != null && !period.isBlank())
                ? period
                : today.getYear() + "-" + String.format("%02d", today.getMonthValue());

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("reportType", reportType.toUpperCase());
        payload.put("period",     scopeLabel);

        String metrics;

        switch (reportType.toUpperCase()) {

            case "KPI_SUMMARY" -> {
                metrics = "TAT,SEVERITY,SETTLEMENT_TIME";
                try {
                    List<ClaimKpiDTO> kpis = claimsMetricsServiceClient.getAllKpis();
                    payload.put("totalKpiRecords", kpis.size());
                    payload.put("distinctClaims",  kpis.stream().map(ClaimKpiDTO::getClaimId).filter(Objects::nonNull).distinct().count());
                    payload.put("avgByMetric",     kpis.stream()
                            .filter(k -> k.getMetricName() != null && k.getMetricValue() != null)
                            .collect(Collectors.groupingBy(ClaimKpiDTO::getMetricName,
                                    Collectors.averagingDouble(k -> k.getMetricValue().doubleValue()))));
                } catch (Exception ex) {
                    log.warn("claims-metrics-service unavailable during KPI_SUMMARY generation: {}", ex.getMessage());
                    payload.put("kpiError", "claims-metrics-service unavailable");
                }
            }

            case "FRAUD_RISK" -> {
                metrics = "RISK_SCORE,HIGH_RISK_COUNT,AVG_SCORE";
                try {
                    List<FraudRiskScoreDTO> scores = fraudRiskServiceClient.getAllRiskScores();
                    OptionalDouble avg = scores.stream().filter(s -> s.getScoreValue() != null)
                            .mapToDouble(FraudRiskScoreDTO::getScoreValue).average();
                    long highRisk = scores.stream().filter(s -> s.getScoreValue() != null && s.getScoreValue() >= 80.0).count();
                    payload.put("totalScored",    scores.size());
                    payload.put("averageScore",   avg.isPresent() ? String.format("%.2f", avg.getAsDouble()) : "N/A");
                    payload.put("highRiskCount",  highRisk);
                } catch (Exception ex) {
                    log.warn("fraud-risk-service unavailable during FRAUD_RISK generation: {}", ex.getMessage());
                    payload.put("fraudError", "fraud-risk-service unavailable");
                }
            }

            case "LEAKAGE" -> {
                metrics = "TOTAL_FLAGS,TOTAL_LOSS,BREAKDOWN_BY_TYPE";
                try {
                    payload.putAll(denialLeakageServiceClient.getLeakageSummaryByType());
                } catch (Exception ex) {
                    log.warn("denial-leakage-service unavailable during LEAKAGE generation: {}", ex.getMessage());
                    payload.put("leakageError", "denial-leakage-service unavailable");
                }
            }

            case "AGING_HEALTH" -> {
                metrics = "TOTAL_CLAIMS,AVG_AGING_DAYS,CRITICAL_CLAIMS,BUCKET_DISTRIBUTION";
                try {
                    payload.putAll(costReserveAgingClient.getPortfolioAgingHealth());
                } catch (Exception ex) {
                    log.warn("cost-reserve-service unavailable during AGING_HEALTH generation: {}", ex.getMessage());
                    payload.put("agingError", "cost-reserve-service unavailable");
                }
            }

            case "ADJUSTER_PERFORMANCE" -> {
                metrics = "AVG_TAT,SLA_COMPLIANCE,TRAINING_REQUIRED,PERFORMANCE_INDEX";
                try {
                    List<AdjusterPerformanceDTO> ps =
                            adjusterOperationsClient.getAllAdjusterPerformance(period);
                    OptionalDouble avgSla = ps.stream().filter(p -> p.getSlaComplianceRate() != null)
                            .mapToDouble(AdjusterPerformanceDTO::getSlaComplianceRate).average();
                    long flagged = ps.stream()
                            .filter(p -> "TRAINING_REQUIRED".equals(p.getPerformanceFlag())).count();
                    payload.put("totalAdjusters",          ps.size());
                    payload.put("trainingRequired",        flagged);
                    payload.put("avgSlaComplianceRate",    avgSla.isPresent() ? String.format("%.1f%%", avgSla.getAsDouble()) : "N/A");
                } catch (Exception ex) {
                    log.warn("AdjusterAndOperations unavailable during ADJUSTER_PERFORMANCE generation: {}", ex.getMessage());
                    payload.put("adjusterError", "AdjusterAndOperations service unavailable");
                }
            }

            case "EXECUTIVE_SUMMARY" -> {
                metrics = "KPI,FRAUD_RISK,LEAKAGE,AGING,ADJUSTERS";
                try { payload.put("kpiRecords",    claimsMetricsServiceClient.getAllKpis().size()); }
                catch (Exception ignored) { payload.put("kpiRecords", "unavailable"); }

                try { payload.put("highRiskScores", fraudRiskServiceClient.getRiskScoresAboveThreshold(80.0).size()); }
                catch (Exception ignored) { payload.put("highRiskScores", "unavailable"); }

                try { payload.putAll(denialLeakageServiceClient.getLeakageSummaryByType()); }
                catch (Exception ignored) { payload.put("leakageSummary", "unavailable"); }

                try { payload.put("agingHealth", costReserveAgingClient.getPortfolioAgingHealth()); }
                catch (Exception ignored) { payload.put("agingHealth", "unavailable"); }

                try {
                    List<AdjusterPerformanceDTO> ps = adjusterOperationsClient.getAllAdjusterPerformance(null);
                    payload.put("totalAdjusters",    ps.size());
                    payload.put("trainingRequired",  ps.stream().filter(p -> "TRAINING_REQUIRED".equals(p.getPerformanceFlag())).count());
                } catch (Exception ignored) { payload.put("adjusterSummary", "unavailable"); }
            }

            default -> {
                metrics = "GENERIC";
                payload.put("warning", "Unknown reportType '" + reportType
                        + "'. Supported: KPI_SUMMARY, FRAUD_RISK, LEAKAGE, AGING_HEALTH, ADJUSTER_PERFORMANCE, EXECUTIVE_SUMMARY");
                log.warn("generateReport called with unknown reportType: {}", reportType);
            }
        }

        AnalyticsReportRequest req = new AnalyticsReportRequest();
        req.setScope(ReportScope.PERIOD);
        req.setScopeValue(scopeLabel);
        req.setMetrics(metrics);
        req.setGeneratedDate(today);
        req.setGeneratedBy(generatedBy);
        req.setReportData(toJson(payload));

        return new ResponseEntity<>(analyticsReportService.createReport(req), HttpStatus.CREATED);
    }

    // -------------------------------------------------------
    // Private Helpers
    // -------------------------------------------------------

    /**
     * Serialises the report payload map to a compact JSON string for storage
     * in {@code AnalyticsReport.reportData}.  Returns "{}" on any error so the
     * report record is always saved even if serialisation fails.
     */
    private String toJson(Map<String, Object> map) {
        try {
            return objectMapper.writeValueAsString(map);
        } catch (Exception e) {
            log.warn("Failed to serialise report payload to JSON: {}", e.getMessage());
            return "{}";
        }
    }
}
