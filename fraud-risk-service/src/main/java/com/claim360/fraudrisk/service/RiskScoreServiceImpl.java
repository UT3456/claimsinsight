package com.claim360.fraudrisk.service;

import com.claim360.fraudrisk.client.NotificationServiceClient;
import com.claim360.fraudrisk.client.dto.NotificationRequestDTO;
import com.claim360.fraudrisk.dto.RiskScoreRequest;
import com.claim360.fraudrisk.dto.RiskScoreResponse;
import com.claim360.fraudrisk.entity.RiskScore;
import com.claim360.fraudrisk.exception.ResourceNotFoundException;
import com.claim360.fraudrisk.repository.RiskScoreRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RiskScoreServiceImpl implements RiskScoreService {

    private static final double HIGH_RISK_THRESHOLD = 75.0;
    private static final long SYSTEM_USER_ID = 1L;

    private final RiskScoreRepository riskScoreRepository;
    private final ModelMapper modelMapper;

    /** Optional: null when NotificationService is unreachable. Alerting is non-blocking. */
    @Autowired(required = false)
    private NotificationServiceClient notificationServiceClient;

    // ── Create ──────────────────────────────────────────────────────────────

    @Override
    public RiskScoreResponse createRiskScore(RiskScoreRequest request) {
        log.info("Creating RiskScore for claimId: {}", request.getClaimId());
        RiskScore score = modelMapper.map(request, RiskScore.class);
        RiskScore saved = riskScoreRepository.save(score);
        log.info("RiskScore created with ID: {}", saved.getScoreId());

        // Alert via NotificationService when risk score exceeds high-risk threshold
        if (saved.getScoreValue() >= HIGH_RISK_THRESHOLD && notificationServiceClient != null) {
            try {
                NotificationRequestDTO notification = NotificationRequestDTO.builder()
                        .userId(SYSTEM_USER_ID)
                        .title("High Fraud Risk Alert — " + saved.getClaimId())
                        .message(String.format("Claim %s has been assigned a fraud risk score of %.1f, which exceeds the high-risk threshold of %.1f.",
                                saved.getClaimId(), saved.getScoreValue(), HIGH_RISK_THRESHOLD))
                        .category("RISK")
                        .referenceId(saved.getClaimId())
                        .build();
                notificationServiceClient.createNotification(notification);
                log.info("High-risk alert sent for claimId {} (score={})", saved.getClaimId(), saved.getScoreValue());
            } catch (Exception ex) {
                log.warn("Failed to send high-risk notification for claimId {} — NotificationService may be unavailable", saved.getClaimId());
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
}