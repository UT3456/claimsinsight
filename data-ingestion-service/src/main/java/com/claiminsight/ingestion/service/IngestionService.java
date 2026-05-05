package com.claiminsight.ingestion.service;

import com.claiminsight.ingestion.client.AdjusterOperationsClient;
import com.claiminsight.ingestion.client.ClaimsMetricsServiceClient;
import com.claiminsight.ingestion.client.CostReserveServiceClient;
import com.claiminsight.ingestion.client.DenialLeakageServiceClient;
import com.claiminsight.ingestion.client.FraudRiskServiceClient;
import com.claiminsight.ingestion.client.NotificationServiceClient;
import com.claiminsight.ingestion.client.dto.AutoAnalyzeDenialRequest;
import com.claiminsight.ingestion.client.dto.AutoEvaluateRiskRequest;
import com.claiminsight.ingestion.client.dto.AutoGenerateSlaRequest;
import com.claiminsight.ingestion.client.dto.AutoInitializeCostRequest;
import com.claiminsight.ingestion.client.dto.NotificationDispatchRequestDTO;
import com.claiminsight.ingestion.dto.IngestionRequestDTO;
import com.claiminsight.ingestion.dto.IngestionResponseDTO;
import com.claiminsight.ingestion.exception.InvalidFeedStatusException;
import com.claiminsight.ingestion.exception.ResourceNotFoundException;
import com.claiminsight.ingestion.mapper.ClaimRawMapper;
import com.claiminsight.ingestion.model.DataFeed;
import com.claiminsight.ingestion.model.FeedStatus;
import com.claiminsight.ingestion.repository.ClaimRawRepository;
import com.claiminsight.ingestion.repository.DataFeedRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/** Service layer for ingesting and querying raw claim records. Results are cached in 'rawClaims'. */
@Service
@RequiredArgsConstructor
@Slf4j
public class IngestionService {

    private final ClaimRawRepository claimRawRepository;
    private final DataFeedRepository dataFeedRepository;
    private final ClaimRawMapper claimRawMapper;

    /** Optional: null when NotificationService is unreachable. Alerting is non-blocking. */
    @Autowired(required = false)
    private NotificationServiceClient notificationServiceClient;

    /** Optional: null when claims-metrics-service is unreachable. KPI generation is non-blocking. */
    @Autowired(required = false)
    private ClaimsMetricsServiceClient claimsMetricsServiceClient;

    /** Optional: null when AdjusterAndOperations is unreachable. SLA tracking is non-blocking. */
    @Autowired(required = false)
    private AdjusterOperationsClient adjusterOperationsClient;

    /** Optional: null when fraud-risk-service is unreachable. Risk evaluation is non-blocking. */
    @Autowired(required = false)
    private FraudRiskServiceClient fraudRiskServiceClient;

    /** Optional: null when denial-leakage-service is unreachable. Denial analysis is non-blocking. */
    @Autowired(required = false)
    private DenialLeakageServiceClient denialLeakageServiceClient;

    /** Optional: null when cost-reserve-service is unreachable. Cost/reserve seeding is non-blocking. */
    @Autowired(required = false)
    private CostReserveServiceClient costReserveServiceClient;

