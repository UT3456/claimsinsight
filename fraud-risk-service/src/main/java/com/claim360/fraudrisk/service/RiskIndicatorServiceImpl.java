package com.claim360.fraudrisk.service;

import com.claim360.fraudrisk.dto.RiskIndicatorRequest;
import com.claim360.fraudrisk.dto.RiskIndicatorResponse;
import com.claim360.fraudrisk.entity.RiskIndicator;
import com.claim360.fraudrisk.enums.IndicatorType;
import com.claim360.fraudrisk.exception.ResourceNotFoundException;
import com.claim360.fraudrisk.repository.RiskIndicatorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RiskIndicatorServiceImpl implements RiskIndicatorService {

    private final RiskIndicatorRepository riskIndicatorRepository;
    private final ModelMapper modelMapper;

    // ── Create ──────────────────────────────────────────────────────────────

    @Override
    public RiskIndicatorResponse createRiskIndicator(RiskIndicatorRequest request) {
        log.info("Creating RiskIndicator for claimId: {}", request.getClaimId());
        RiskIndicator indicator = modelMapper.map(request, RiskIndicator.class);
        RiskIndicator saved = riskIndicatorRepository.save(indicator);
        log.info("RiskIndicator created with ID: {}", saved.getIndicatorId());
        return modelMapper.map(saved, RiskIndicatorResponse.class);
    }

    // ── Read ────────────────────────────────────────────────────────────────

    @Override
    public RiskIndicatorResponse getRiskIndicatorById(Long indicatorId) {
        log.info("Fetching RiskIndicator with ID: {}", indicatorId);
        RiskIndicator indicator = riskIndicatorRepository.findById(indicatorId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "RiskIndicator", "indicatorId", indicatorId));
        return modelMapper.map(indicator, RiskIndicatorResponse.class);
    }

    @Override
    public List<RiskIndicatorResponse> getAllRiskIndicators() {
        log.info("Fetching all RiskIndicators");
        return riskIndicatorRepository.findAll()
                .stream()
                .map(indicator -> modelMapper.map(indicator, RiskIndicatorResponse.class))
                .collect(Collectors.toList());
    }

    @Override
    public List<RiskIndicatorResponse> getRiskIndicatorsByClaimId(String claimId) {
        log.info("Fetching RiskIndicators for claimId: {}", claimId);
        List<RiskIndicator> indicators = riskIndicatorRepository.findByClaimId(claimId);
        if (indicators.isEmpty()) {
            throw new ResourceNotFoundException("RiskIndicator", "claimId", claimId);
        }
        return indicators.stream()
                .map(indicator -> modelMapper.map(indicator, RiskIndicatorResponse.class))
                .collect(Collectors.toList());
    }

    @Override
    public List<RiskIndicatorResponse> getRiskIndicatorsByType(IndicatorType indicatorType) {
        log.info("Fetching RiskIndicators by type: {}", indicatorType);
        List<RiskIndicator> indicators = riskIndicatorRepository
                .findByIndicatorType(indicatorType);
        if (indicators.isEmpty()) {
            throw new ResourceNotFoundException(
                    "RiskIndicator", "indicatorType", indicatorType);
        }
        return indicators.stream()
                .map(indicator -> modelMapper.map(indicator, RiskIndicatorResponse.class))
                .collect(Collectors.toList());
    }

    @Override
    public List<RiskIndicatorResponse> getRiskIndicatorsBySeverity(String severity) {
        log.info("Fetching RiskIndicators by severity: {}", severity);
        List<RiskIndicator> indicators = riskIndicatorRepository.findBySeverity(severity);
        if (indicators.isEmpty()) {
            throw new ResourceNotFoundException("RiskIndicator", "severity", severity);
        }
        return indicators.stream()
                .map(indicator -> modelMapper.map(indicator, RiskIndicatorResponse.class))
                .collect(Collectors.toList());
    }

    // ── Update ──────────────────────────────────────────────────────────────

    @Override
    public RiskIndicatorResponse updateRiskIndicator(Long indicatorId,
                                                     RiskIndicatorRequest request) {
        log.info("Updating RiskIndicator with ID: {}", indicatorId);
        RiskIndicator existing = riskIndicatorRepository.findById(indicatorId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "RiskIndicator", "indicatorId", indicatorId));

        // Map updated fields from request to existing entity
        modelMapper.map(request, existing);
        existing.setIndicatorId(indicatorId);

        RiskIndicator updated = riskIndicatorRepository.save(existing);
        log.info("RiskIndicator updated with ID: {}", updated.getIndicatorId());
        return modelMapper.map(updated, RiskIndicatorResponse.class);
    }

    // ── Delete ──────────────────────────────────────────────────────────────

    @Override
    public void deleteRiskIndicator(Long indicatorId) {
        log.info("Deleting RiskIndicator with ID: {}", indicatorId);
        RiskIndicator existing = riskIndicatorRepository.findById(indicatorId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "RiskIndicator", "indicatorId", indicatorId));
        riskIndicatorRepository.delete(existing);
        log.info("RiskIndicator deleted with ID: {}", indicatorId);
    }
}