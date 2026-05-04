package com.claim360.fraudrisk.service;

import com.claim360.fraudrisk.dto.RiskScoreRequest;
import com.claim360.fraudrisk.dto.RiskScoreResponse;

import java.util.List;

public interface RiskScoreService {

    // Create a new risk score
    RiskScoreResponse createRiskScore(RiskScoreRequest request);

    // Get a risk score by ID
    RiskScoreResponse getRiskScoreById(Long scoreId);

    // Get all risk scores
    List<RiskScoreResponse> getAllRiskScores();

    // Get all risk scores for a specific claim
    List<RiskScoreResponse> getRiskScoresByClaimId(String claimId);

    // Get the latest risk score for a specific claim
    RiskScoreResponse getLatestRiskScoreByClaimId(String claimId);

    // Get all risk scores above a threshold
    List<RiskScoreResponse> getRiskScoresAboveThreshold(Double threshold);

    // Update a risk score
    RiskScoreResponse updateRiskScore(Long scoreId, RiskScoreRequest request);

    // Delete a risk score
    void deleteRiskScore(Long scoreId);

    // Auto-evaluate fraud risk from a raw ingestion payload (called by data-ingestion-service)
    RiskScoreResponse autoEvaluateRisk(String claimId, String payloadJson);
}