    /** Ingests a raw claim, links it to a DataFeed, and updates the feed's last sync date. */
    @CacheEvict(value = {"rawClaims", "feeds"}, allEntries = true)
    public IngestionResponseDTO ingestClaim(IngestionRequestDTO request) {
        DataFeed feed = dataFeedRepository.findById(request.getFeedId())
                .orElseThrow(() -> new ResourceNotFoundException("DataFeed with ID " + request.getFeedId() + " not found"));

        // Business rule: only ACTIVE feeds can receive new claim records.
        // If rejected, raise a SYSTEM notification so ops see the ingestion failure
        // in real time (the upstream caller also gets a 400, but that's often a
        // batch job with no human in the loop).
        if (feed.getStatus() != FeedStatus.ACTIVE) {
            notifyIngestionRejected(feed, request);
            throw new InvalidFeedStatusException(
                    "Cannot ingest claim into feed ID " + request.getFeedId() +
                    " — feed status is " + feed.getStatus() + ". Only ACTIVE feeds are allowed.");
        }

        var saved = claimRawRepository.save(claimRawMapper.toEntity(request, feed));

        feed.setLastSyncDate(LocalDateTime.now());
        dataFeedRepository.save(feed);

        // ── Step 1 ── KPI calculation MUST complete synchronously first.
        // fraud-risk-service's HighCost rule reads the SEVERITY KPI from
        // claims-metrics-service via Feign during auto-evaluation (step 2).
        // If KPIs are not yet persisted, SEVERITY will be missing and the rule
        // falls back to a raw claimAmount threshold instead of the computed score.
        // Fire-and-forget: a failure here is non-fatal — KPIs can be recalculated
        // manually via POST /api/kpis/calculate/{claimId} at any time.
        triggerKpiCalculation(request.getClaimId());

        // ── Steps 2–5 ── All remaining downstream triggers are independent of each
        // other and run in parallel on the ForkJoinPool common pool.
        // Each trigger already wraps its Feign call in a try/catch so a failure in
        // one branch never affects the others or the ingest response.
        final String claimId  = request.getClaimId();
        final String payload  = request.getPayloadJson();
        CompletableFuture<Void> fraudFuture  = CompletableFuture.runAsync(() -> triggerFraudEvaluation(claimId, payload));
        CompletableFuture<Void> costFuture   = CompletableFuture.runAsync(() -> triggerCostInitialization(claimId, payload));
        CompletableFuture<Void> denialFuture = CompletableFuture.runAsync(() -> triggerDenialAnalysis(claimId, payload));
        CompletableFuture<Void> slaFuture    = CompletableFuture.runAsync(() -> triggerSlaViolation(claimId, payload));
        // Wait for all parallel steps before returning so the caller can rely on
        // them having been *dispatched* (each call may still fail internally, but
        // we won't silently skip them).
        CompletableFuture.allOf(fraudFuture, costFuture, denialFuture, slaFuture).join();

        log.info("Claim {} ingested, rawId: {}", request.getClaimId(), saved.getRawId());
        return claimRawMapper.toResponseDTO(saved);
    }

    private void notifyIngestionRejected(DataFeed feed, IngestionRequestDTO request) {
        if (notificationServiceClient == null) return;
        try {
            NotificationDispatchRequestDTO notification = NotificationDispatchRequestDTO.builder()
                    .targetRoles(Set.of("ADMIN", "EXECUTIVE"))
                    .title("Ingestion rejected — feed " + feed.getFeedId() + " not ACTIVE")
                    .message("Claim " + request.getClaimId() + " was rejected because feed "
                            + feed.getFeedId() + " is in status " + feed.getStatus()
                            + ". Upstream batch jobs may be retrying against a dead feed.")
                    .category("SYSTEM")
                    .referenceId(request.getClaimId())
                    .build();
            notificationServiceClient.dispatchNotification(notification);
            log.info("Dispatched SYSTEM notification for rejected ingest on feed {}", feed.getFeedId());
        } catch (Exception e) {
            log.warn("Failed to dispatch ingestion-rejected notification for feed {}: {}",
                    feed.getFeedId(), e.getMessage());
        }
    }

