package com.claim360.denialleakage.service;

import com.claim360.denialleakage.client.FraudRiskServiceClient;
import com.claim360.denialleakage.client.NotificationServiceClient;
import com.claim360.denialleakage.client.dto.FraudRiskScoreDTO;
import com.claim360.denialleakage.client.dto.NotificationRequestDTO;
import com.claim360.denialleakage.dto.LeakageFlagRequest;
import com.claim360.denialleakage.dto.LeakageFlagResponse;
import com.claim360.denialleakage.entity.LeakageFlag;
import com.claim360.denialleakage.enums.LeakageType;
import com.claim360.denialleakage.exception.ResourceNotFoundException;
import com.claim360.denialleakage.repository.LeakageFlagRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service implementation for LeakageFlag operations.
 * Uses ModelMapper for Entity to DTO conversion.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class LeakageFlagServiceImpl implements LeakageFlagService {

    private final LeakageFlagRepository leakageFlagRepository;
    private final ModelMapper modelMapper;

    /** Large-loss threshold for leakage alerts. */
    private static final double LARGE_LOSS_THRESHOLD = 10_000.0;

    /** Optional: null when fraud-risk-service is unreachable. Cross-reference is non-blocking. */
    @Autowired(required = false)
    private FraudRiskServiceClient fraudRiskServiceClient;

    /** Optional: null when NotificationService is unreachable. Notifications are non-blocking. */
    @Autowired(required = false)
    private NotificationServiceClient notificationServiceClient;

    @Override
    public LeakageFlagResponse createLeakageFlag(LeakageFlagRequest request) {
        log.info("Creating LeakageFlag for claimId: {}", request.getClaimId());

        // Cross-reference fraud risk scores for this claim before flagging leakage
        if (fraudRiskServiceClient != null) {
            try {
                List<FraudRiskScoreDTO> riskScores = fraudRiskServiceClient.getRiskScoresByClaimId(request.getClaimId());
                if (!riskScores.isEmpty()) {
                    double maxScore = riskScores.stream()
                            .mapToDouble(FraudRiskScoreDTO::getScoreValue)
                            .max()
                            .orElse(0.0);
                    log.info("Fraud risk cross-reference for claimId {}: {} score(s) found, max score={}",
                            request.getClaimId(), riskScores.size(), maxScore);
                }
            } catch (Exception ex) {
                log.debug("Could not fetch fraud risk scores for claimId {} — fraud-risk-service may be unavailable", request.getClaimId());
            }
        }

        LeakageFlag flag = modelMapper.map(request, LeakageFlag.class);
        LeakageFlag saved = leakageFlagRepository.save(flag);
        log.info("LeakageFlag created with ID: {}", saved.getLeakageId());

        if (saved.getEstimatedLoss() != null && saved.getEstimatedLoss() >= LARGE_LOSS_THRESHOLD) {
            sendLeakageNotification(saved);
        }

        return modelMapper.map(saved, LeakageFlagResponse.class);
    }

    private void sendLeakageNotification(LeakageFlag flag) {
        if (notificationServiceClient == null) return;
        try {
            NotificationRequestDTO notification = NotificationRequestDTO.builder()
                    .userId(1L) // TODO: fan out to leakage-owning users via user-service
                    .title("High-Value Leakage Flag - Claim " + flag.getClaimId())
                    .message("Claim " + flag.getClaimId() + " flagged for " + flag.getLeakageType()
                            + " leakage. Estimated loss: $" + flag.getEstimatedLoss() + ". Requires investigation.")
                    .category("DENIAL")
                    .referenceId(flag.getClaimId())
                    .build();
            notificationServiceClient.createNotification(notification);
            log.info("Sent leakage notification for claim {}", flag.getClaimId());
        } catch (Exception e) {
            log.warn("Failed to send leakage notification for claim {}: {}", flag.getClaimId(), e.getMessage());
        }
    }

    @Override
    public LeakageFlagResponse getLeakageFlagById(Long leakageId) {
        log.info("Fetching LeakageFlag with ID: {}", leakageId);
        LeakageFlag flag = leakageFlagRepository.findById(leakageId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "LeakageFlag", "leakageId", leakageId));
        return modelMapper.map(flag, LeakageFlagResponse.class);
    }

    @Override
    public List<LeakageFlagResponse> getAllLeakageFlags() {
        log.info("Fetching all LeakageFlags");
        return leakageFlagRepository.findAll()
                .stream()
                .map(flag -> modelMapper.map(flag, LeakageFlagResponse.class))
                .collect(Collectors.toList());
    }

    @Override
    public List<LeakageFlagResponse> getLeakageFlagsByClaimId(String claimId) {
        log.info("Fetching LeakageFlags for claimId: {}", claimId);
        List<LeakageFlag> flags = leakageFlagRepository.findByClaimId(claimId);
        if (flags.isEmpty()) {
            throw new ResourceNotFoundException("LeakageFlag", "claimId", claimId);
        }
        return flags.stream()
                .map(flag -> modelMapper.map(flag, LeakageFlagResponse.class))
                .collect(Collectors.toList());
    }

    @Override
    public List<LeakageFlagResponse> getLeakageFlagsByLeakageType(LeakageType leakageType) {
        log.info("Fetching LeakageFlags by type: {}", leakageType);
        List<LeakageFlag> flags = leakageFlagRepository.findByLeakageType(leakageType);
        if (flags.isEmpty()) {
            throw new ResourceNotFoundException("LeakageFlag", "leakageType", leakageType);
        }
        return flags.stream()
                .map(flag -> modelMapper.map(flag, LeakageFlagResponse.class))
                .collect(Collectors.toList());
    }

    @Override
    public List<LeakageFlagResponse> getLeakageFlagsByEstimatedLoss(Double amount) {
        log.info("Fetching LeakageFlags above estimated loss: {}", amount);
        List<LeakageFlag> flags = leakageFlagRepository
                .findByEstimatedLossGreaterThanEqual(amount);
        if (flags.isEmpty()) {
            throw new ResourceNotFoundException(
                    "LeakageFlag", "estimatedLoss >= ", amount);
        }
        return flags.stream()
                .map(flag -> modelMapper.map(flag, LeakageFlagResponse.class))
                .collect(Collectors.toList());
    }

    @Override
    public List<LeakageFlagResponse> getLeakageFlagsByClaimIdAndLeakageType(
            String claimId, LeakageType leakageType) {
        log.info("Fetching LeakageFlags by claimId: {} and type: {}", claimId, leakageType);
        List<LeakageFlag> flags = leakageFlagRepository
                .findByClaimIdAndLeakageType(claimId, leakageType);
        if (flags.isEmpty()) {
            throw new ResourceNotFoundException(
                    "LeakageFlag", "claimId and leakageType",
                    claimId + " / " + leakageType);
        }
        return flags.stream()
                .map(flag -> modelMapper.map(flag, LeakageFlagResponse.class))
                .collect(Collectors.toList());
    }

    @Override
    public LeakageFlagResponse updateLeakageFlag(Long leakageId,
                                                 LeakageFlagRequest request) {
        log.info("Updating LeakageFlag with ID: {}", leakageId);
        LeakageFlag existing = leakageFlagRepository.findById(leakageId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "LeakageFlag", "leakageId", leakageId));
        modelMapper.map(request, existing);
        existing.setLeakageId(leakageId);
        LeakageFlag updated = leakageFlagRepository.save(existing);
        log.info("LeakageFlag updated with ID: {}", updated.getLeakageId());
        return modelMapper.map(updated, LeakageFlagResponse.class);
    }

    @Override
    public void deleteLeakageFlag(Long leakageId) {
        log.info("Deleting LeakageFlag with ID: {}", leakageId);
        LeakageFlag existing = leakageFlagRepository.findById(leakageId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "LeakageFlag", "leakageId", leakageId));
        leakageFlagRepository.delete(existing);
        log.info("LeakageFlag deleted with ID: {}", leakageId);
    }

    // ── Leakage Summary ──────────────────────────────────────────────────────────

    /**
     * Executes a {@code GROUP BY leakageType} aggregate query and returns a
     * dashboard-ready summary: total flag count, total estimated loss, and a
     * per-type breakdown each containing count and estimated loss.
     *
     * <p>Example response structure:
     * <pre>{
     *   "totalFlags": 12,
     *   "totalEstimatedLoss": "145000.00",
     *   "breakdown": [
     *     { "leakageType": "Overpayment", "count": 8, "totalEstimatedLoss": "120000.00" },
     *     { "leakageType": "Delay",       "count": 4, "totalEstimatedLoss":  "25000.00" }
     *   ]
     * }</pre></p>
     */
    @Override
    public Map<String, Object> getLeakageSummaryByType() {
        log.info("Aggregating leakage summary by type");
        List<Object[]> rows = leakageFlagRepository.getLeakageSummaryGroupedByType();

        long totalFlags = 0;
        double totalLoss = 0;
        List<Map<String, Object>> breakdown = new ArrayList<>();

        for (Object[] row : rows) {
            LeakageType type  = (LeakageType) row[0];
            long count        = (Long) row[1];
            double loss       = row[2] != null ? ((Number) row[2]).doubleValue() : 0.0;
            totalFlags += count;
            totalLoss  += loss;

            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("leakageType",         type.name());
            entry.put("count",               count);
            entry.put("totalEstimatedLoss",  String.format("%.2f", loss));
            breakdown.add(entry);
        }

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("totalFlags",         totalFlags);
        summary.put("totalEstimatedLoss", String.format("%.2f", totalLoss));
        summary.put("breakdown",          breakdown);
        return summary;
    }
}