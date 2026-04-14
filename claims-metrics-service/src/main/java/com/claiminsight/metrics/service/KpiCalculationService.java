package com.claiminsight.metrics.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;

import com.claiminsight.metrics.client.DataIngestionClient;
import com.claiminsight.metrics.client.dto.ClaimRawDTO;
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

    private final DataIngestionClient dataIngestionClient;
    private final ClaimKpiRepository  claimKpiRepository;
    private final ClaimKpiMapper      claimKpiMapper;
    private final ObjectMapper        objectMapper;

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
        LocalDate incidentDate = extractIncidentDate(records.get(0).getPayloadJson(), firstIngested);
        long tatDays = ChronoUnit.DAYS.between(incidentDate, today);
        BigDecimal tat = BigDecimal.valueOf(Math.max(1, tatDays));

        // ── SEVERITY ────────────────────────────────────────────────────
        BigDecimal claimAmount = extractClaimAmount(records.get(0).getPayloadJson());
        BigDecimal severity = claimAmount
                .divide(BigDecimal.valueOf(10_000), 4, RoundingMode.HALF_UP)
                .min(BigDecimal.TEN)
                .max(BigDecimal.ONE)
                .setScale(2, RoundingMode.HALF_UP);

        // ── LOSS RATIO ───────────────────────────────────────────────────
        BigDecimal premium = extractPremium(records.get(0).getPayloadJson());
        BigDecimal lossRatio = premium.compareTo(BigDecimal.ZERO) > 0
                ? claimAmount.divide(premium, 4, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        // ── SETTLEMENT TIME ──────────────────────────────────────────────
        // Days from incident/filing date to settlement/closure date from payload.
        // Falls back to cycleTime when settlementDate is absent.
        LocalDate settlementDate = extractSettlementDate(records.get(0).getPayloadJson(), null);
        BigDecimal settlementTime;
        if (settlementDate != null) {
            long settlementDays = ChronoUnit.DAYS.between(incidentDate, settlementDate);
            settlementTime = BigDecimal.valueOf(Math.max(1, settlementDays));
        } else {
            settlementTime = cycleTime; // use cycle time as proxy when settlement date unavailable
        }

        // ── SAVE ALL 6 KPIs ─────────────────────────────────────────────
        List<ClaimKpiResponseDTO> savedKpis = new ArrayList<>();
        savedKpis.add(saveKpi(claimId, MetricName.TAT,             tat,            calculatedDate));
        savedKpis.add(saveKpi(claimId, MetricName.CYCLE_TIME,      cycleTime,      calculatedDate));
        savedKpis.add(saveKpi(claimId, MetricName.SEVERITY,        severity,       calculatedDate));
        savedKpis.add(saveKpi(claimId, MetricName.FREQUENCY,       frequency,      calculatedDate));
        savedKpis.add(saveKpi(claimId, MetricName.LOSS_RATIO,      lossRatio,      calculatedDate));
        savedKpis.add(saveKpi(claimId, MetricName.SETTLEMENT_TIME, settlementTime, calculatedDate));

        log.info("KPIs saved for claimId: {} — TAT={}, CYCLE_TIME={}, SEVERITY={}, FREQUENCY={}, LOSS_RATIO={}, SETTLEMENT_TIME={}",
                claimId, tat, cycleTime, severity, frequency, lossRatio, settlementTime);

        return new KpiSummaryDTO(claimId, tat, cycleTime, severity, frequency, lossRatio, settlementTime, calculatedDate, savedKpis);
    }

    private ClaimKpiResponseDTO saveKpi(String claimId, MetricName name, BigDecimal value, LocalDate date) {
        ClaimKPI kpi = new ClaimKPI();
        kpi.setClaimId(claimId);
        kpi.setMetricName(name);
        kpi.setMetricValue(value);
        kpi.setMetricDate(date);
        return claimKpiMapper.toResponseDTO(claimKpiRepository.save(kpi));
    }

    private LocalDate extractIncidentDate(String payloadJson, LocalDate fallback) {
        try {
            JsonNode node = objectMapper.readTree(payloadJson);
            for (String field : new String[]{"incidentDate", "admissionDate"}) {
                JsonNode n = node.get(field);
                if (n != null && !n.isNull()) return LocalDate.parse(n.asText());
            }
        } catch (Exception e) {
            log.warn("Could not parse incidentDate from payload, using fallback");
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

    private LocalDate extractSettlementDate(String payloadJson, LocalDate fallback) {
        try {
            JsonNode node = objectMapper.readTree(payloadJson);
            for (String field : new String[]{"settlementDate", "closedDate", "resolvedDate", "closureDate"}) {
                JsonNode n = node.get(field);
                if (n != null && !n.isNull()) return LocalDate.parse(n.asText());
            }
        } catch (Exception e) {
            log.warn("Could not parse settlementDate from payload, settlement time will use cycleTime as proxy");
        }
        return fallback;
    }
}