    /**
     * Fires a non-blocking SLA violation auto-generation request to AdjusterAndOperations.
     * Extracts {@code incidentDate} (or {@code admissionDate} / {@code filedDate}) from
     * the payload JSON and computes the real elapsed days so the severity reflects the
     * claim's actual age — not a hardcoded placeholder.
     *
     * <p>Example: payload with {@code "incidentDate":"2026-01-10"} ingested on 2026-05-04
     * gives {@code actualDays = 114}, {@code daysOverdue = 84}, severity = CRITICAL.</p>
     */
    private void triggerSlaViolation(String claimId, String payloadJson) {
        if (adjusterOperationsClient == null) {
            log.warn("AdjusterAndOperations client not available; SLA auto-generation skipped for claim {}", claimId);
            return;
        }
        try {
            int actualDays = computeActualDays(payloadJson);
            adjusterOperationsClient.autoGenerateSlaViolation(
                    AutoGenerateSlaRequest.builder()
                            .claimRef(claimId)
                            .adjusterId(1L)
                            .actualDays(actualDays)
                            .slaTargetDays(30)
                            .build());
            log.info("SLA violation auto-generation triggered for claim {} (actualDays={})", claimId, actualDays);
        } catch (Exception e) {
            log.warn("SLA violation auto-generation failed for claim {} (non-fatal): {}", claimId, e.getMessage());
        }
    }

    /**
     * Parses the first available date field from the payload JSON and returns
     * the number of calendar days elapsed from that date to today.
     *
     * <p>Fields tried in order: {@code incidentDate}, {@code admissionDate},
     * {@code filedDate}. All must be in ISO format {@code YYYY-MM-DD}.</p>
     *
     * <p>Returns {@code 31} (a LOW-severity breach placeholder) when no date
     * field is found or the value cannot be parsed.</p>
     */
    private int computeActualDays(String payloadJson) {
        if (payloadJson == null || payloadJson.isBlank()) return 31;
        for (String field : new String[]{"incidentDate", "admissionDate", "filedDate"}) {
            try {
                String marker = "\"" + field + "\":\"";
                int idx = payloadJson.indexOf(marker);
                if (idx < 0) continue;
                int start = idx + marker.length();
                int end   = payloadJson.indexOf('"', start);
                if (end <= start) continue;
                LocalDate incidentDate = LocalDate.parse(payloadJson.substring(start, end));
                long days = ChronoUnit.DAYS.between(incidentDate, LocalDate.now());
                int result = (int) Math.max(1, days);
                log.debug("SLA actualDays for payload field '{}': {} days", field, result);
                return result;
            } catch (Exception ignored) { /* try next field */ }
        }
        log.debug("No date field found in payload — using default actualDays=31");
        return 31;
    }

    /**
     * Fires a non-blocking KPI-calculation request to claims-metrics-service.
     * If the remote call fails (service down, timeout, 4xx/5xx), a WARN is logged
     * and ingestion is unaffected. The KPIs can always be recalculated later via
     * POST /api/kpis/calculate/{claimId}.
     */
    private void triggerKpiCalculation(String claimId) {
        if (claimsMetricsServiceClient == null) {
            log.warn("claims-metrics-service client not available; KPI auto-generation skipped for claim {}", claimId);
            return;
        }
        try {
            claimsMetricsServiceClient.calculateKpis(claimId);
            log.info("KPI auto-generation triggered for claim {}", claimId);
        } catch (Exception e) {
            log.warn("KPI auto-generation failed for claim {} (non-fatal): {}", claimId, e.getMessage());
        }
    }

    /**
     * Fires a non-blocking fraud risk evaluation request to fraud-risk-service.
     * Passes payloadJson so the risk engine can extract claimAmount, policyStartDate,
     * and incidentDate to evaluate HighCost + UnusualTiming rules without a DB roundtrip.
     */
    private void triggerFraudEvaluation(String claimId, String payloadJson) {
        if (fraudRiskServiceClient == null) {
            log.warn("fraud-risk-service client not available; fraud evaluation skipped for claim {}", claimId);
            return;
        }
        try {
            fraudRiskServiceClient.autoEvaluateRisk(
                    AutoEvaluateRiskRequest.builder()
                            .claimId(claimId)
                            .payloadJson(payloadJson)
                            .build());
            log.info("Fraud risk evaluation triggered for claim {}", claimId);
        } catch (Exception e) {
            log.warn("Fraud risk evaluation failed for claim {} (non-fatal): {}", claimId, e.getMessage());
        }
    }

