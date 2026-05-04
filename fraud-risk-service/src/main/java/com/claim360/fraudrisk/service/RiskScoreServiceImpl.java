package com.claim360.fraudrisk.service;

import com.claim360.fraudrisk.client.ClaimsMetricsServiceClient;
import com.claim360.fraudrisk.client.NotificationServiceClient;
import com.claim360.fraudrisk.client.dto.ClaimKpiDTO;
import com.claim360.fraudrisk.client.dto.NotificationDispatchRequestDTO;
import com.claim360.fraudrisk.dto.RiskScoreRequest;
import com.claim360.fraudrisk.dto.RiskScoreResponse;
import com.claim360.fraudrisk.entity.RiskIndicator;
import com.claim360.fraudrisk.entity.RiskScore;
import com.claim360.fraudrisk.enums.IndicatorType;
import com.claim360.fraudrisk.exception.ResourceNotFoundException;
import com.claim360.fraudrisk.repository.RiskIndicatorRepository;
import com.claim360.fraudrisk.repository.RiskScoreRepository;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RiskScoreServiceImpl implements RiskScoreService {

    private static final double HIGH_RISK_THRESHOLD = 75.0;

    private final RiskScoreRepository riskScoreRepository;
    private final RiskIndicatorRepository riskIndicatorRepository;
    private final ModelMapper modelMapper;

    /** Optional: null when NotificationService is unreachable. Alerting is non-blocking. */
    @Autowired(required = false)
    private NotificationServiceClient notificationServiceClient;

    /** Optional: null when claims-metrics-service is unreachable.
     *  When available, HighCost rule uses the portfolio-relative SEVERITY KPI
     *  instead of raw claimAmount cutoffs. */
    @Autowired(required = false)
    private ClaimsMetricsServiceClient claimsMetricsServiceClient;

    // ── Create ──────────────────────────────────────────────────────────────

    @Override
    public RiskScoreResponse createRiskScore(RiskScoreRequest request) {
        log.info("Creating RiskScore for claimId: {}", request.getClaimId());
        RiskScore score = modelMapper.map(request, RiskScore.class);
        RiskScore saved = riskScoreRepository.save(score);
        log.info("RiskScore created with ID: {}", saved.getScoreId());

        // Alert via NotificationService when risk score exceeds high-risk threshold.
        // Dispatch to FRAUD analysts (who triage) + MANAGER (who approves holds).
        if (saved.getScoreValue() >= HIGH_RISK_THRESHOLD && notificationServiceClient != null) {
            try {
                NotificationDispatchRequestDTO notification = NotificationDispatchRequestDTO.builder()
                        .targetRoles(Set.of("FRAUD", "MANAGER"))
                        .title("High Fraud Risk Alert — " + saved.getClaimId())
                        .message(String.format("Claim %s has been assigned a fraud risk score of %.1f, which exceeds the high-risk threshold of %.1f.",
                                saved.getClaimId(), saved.getScoreValue(), HIGH_RISK_THRESHOLD))
                        .category("RISK")
                        .referenceId(saved.getClaimId())
                        .build();
                notificationServiceClient.dispatchNotification(notification);
                log.info("High-risk alert dispatched for claimId {} (score={})", saved.getClaimId(), saved.getScoreValue());
            } catch (Exception ex) {
                log.warn("Failed to dispatch high-risk notification for claimId {} — NotificationService may be unavailable", saved.getClaimId());
            }
        }

        return modelMapper.map(saved, RiskScoreResponse.class);
    }

    // ── Read ────────────────────────────────────────────────────────────────

    @Override
    public RiskScoreResponse getRiskScoreById(Long scoreId) {
        log.info("Fetching RiskScore with ID: {}", scoreId);
        RiskScore score = riskScoreRepository.findById(scoreId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "RiskScore", "scoreId", scoreId));
        return modelMapper.map(score, RiskScoreResponse.class);
    }

    @Override
    public List<RiskScoreResponse> getAllRiskScores() {
        log.info("Fetching all RiskScores");
        return riskScoreRepository.findAll()
                .stream()
                .map(score -> modelMapper.map(score, RiskScoreResponse.class))
                .collect(Collectors.toList());
    }

    @Override
    public List<RiskScoreResponse> getRiskScoresByClaimId(String claimId) {
        log.info("Fetching RiskScores for claimId: {}", claimId);
        List<RiskScore> scores = riskScoreRepository.findByClaimId(claimId);
        if (scores.isEmpty()) {
            throw new ResourceNotFoundException("RiskScore", "claimId", claimId);
        }
        return scores.stream()
                .map(score -> modelMapper.map(score, RiskScoreResponse.class))
                .collect(Collectors.toList());
    }

    @Override
    public RiskScoreResponse getLatestRiskScoreByClaimId(String claimId) {
        log.info("Fetching latest RiskScore for claimId: {}", claimId);
        RiskScore score = riskScoreRepository
                .findTopByClaimIdOrderByComputedDateDesc(claimId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "RiskScore", "claimId", claimId));
        return modelMapper.map(score, RiskScoreResponse.class);
    }

    @Override
    public List<RiskScoreResponse> getRiskScoresAboveThreshold(Double threshold) {
        log.info("Fetching RiskScores above threshold: {}", threshold);
        List<RiskScore> scores = riskScoreRepository
                .findByScoreValueGreaterThanEqual(threshold);
        if (scores.isEmpty()) {
            throw new ResourceNotFoundException(
                    "RiskScore", "scoreValue >= ", threshold);
        }
        return scores.stream()
                .map(score -> modelMapper.map(score, RiskScoreResponse.class))
                .collect(Collectors.toList());
    }

    // ── Update ──────────────────────────────────────────────────────────────

    @Override
    public RiskScoreResponse updateRiskScore(Long scoreId, RiskScoreRequest request) {
        log.info("Updating RiskScore with ID: {}", scoreId);
        RiskScore existing = riskScoreRepository.findById(scoreId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "RiskScore", "scoreId", scoreId));

        // Map updated fields from request to existing entity
        modelMapper.map(request, existing);
        existing.setScoreId(scoreId);

        RiskScore updated = riskScoreRepository.save(existing);
        log.info("RiskScore updated with ID: {}", updated.getScoreId());
        return modelMapper.map(updated, RiskScoreResponse.class);
    }

    // ── Delete ──────────────────────────────────────────────────────────────

    @Override
    public void deleteRiskScore(Long scoreId) {
        log.info("Deleting RiskScore with ID: {}", scoreId);
        RiskScore existing = riskScoreRepository.findById(scoreId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "RiskScore", "scoreId", scoreId));
        riskScoreRepository.delete(existing);
        log.info("RiskScore deleted with ID: {}", scoreId);
    }

    // ── Auto-Evaluate (called by data-ingestion-service) ────────────────────

    @Override
    public RiskScoreResponse autoEvaluateRisk(String claimId, String payloadJson) {
        log.info("Auto-evaluating fraud risk for claimId: {}", claimId);

        // ── Rule 1: HighCost ─────────────────────────────────────────────────────
        // Prefer the portfolio-relative SEVERITY KPI from claims-metrics-service.
        // Fallback to raw claimAmount thresholds when the KPI is not yet computed.
        String highCostSeverity = null;
        if (claimsMetricsServiceClient != null) {
            try {
                List<ClaimKpiDTO> severityKpis = claimsMetricsServiceClient
                        .getKpisByClaimIdAndMetricName(claimId, "SEVERITY");
                if (!severityKpis.isEmpty()) {
                    double severityValue = severityKpis.stream()
                            .mapToDouble(ClaimKpiDTO::getMetricValue).max().orElse(0);
                    if (severityValue >= 2.0) {
                        highCostSeverity = severityValue >= 8.0 ? "HIGH"
                                : severityValue >= 5.0 ? "MEDIUM" : "LOW";
                        log.debug("HighCost severity from KPI SEVERITY={} for claimId={}", severityValue, claimId);
                    }
                }
            } catch (Exception ex) {
                log.debug("Could not fetch SEVERITY KPI for claimId={}, falling back to raw amount: {}",
                        claimId, ex.getMessage());
            }
        }
        // Fallback: raw claimAmount thresholds
        double claimAmount = extractDouble(payloadJson, "claimAmount");
        if (highCostSeverity == null && claimAmount > 10_000) {
            highCostSeverity = claimAmount > 50_000 ? "HIGH" : claimAmount > 25_000 ? "MEDIUM" : "LOW";
        }
        if (highCostSeverity != null) {
            RiskIndicator indicator = new RiskIndicator();
            indicator.setClaimId(claimId);
            indicator.setIndicatorType(IndicatorType.HighCost);
            indicator.setSeverity(highCostSeverity);
            indicator.setTriggeredDate(LocalDate.now());
            riskIndicatorRepository.save(indicator);
            log.info("HighCost indicator ({}) fired for claimId={}", highCostSeverity, claimId);
        }

        // ── Rule 2: UnusualTiming — claim filed < 30 days after policy started ──
        String filedDateStr  = extractString(payloadJson, "filedDate", "incidentDate", "admissionDate");
        String policyStart   = extractString(payloadJson, "policyStartDate");
        if (filedDateStr != null && policyStart != null) {
            try {
                long gap = ChronoUnit.DAYS.between(LocalDate.parse(policyStart), LocalDate.parse(filedDateStr));
                if (gap >= 0 && gap < 30) {
                    String severity = gap < 7 ? "HIGH" : gap < 15 ? "MEDIUM" : "LOW";
                    RiskIndicator indicator = new RiskIndicator();
                    indicator.setClaimId(claimId);
                    indicator.setIndicatorType(IndicatorType.UnusualTiming);
                    indicator.setSeverity(severity);
                    indicator.setTriggeredDate(LocalDate.now());
                    riskIndicatorRepository.save(indicator);
                    log.info("UnusualTiming indicator ({}) fired for claimId={}, gap={}d", severity, claimId, gap);
                }
            } catch (Exception ex) {
                log.debug("Could not parse dates for UnusualTiming rule on claimId={}: {}", claimId, ex.getMessage());
            }
        }

        // ── Rule 3: Pattern — portfolio-level fraud concentration check ──────────
        // If 3+ distinct claims have scored ≥ 50 in the last 12 months, the volume
        // of high-risk activity constitutes a Pattern that warrants investigation.
        // We avoid double-flagging by checking for an existing Pattern indicator first.
        try {
            LocalDate oneYearAgo = LocalDate.now().minusYears(1);
            long recentHighRiskCount = riskScoreRepository
                    .findByScoreValueGreaterThanEqual(50.0)
                    .stream()
                    .filter(s -> s.getComputedDate() != null
                            && s.getComputedDate().isAfter(oneYearAgo))
                    .count();
            if (recentHighRiskCount >= 3) {
                List<RiskIndicator> existingPattern = riskIndicatorRepository
                        .findByClaimIdAndIndicatorType(claimId, IndicatorType.Pattern);
                if (existingPattern.isEmpty()) {
                    String patternSeverity = recentHighRiskCount >= 10 ? "HIGH"
                            : recentHighRiskCount >= 6 ? "MEDIUM" : "LOW";
                    RiskIndicator patternIndicator = new RiskIndicator();
                    patternIndicator.setClaimId(claimId);
                    patternIndicator.setIndicatorType(IndicatorType.Pattern);
                    patternIndicator.setSeverity(patternSeverity);
                    patternIndicator.setTriggeredDate(LocalDate.now());
                    riskIndicatorRepository.save(patternIndicator);
                    log.info("Pattern indicator ({}) fired for claimId={} — {} high-risk claims in portfolio (last 12 months)",
                            patternSeverity, claimId, recentHighRiskCount);
                }
            }
        } catch (Exception ex) {
            log.debug("Pattern rule evaluation failed for claimId={} (non-fatal): {}", claimId, ex.getMessage());
        }

        // ── Weighted score from all indicators for this claim ───────────────────
        List<RiskIndicator> indicators = riskIndicatorRepository.findByClaimId(claimId);
        double score = 0;
        for (RiskIndicator ind : indicators) {
            int points = "HIGH".equals(ind.getSeverity()) ? 50 : "MEDIUM".equals(ind.getSeverity()) ? 25 : 10;
            double weight = switch (ind.getIndicatorType()) {
                case HighCost      -> 1.2;
                case Pattern       -> 1.5;
                case UnusualTiming -> 0.8;
            };
            score += points * weight;
        }
        score = Math.min(score, 100.0);

        RiskScore riskScore = new RiskScore();
        riskScore.setClaimId(claimId);
        riskScore.setScoreValue(score);
        riskScore.setComputedDate(LocalDate.now());
        RiskScore saved = riskScoreRepository.save(riskScore);
        log.info("Auto-evaluated risk for claimId={}: score={}, indicators={}", claimId, score, indicators.size());

        // ── High-risk notification ───────────────────────────────────────────────
        if (score >= HIGH_RISK_THRESHOLD && notificationServiceClient != null) {
            try {
                NotificationDispatchRequestDTO notification = NotificationDispatchRequestDTO.builder()
                        .targetRoles(Set.of("FRAUD", "MANAGER"))
                        .title("High Fraud Risk Alert — " + claimId)
                        .message(String.format(
                                "Claim %s auto-evaluated with fraud risk score %.1f (threshold %.1f). %d indicator(s) triggered.",
                                claimId, score, HIGH_RISK_THRESHOLD, indicators.size()))
                        .category("RISK")
                        .referenceId(claimId)
                        .build();
                notificationServiceClient.dispatchNotification(notification);
                log.info("High-risk alert dispatched for claimId={} (score={})", claimId, score);
            } catch (Exception ex) {
                log.warn("Failed to dispatch high-risk notification for claimId={}", claimId);
            }
        }

        return modelMapper.map(saved, RiskScoreResponse.class);
    }

    // ── Payload parsing helpers ──────────────────────────────────────────────────

    private double extractDouble(String json, String... keys) {
        if (json == null) return 0;
        for (String key : keys) {
            try {
                String marker = "\"" + key + "\":";
                int idx = json.indexOf(marker);
                if (idx < 0) continue;
                int start = idx + marker.length();
                while (start < json.length() && json.charAt(start) == ' ') start++;
                int end = start;
                while (end < json.length() && (Character.isDigit(json.charAt(end)) || json.charAt(end) == '.')) end++;
                if (end > start) return Double.parseDouble(json.substring(start, end));
            } catch (Exception ignored) {}
        }
        return 0;
    }

    private String extractString(String json, String... keys) {
        if (json == null) return null;
        for (String key : keys) {
            try {
                String marker = "\"" + key + "\":\"";
                int idx = json.indexOf(marker);
                if (idx < 0) continue;
                int start = idx + marker.length();
                int end   = json.indexOf('"', start);
                if (end > start) return json.substring(start, end);
            } catch (Exception ignored) {}
        }
        return null;
    }
}