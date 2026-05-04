package com.claim360.denialleakage.service;

import com.claim360.denialleakage.client.NotificationServiceClient;
import com.claim360.denialleakage.client.dto.NotificationDispatchRequestDTO;
import com.claim360.denialleakage.dto.DenialPatternRequest;
import com.claim360.denialleakage.dto.DenialPatternResponse;
import com.claim360.denialleakage.entity.DenialPattern;
import com.claim360.denialleakage.entity.LeakageFlag;
import com.claim360.denialleakage.enums.LeakageType;
import com.claim360.denialleakage.exception.ResourceNotFoundException;
import com.claim360.denialleakage.repository.DenialPatternRepository;
import com.claim360.denialleakage.repository.LeakageFlagRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.scheduling.annotation.Scheduled;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class DenialPatternServiceImpl implements DenialPatternService {

    private final DenialPatternRepository denialPatternRepository;
    private final ModelMapper modelMapper;
    private final LeakageFlagRepository leakageFlagRepository;

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

    @Override
    public Map<String, Object> autoAnalyzeDenial(String claimId, String payloadJson) {
        log.info("Auto-analyzing denial/leakage for claimId: {}", claimId);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("claimId", claimId);

        // ── Extract denial fields from payload ──────────────────────────────────
        String status     = extractString(payloadJson, "status", "claimStatus");
        String denialCode = extractString(payloadJson, "denialCode");
        String reason     = extractString(payloadJson, "denialReason", "reason");

        // ── 1. Create DenialPattern if the claim is denied or carries a denial code
        boolean isDenied = "DENIED".equalsIgnoreCase(status) || denialCode != null;
        if (isDenied && denialCode != null) {
            DenialPattern pattern = new DenialPattern();
            pattern.setClaimId(claimId);
            pattern.setDenialCode(denialCode);
            pattern.setReason(reason != null ? reason : "Auto-detected denial code: " + denialCode);
            pattern.setOccurrenceDate(LocalDate.now());
            DenialPattern saved = denialPatternRepository.save(pattern);
            sendDenialNotification(saved);
            log.info("Auto-created DenialPattern id={} for claimId={}", saved.getPatternId(), claimId);
            result.put("denialPatternCreated", true);
            result.put("patternId", saved.getPatternId());
            result.put("denialCode", denialCode);
        } else {
            result.put("denialPatternCreated", false);
            result.put("info", "No denial code in payload — claim not currently denied");
        }

        // ── 2. Leakage check: Overpayment if claimAmount exceeds policyLimit ────
        double claimAmount = extractDouble(payloadJson, "claimAmount");
        double policyLimit = extractDouble(payloadJson, "policyLimit", "coverageLimit");
        if (policyLimit > 0 && claimAmount > policyLimit) {
            LeakageFlag flag = new LeakageFlag();
            flag.setClaimId(claimId);
            flag.setLeakageType(LeakageType.Overpayment);
            flag.setEstimatedLoss(claimAmount - policyLimit);
            flag.setIdentifiedDate(LocalDate.now());
            leakageFlagRepository.save(flag);
            log.info("Overpayment LeakageFlag created for claimId={}, estimatedLoss={}", claimId, claimAmount - policyLimit);
            result.put("leakageFlagCreated", true);
            result.put("leakageType", "Overpayment");
            result.put("estimatedLoss", claimAmount - policyLimit);
        } else {
            result.put("leakageFlagCreated", false);
        }

        return result;
    }

    // ── Weekly Denial-Spike Detection ────────────────────────────────────────────

    /**
     * Runs every Monday at 06:00 and compares this week's denial count with last week's.
     * A spike is defined as ≥ 20% week-over-week increase (or ≥ 5 denials with no prior
     * baseline). When a spike is detected a DENIAL notification is dispatched to ANALYST
     * and MANAGER so the team can investigate systemic denial patterns before they grow.
     */
    @Scheduled(cron = "0 0 6 * * MON")
    public void detectDenialSpike() {
        log.info("Weekly denial spike detection starting...");
        try {
            LocalDate today = LocalDate.now();
            LocalDate thisWeekStart = today.minusDays(7);
            LocalDate lastWeekStart = today.minusDays(14);

            long thisWeek = denialPatternRepository.countByOccurrenceDateBetween(thisWeekStart, today);
            long lastWeek = denialPatternRepository.countByOccurrenceDateBetween(lastWeekStart, thisWeekStart);
            log.info("Denial spike check: thisWeek={}, lastWeek={}", thisWeek, lastWeek);

            if (lastWeek > 0) {
                double ratio = (double) thisWeek / lastWeek;
                if (ratio > 1.20) {
                    sendDenialSpikeNotification(thisWeek, lastWeek, ratio);
                }
            } else if (thisWeek >= 5) {
                // No prior-week baseline but absolute count is already high
                sendDenialSpikeNotification(thisWeek, 0, Double.MAX_VALUE);
            }
        } catch (Exception e) {
            log.warn("Denial spike detection failed (non-fatal): {}", e.getMessage());
        }
    }

    private void sendDenialSpikeNotification(long thisWeek, long lastWeek, double ratio) {
        if (notificationServiceClient == null) return;
        try {
            String rateStr = Double.isInfinite(ratio)
                    ? "N/A (no prior baseline)"
                    : String.format("%.0f%%", (ratio - 1.0) * 100);
            NotificationDispatchRequestDTO notification = NotificationDispatchRequestDTO.builder()
                    .targetRoles(Set.of("ANALYST", "MANAGER"))
                    .title("Denial Spike Alert — " + thisWeek + " Denials This Week")
                    .message("Denial count this week (" + thisWeek + ") is " + rateStr
                            + " higher than last week (" + lastWeek
                            + "). Investigate for systemic denial patterns.")
                    .category("DENIAL")
                    .referenceId("denial-spike-weekly")
                    .build();
            notificationServiceClient.dispatchNotification(notification);
            log.info("Denial spike notification dispatched: thisWeek={}, lastWeek={}", thisWeek, lastWeek);
        } catch (Exception e) {
            log.warn("Failed to dispatch denial spike notification: {}", e.getMessage());
        }
    }

    // ── Payload parsing helpers ──────────────────────────────────────────────────

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
}