    /**
     * Fires a non-blocking denial analysis request to denial-leakage-service.
     * If the payload contains a denial code or status=DENIED, a DenialPattern is created.
     * Overpayment LeakageFlags are raised when claimAmount exceeds policyLimit.
     */
    private void triggerDenialAnalysis(String claimId, String payloadJson) {
        if (denialLeakageServiceClient == null) {
            log.warn("denial-leakage-service client not available; denial analysis skipped for claim {}", claimId);
            return;
        }
        try {
            denialLeakageServiceClient.autoAnalyzeDenial(
                    AutoAnalyzeDenialRequest.builder()
                            .claimId(claimId)
                            .payloadJson(payloadJson)
                            .build());
            log.info("Denial analysis triggered for claim {}", claimId);
        } catch (Exception e) {
            log.warn("Denial analysis failed for claim {} (non-fatal): {}", claimId, e.getMessage());
        }
    }

    /**
     * Fires a non-blocking cost/reserve/aging initialization request to cost-reserve-service.
     * Seeds ClaimCost (SETTLEMENT), ClaimReserve (120% of claimAmount), and
     * AgingRecord (bucket from incidentDate elapsed days) in one call.
     */
    private void triggerCostInitialization(String claimId, String payloadJson) {
        if (costReserveServiceClient == null) {
            log.warn("cost-reserve-service client not available; cost initialization skipped for claim {}", claimId);
            return;
        }
        try {
            costReserveServiceClient.autoInitializeCost(
                    AutoInitializeCostRequest.builder()
                            .claimId(claimId)
                            .payloadJson(payloadJson)
                            .build());
            log.info("Cost/reserve/aging initialization triggered for claim {}", claimId);
        } catch (Exception e) {
            log.warn("Cost initialization failed for claim {} (non-fatal): {}", claimId, e.getMessage());
        }
    }

    /**
     * Returns all raw claim records. Result is cached.
     * @Transactional(readOnly) keeps the Hibernate session open while the stream
     * maps ClaimRaw → DTO so the lazy DataFeed proxy can be initialised without
     * triggering "no session" errors.
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "rawClaims", key = "'all'")
    public List<IngestionResponseDTO> getAllRawClaims() {
        return claimRawRepository.findAll()
                .stream().map(claimRawMapper::toResponseDTO).collect(Collectors.toList());
    }

    /** Returns raw records by claimId. Result is cached. */
    @Transactional(readOnly = true)
    @Cacheable(value = "rawClaims", key = "#claimId")
    public List<IngestionResponseDTO> getRawClaimsByClaimId(String claimId) {
        return claimRawRepository.findByClaimId(claimId)
                .stream().map(claimRawMapper::toResponseDTO).collect(Collectors.toList());
    }

    /** Returns raw records by feedId. Throws 404 if feed does not exist. */
    @Transactional(readOnly = true)
    @Cacheable(value = "rawClaims", key = "'feed-' + #feedId")
    public List<IngestionResponseDTO> getRawClaimsByFeedId(Long feedId) {
        if (!dataFeedRepository.existsById(feedId)) {
            throw new ResourceNotFoundException("DataFeed with ID " + feedId + " not found");
        }
        return claimRawRepository.findByDataFeed_FeedId(feedId)
                .stream().map(claimRawMapper::toResponseDTO).collect(Collectors.toList());
    }

    /** Deletes a raw claim record by ID. Throws 404 if not found. */
    @CacheEvict(value = "rawClaims", allEntries = true)
    public void deleteRawClaim(Long rawId) {
        if (!claimRawRepository.existsById(rawId)) {
            throw new ResourceNotFoundException("ClaimRaw with ID " + rawId + " not found");
        }
        claimRawRepository.deleteById(rawId);
    }
}
