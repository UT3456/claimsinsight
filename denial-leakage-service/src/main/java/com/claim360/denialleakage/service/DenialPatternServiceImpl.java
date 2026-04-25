package com.claim360.denialleakage.service;

import com.claim360.denialleakage.client.NotificationServiceClient;
import com.claim360.denialleakage.client.dto.NotificationDispatchRequestDTO;
import com.claim360.denialleakage.dto.DenialPatternRequest;
import com.claim360.denialleakage.dto.DenialPatternResponse;
import com.claim360.denialleakage.entity.DenialPattern;
import com.claim360.denialleakage.exception.ResourceNotFoundException;
import com.claim360.denialleakage.repository.DenialPatternRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class DenialPatternServiceImpl implements DenialPatternService {

    private final DenialPatternRepository denialPatternRepository;
    private final ModelMapper modelMapper;

    /** Optional: null when NotificationService is unreachable. Notifications are non-blocking. */
    @Autowired(required = false)
    private NotificationServiceClient notificationServiceClient;

    @Override
    public DenialPatternResponse createDenialPattern(DenialPatternRequest request) {
        log.info("Creating DenialPattern for claimId: {}", request.getClaimId());
        DenialPattern pattern = modelMapper.map(request, DenialPattern.class);
        DenialPattern saved = denialPatternRepository.save(pattern);
        log.info("DenialPattern created with ID: {}", saved.getPatternId());
        sendDenialNotification(saved);
        return modelMapper.map(saved, DenialPatternResponse.class);
    }

    private void sendDenialNotification(DenialPattern pattern) {
        if (notificationServiceClient == null) return;
        try {
            // Dispatch to ANALYST (owns the appeal workflow) + MANAGER (approves
            // rework / escalation) instead of hardcoding userId=1L.
            NotificationDispatchRequestDTO notification = NotificationDispatchRequestDTO.builder()
                    .targetRoles(Set.of("ANALYST", "MANAGER"))
                    .title("Denial Pattern Flagged - Claim " + pattern.getClaimId())
                    .message("Claim " + pattern.getClaimId() + " recorded denial code "
                            + pattern.getDenialCode() + ". Review the denial reason and consider appeal eligibility.")
                    .category("DENIAL")
                    .referenceId(pattern.getClaimId())
                    .build();
            notificationServiceClient.dispatchNotification(notification);
            log.info("Dispatched DENIAL notification for claim {}", pattern.getClaimId());
        } catch (Exception e) {
            log.warn("Failed to dispatch denial notification for claim {}: {}", pattern.getClaimId(), e.getMessage());
        }
    }

    @Override
    public DenialPatternResponse getDenialPatternById(Long patternId) {
        log.info("Fetching DenialPattern with ID: {}", patternId);
        DenialPattern pattern = denialPatternRepository.findById(patternId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "DenialPattern", "patternId", patternId));
        return modelMapper.map(pattern, DenialPatternResponse.class);
    }

    @Override
    public List<DenialPatternResponse> getAllDenialPatterns() {
        log.info("Fetching all DenialPatterns");
        return denialPatternRepository.findAll()
                .stream()
                .map(pattern -> modelMapper.map(pattern, DenialPatternResponse.class))
                .collect(Collectors.toList());
    }

    @Override
    public List<DenialPatternResponse> getDenialPatternsByClaimId(String claimId) {
        log.info("Fetching DenialPatterns for claimId: {}", claimId);
        List<DenialPattern> patterns = denialPatternRepository.findByClaimId(claimId);
        if (patterns.isEmpty()) {
            throw new ResourceNotFoundException("DenialPattern", "claimId", claimId);
        }
        return patterns.stream()
                .map(pattern -> modelMapper.map(pattern, DenialPatternResponse.class))
                .collect(Collectors.toList());
    }

    @Override
    public List<DenialPatternResponse> getDenialPatternsByDenialCode(String denialCode) {
        log.info("Fetching DenialPatterns by denialCode: {}", denialCode);
        List<DenialPattern> patterns = denialPatternRepository.findByDenialCode(denialCode);
        if (patterns.isEmpty()) {
            throw new ResourceNotFoundException("DenialPattern", "denialCode", denialCode);
        }
        return patterns.stream()
                .map(pattern -> modelMapper.map(pattern, DenialPatternResponse.class))
                .collect(Collectors.toList());
    }

    @Override
    public List<DenialPatternResponse> getDenialPatternsByReasonKeyword(String keyword) {
        log.info("Fetching DenialPatterns by keyword: {}", keyword);
        List<DenialPattern> patterns = denialPatternRepository
                .findByReasonContainingIgnoreCase(keyword);
        if (patterns.isEmpty()) {
            throw new ResourceNotFoundException(
                    "DenialPattern", "reason containing", keyword);
        }
        return patterns.stream()
                .map(pattern -> modelMapper.map(pattern, DenialPatternResponse.class))
                .collect(Collectors.toList());
    }

    @Override
    public List<DenialPatternResponse> getDenialPatternsByClaimIdAndDenialCode(
            String claimId, String denialCode) {
        log.info("Fetching DenialPatterns by claimId: {} and denialCode: {}",
                claimId, denialCode);
        List<DenialPattern> patterns = denialPatternRepository
                .findByClaimIdAndDenialCode(claimId, denialCode);
        if (patterns.isEmpty()) {
            throw new ResourceNotFoundException(
                    "DenialPattern", "claimId and denialCode",
                    claimId + " / " + denialCode);
        }
        return patterns.stream()
                .map(pattern -> modelMapper.map(pattern, DenialPatternResponse.class))
                .collect(Collectors.toList());
    }

    @Override
    public DenialPatternResponse updateDenialPattern(Long patternId,
                                                     DenialPatternRequest request) {
        log.info("Updating DenialPattern with ID: {}", patternId);
        DenialPattern existing = denialPatternRepository.findById(patternId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "DenialPattern", "patternId", patternId));
        modelMapper.map(request, existing);
        existing.setPatternId(patternId);
        DenialPattern updated = denialPatternRepository.save(existing);
        log.info("DenialPattern updated with ID: {}", updated.getPatternId());
        return modelMapper.map(updated, DenialPatternResponse.class);
    }

    @Override
    public void deleteDenialPattern(Long patternId) {
        log.info("Deleting DenialPattern with ID: {}", patternId);
        DenialPattern existing = denialPatternRepository.findById(patternId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "DenialPattern", "patternId", patternId));
        denialPatternRepository.delete(existing);
        log.info("DenialPattern deleted with ID: {}", patternId);
    }
}