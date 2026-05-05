package com.claiminsight.metrics.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.claiminsight.metrics.client.DataIngestionClient;
import com.claiminsight.metrics.client.NotificationServiceClient;
import com.claiminsight.metrics.client.dto.ClaimRawDTO;
import com.claiminsight.metrics.client.dto.NotificationRequestDTO;
import com.claiminsight.metrics.client.dto.NotificationDispatchRequestDTO;
import com.claiminsight.metrics.dto.ClaimKpiResponseDTO;
import com.claiminsight.metrics.dto.KpiSummaryDTO;
import com.claiminsight.metrics.exception.ResourceNotFoundException;
import com.claiminsight.metrics.mapper.ClaimKpiMapper;
import com.claiminsight.metrics.model.ClaimKPI;
import com.claiminsight.metrics.model.MetricName;
import com.claiminsight.metrics.repository.ClaimKpiRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Computes and saves all 5 KPI metrics for a given claim.
 * Raw claim data is fetched from data-ingestion-service via Feign Client
 * instead of directly querying the shared database.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class KpiCalculationService {

    /** Operational KPI thresholds — breaching any triggers a PERFORMANCE notification. */
    private static final BigDecimal TAT_THRESHOLD_DAYS         = BigDecimal.valueOf(30);
    private static final BigDecimal CYCLE_TIME_THRESHOLD_DAYS  = BigDecimal.valueOf(60);
    private static final BigDecimal LOSS_RATIO_THRESHOLD       = BigDecimal.ONE; // 1.0 = claims exceed premiums
    private static final BigDecimal SEVERITY_THRESHOLD         = BigDecimal.valueOf(8); // scale 1-10
    private static final Set<String> PERFORMANCE_ALERT_ROLES   = Set.of("MANAGER", "EXECUTIVE");

    private final DataIngestionClient dataIngestionClient;
    private final ClaimKpiRepository  claimKpiRepository;
    private final ClaimKpiMapper      claimKpiMapper;
    private final ObjectMapper        objectMapper;

    /** Optional: null when NotificationService is unreachable. Notifications are non-blocking. */
    @Autowired(required = false)
    private NotificationServiceClient notificationServiceClient;

    /**
     * Fetches raw claim records from data-ingestion-service via Feign,
     * calculates all 5 KPIs and saves them to the database.
     */
    @CacheEvict(value = "kpis", allEntries = true)
    public KpiSummaryDTO calculateAndSave(String claimId) {

        log.info("Fetching raw claim data for claimId: {} via Feign", claimId);
        List<ClaimRawDTO> records = dataIngestionClient.getRawClaimsByClaimId(claimId);

        if (records == null || records.isEmpty()) {
            throw new ResourceNotFoundException("No raw claim records found for claimId: " + claimId);
        }

        log.info("Calculating KPIs for claimId: {} ({} raw records)", claimId, records.size());

        LocalDate today         = LocalDate.now();
        LocalDate calculatedDate = today;

        // ── FREQUENCY ────────────────────────────────────────────────────
        BigDecimal frequency = BigDecimal.valueOf(records.size());

        // ── CYCLE_TIME ────────────────────────────────────────────────────
        LocalDate firstIngested = records.stream()
                .map(r -> r.getIngestedDate().toLocalDate())
                .min(Comparator.naturalOrder())
                .orElse(today);

        LocalDate lastIngested = records.stream()
                .map(r -> r.getIngestedDate().toLocalDate())
                .max(Comparator.naturalOrder())
                .orElse(today);

        long cycleTimeDays = ChronoUnit.DAYS.between(firstIngested, lastIngested);
        BigDecimal cycleTime = BigDecimal.valueOf(Math.max(1, cycleTimeDays));

        // ── TAT (Turnaround Time) ────────────────────────────────────────
        // Start: when the claim was officially filed (filedDate preferred, then incidentDate)
        // End  : when the claim was closed/settled; falls back to today for still-open claims
        String payloadJson0 = records.get(0).getPayloadJson();
        LocalDate tatStart = extractDate(payloadJson0,
                new String[]{"filedDate", "incidentDate", "admissionDate"}, firstIngested);
        LocalDate tatEnd   = extractDate(payloadJson0,
                new String[]{"closedDate", "settlementDate", "resolvedDate", "closureDate"}, today);
        long tatDays = ChronoUnit.DAYS.between(tatStart, tatEnd);
        BigDecimal tat = BigDecimal.valueOf(Math.max(1, tatDays));

        // ── SEVERITY ────────────────────────────────────────────────────
        // Portfolio-relative: z-score vs existing SEVERITY KPIs (fallback to fixed scale).
        BigDecimal claimAmount = extractClaimAmount(payloadJson0);
        BigDecimal severity = computePortfolioRelativeSeverity(claimAmount);

        // ── LOSS RATIO ───────────────────────────────────────────────────
        BigDecimal premium = extractPremium(payloadJson0);
        BigDecimal lossRatio = premium.compareTo(BigDecimal.ZERO) > 0
                ? claimAmount.divide(premium, 4, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        // ── SETTLEMENT TIME ──────────────────────────────────────────────
        // Days from filing/incident date to settlement/closure; falls back to cycleTime.
        LocalDate settlementDate = extractDate(payloadJson0,
                new String[]{"settlementDate", "closedDate", "resolvedDate", "closureDate"}, null);
        BigDecimal settlementTime;
        if (settlementDate != null) {
            long settlementDays = ChronoUnit.DAYS.between(tatStart, settlementDate);
            settlementTime = BigDecimal.valueOf(Math.max(1, settlementDays));
        } else {
            settlementTime = cycleTime; // use cycle time as proxy when closure date is absent
        }

        // ── SAVE ALL 6 KPIs in a single batch INSERT ─────────────────────
        List<ClaimKPI> kpiEntities = List.of(
            buildKpi(claimId, MetricName.TAT,             tat,            calculatedDate),
            buildKpi(claimId, MetricName.CYCLE_TIME,      cycleTime,      calculatedDate),
            buildKpi(claimId, MetricName.SEVERITY,        severity,       calculatedDate),
            buildKpi(claimId, MetricName.FREQUENCY,       frequency,      calculatedDate),
            buildKpi(claimId, MetricName.LOSS_RATIO,      lossRatio,      calculatedDate),
            buildKpi(claimId, MetricName.SETTLEMENT_TIME, settlementTime, calculatedDate)
        );
        List<ClaimKPI> saved = claimKpiRepository.saveAll(kpiEntities);
        List<ClaimKpiResponseDTO> savedKpis = saved.stream()
                .map(claimKpiMapper::toResponseDTO)
                .collect(Collectors.toList());

        log.info("KPIs saved for claimId: {} — TAT={}, CYCLE_TIME={}, SEVERITY={}, FREQUENCY={}, LOSS_RATIO={}, SETTLEMENT_TIME={}",
                claimId, tat, cycleTime, severity, frequency, lossRatio, settlementTime);

        checkThresholdsAndNotify(claimId, tat, cycleTime, severity, lossRatio);

        return new KpiSummaryDTO(claimId, tat, cycleTime, severity, frequency, lossRatio, settlementTime, calculatedDate, savedKpis);
    }

    /**
     * Inspects the freshly-computed KPIs and fires a single PERFORMANCE notification
     * when one or more operational thresholds are breached. A consolidated alert
     * is preferred over per-metric spam.
     */
    private void checkThresholdsAndNotify(String claimId, BigDecimal tat, BigDecimal cycleTime,
                                          BigDecimal severity, BigDecimal lossRatio) {
        if (notificationServiceClient == null) return;

        List<String> breaches = new ArrayList<>();
        if (tat.compareTo(TAT_THRESHOLD_DAYS) > 0) {
            breaches.add("TAT " + tat + "d > " + TAT_THRESHOLD_DAYS + "d");
        }
        if (cycleTime.compareTo(CYCLE_TIME_THRESHOLD_DAYS) > 0) {
            breaches.add("Cycle time " + cycleTime + "d > " + CYCLE_TIME_THRESHOLD_DAYS + "d");
        }
        if (lossRatio.compareTo(LOSS_RATIO_THRESHOLD) > 0) {
            breaches.add("Loss ratio " + lossRatio + " > " + LOSS_RATIO_THRESHOLD);
        }
        if (severity.compareTo(SEVERITY_THRESHOLD) >= 0) {
            breaches.add("Severity " + severity + " ≥ " + SEVERITY_THRESHOLD);
        }

        if (breaches.isEmpty()) return;

        try {
            NotificationDispatchRequestDTO notification = NotificationDispatchRequestDTO.builder()
                    .targetRoles(PERFORMANCE_ALERT_ROLES)
                    .title("KPI Threshold Breach - Claim " + claimId)
                    .message("Performance thresholds breached on claim " + claimId + ": "
                            + String.join("; ", breaches) + ". Review recommended.")
                    .category("PERFORMANCE")
                    .referenceId(claimId)
                    .build();
            notificationServiceClient.dispatchNotification(notification);
            log.info("Sent PERFORMANCE notification for claim {} to roles {} ({} breach(es))",
                    claimId, PERFORMANCE_ALERT_ROLES, breaches.size());
        } catch (Exception e) {
            log.warn("Failed to send PERFORMANCE notification for claim {}: {}", claimId, e.getMessage());
        }
    }

    /** Builds a KPI entity without persisting it — used for batch saveAll(). */
    private ClaimKPI buildKpi(String claimId, MetricName name, BigDecimal value, LocalDate date) {
        ClaimKPI kpi = new ClaimKPI();
        kpi.setClaimId(claimId);
        kpi.setMetricName(name);
        kpi.setMetricValue(value);
        kpi.setMetricDate(date);
        return kpi;
    }

    // ── Nightly KPI Recompute ────────────────────────────────────────────────────

    /**
     * Recomputes KPIs for every claim that has had at least one KPI record.
     * Runs nightly at 02:00 so dashboards never serve stale data.
     * Each claim recalculates independently; a failure on one claim is logged and
     * skipped without aborting the rest of the batch.
     */
    @Scheduled(cron = "0 0 2 * * *")
    @CacheEvict(value = "kpis", allEntries = true)
    public void recalculateAllKpisNightly() {
        List<String> claimIds = claimKpiRepository.findAllDistinctClaimIds();
        if (claimIds.isEmpty()) {
            log.info("Nightly KPI recompute: no claims found — skipping.");
            return;
        }
        log.info("Nightly KPI recompute starting for {} claim(s)...", claimIds.size());
        int success = 0, failed = 0;
        for (String claimId : claimIds) {
            try {
                calculateAndSave(claimId);
                success++;
            } catch (Exception e) {
                log.warn("Nightly KPI recompute failed for claimId {}: {}", claimId, e.getMessage());
                failed++;
            }
        }
        log.info("Nightly KPI recompute complete: {} succeeded, {} failed (total={})",
                success, failed, claimIds.size());
    }

    // ── Payload extraction helpers ───────────────────────────────────────────────

    /**
     * Generic date extractor: tries each field name in order and returns the first
     * successfully parsed {@link LocalDate}. Falls back to {@code fallback} when none
     * of the fields are present or parseable. All field values must be ISO-8601 dates.
     */
    private LocalDate extractDate(String payloadJson, String[] fields, LocalDate fallback) {
        try {
            JsonNode node = objectMapper.readTree(payloadJson);
            for (String field : fields) {
                JsonNode n = node.get(field);
                if (n != null && !n.isNull()) {
                    String text = n.asText();
                    if (!text.isEmpty()) return LocalDate.parse(text);
                }
            }
        } catch (Exception e) {
            log.warn("Could not parse date fields {} from payload, using fallback", java.util.Arrays.toString(fields));
        }
        return fallback;
    }

    private BigDecimal extractClaimAmount(String payloadJson) {
        try {
            JsonNode node = objectMapper.readTree(payloadJson);
            for (String field : new String[]{"claimAmount", "totalClaim", "amount"}) {
                JsonNode n = node.get(field);
                if (n != null && !n.isNull()) return BigDecimal.valueOf(n.asDouble());
            }
        } catch (Exception e) {
            log.warn("Could not parse claimAmount from payload");
        }
        return BigDecimal.ZERO;
    }

    private BigDecimal extractPremium(String payloadJson) {
        try {
            JsonNode node = objectMapper.readTree(payloadJson);
            JsonNode n = node.get("premium");
            if (n != null && !n.isNull()) return BigDecimal.valueOf(n.asDouble());
        } catch (Exception e) {
            log.warn("Could not parse premium from payload");
        }
        return BigDecimal.ZERO;
    }

    // ── Portfolio-relative SEVERITY ──────────────────────────────────────────────

    /**
     * Computes a portfolio-relative severity score on a 1–10 scale.
     *
     * <p>When the portfolio has ≥ 5 existing SEVERITY KPIs the score is derived from
     * the claim's z-score relative to the portfolio mean and standard deviation:
     * {@code adjustedSeverity = 5 + z × 2.0}, clamped to [1, 10].
     * This means an average-cost claim scores ~5; every standard-deviation above the
     * mean adds 2 points, so outliers naturally score higher.</p>
     *
     * <p>Falls back to the fixed {@code claimAmount / 10,000} formula when the
     * portfolio is too small to produce a reliable baseline.</p>
     */
    private BigDecimal computePortfolioRelativeSeverity(BigDecimal claimAmount) {
        try {
            List<ClaimKPI> portfolioKpis = claimKpiRepository.findByMetricName(MetricName.SEVERITY);
            if (portfolioKpis.size() >= 5) {
                List<Double> vals = portfolioKpis.stream()
                        .map(k -> k.getMetricValue().doubleValue())
                        .collect(Collectors.toList());
                double mean = vals.stream().mapToDouble(d -> d).average().orElse(5.0);
                double variance = vals.stream()
                        .mapToDouble(v -> Math.pow(v - mean, 2))
                        .average().orElse(1.0);
                double stdDev = Math.max(0.1, Math.sqrt(variance)); // floor prevents division by zero

                // Raw severity using the classic formula (unclamped)
                double rawSeverity = claimAmount.doubleValue() / 10_000.0;
                double zScore = (rawSeverity - mean) / stdDev;
                double adjusted = 5.0 + zScore * 2.0;
                log.debug("Portfolio-relative SEVERITY: rawSeverity={}, mean={}, stdDev={}, z={}, adjusted={}",
                        rawSeverity, mean, stdDev, zScore, adjusted);
                return BigDecimal.valueOf(Math.max(1.0, Math.min(10.0, adjusted)))
                        .setScale(2, RoundingMode.HALF_UP);
            }
        } catch (Exception e) {
            log.warn("Portfolio SEVERITY computation failed, falling back to fixed scale: {}", e.getMessage());
        }
        // Fallback: original fixed-scale formula
        return claimAmount.divide(BigDecimal.valueOf(10_000), 4, RoundingMode.HALF_UP)
                .min(BigDecimal.TEN).max(BigDecimal.ONE).setScale(2, RoundingMode.HALF_UP);
    }
}
