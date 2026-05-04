package com.demo.service;

import com.demo.client.NotificationServiceClient;
import com.demo.client.dto.NotificationDispatchRequestDTO;
import com.demo.dto.SLAViolationDTO;
import com.demo.entities.SLAViolation;
import com.demo.exception.DatabaseOperationException;
import com.demo.exception.InvalidInputException;
import com.demo.exception.ResourceNotFoundException;
import com.demo.repositories.SLAViolationRepository;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

/**
 * Service Implementation for tracking and analyzing Service Level Agreement (SLA) Violations.
 */
@Service
@CacheConfig(cacheNames = "sla_violations")
public class SLAViolationServiceImpl implements SLAViolationService {

    private static final Logger log = LoggerFactory.getLogger(SLAViolationServiceImpl.class);

    private static final int ESCALATION_DAYS_THRESHOLD  = 5;

    private static final int ESCALATION_COUNT_THRESHOLD = 3;

    private final SLAViolationRepository repository;
    private final ModelMapper modelMapper;
    private final NotificationServiceClient notificationServiceClient;

    public SLAViolationServiceImpl(SLAViolationRepository repository,
                                   ModelMapper modelMapper,
                                   NotificationServiceClient notificationServiceClient) {
        this.repository = repository;
        this.modelMapper = modelMapper;
        this.notificationServiceClient = notificationServiceClient;
    }

    @Override
    @CacheEvict(allEntries = true)
    public SLAViolationDTO recordSLAViolation(SLAViolationDTO dto) {
        try {
            validateViolationInput(dto);
            SLAViolation entity = new SLAViolation();
            entity.setAdjusterId(dto.getAdjusterId());
            entity.setViolationType(dto.getViolationType());
            entity.setSlaTargetDays(dto.getSlaTargetDays());
            entity.setActualDays(dto.getActualDays());
            entity.setViolationDate(dto.getViolationDate());
            entity.setClaimId(dto.getClaimId());

            SLAViolation saved = repository.save(entity);
            SLAViolationDTO result = computeAndEnrich(saved);

            if ("CRITICAL".equals(result.getSeverity())) {
                sendCriticalViolationNotification(result);
            }

            return result;
        } catch (InvalidInputException e) {
            throw e;
        } catch (Exception e) {
            throw new DatabaseOperationException("Error recording SLA violation: " + e.getMessage());
        }
    }

    private void sendCriticalViolationNotification(SLAViolationDTO dto) {
        try {
            // Dispatch to every MANAGER + EXECUTIVE so the right ops people see
            // a CRITICAL breach — not a single hardcoded system user.
            NotificationDispatchRequestDTO notification = NotificationDispatchRequestDTO.builder()
                    .targetRoles(Set.of("MANAGER", "EXECUTIVE"))
                    .title("Critical SLA Violation - Adjuster " + dto.getAdjusterId())
                    .message("Adjuster " + dto.getAdjusterId() + " has a CRITICAL SLA violation ("
                            + dto.getViolationType() + ") with " + dto.getDaysOverdue()
                            + " days overdue for claim " + dto.getClaimId() + ".")
                    .category("PERFORMANCE")
                    .referenceId(dto.getClaimId() != null ? String.valueOf(dto.getClaimId()) : null)
                    .build();
            notificationServiceClient.dispatchNotification(notification);
        } catch (Exception e) {
            log.warn("Failed to send critical SLA violation notification for adjuster {}: {}",
                    dto.getAdjusterId(), e.getMessage());
        }
    }

    @Override
    public List<SLAViolationDTO> getAllViolations() {
        List<SLAViolation> list = repository.findAll();
        List<SLAViolationDTO> dtos = new ArrayList<>();
        for (SLAViolation v : list) dtos.add(computeAndEnrich(v));
        return dtos;
    }

    @Cacheable(key = "'adj-' + #adjusterId")
    @Override
    public List<SLAViolationDTO> getSLAViolationsByAdjuster(Long adjusterId) {
        try {
            if (adjusterId == null) throw new InvalidInputException("Adjuster ID is required.");
            List<SLAViolation> list = repository.findByAdjusterId(adjusterId);
            return mapAndEnrichList(list, "adjuster ID: " + adjusterId);
        } catch (Exception e) {
            return handleFetchException(e);
        }
    }

    @Override
    @Cacheable(key = "'adj-range-' + #adjusterId + '-' + #start.getTime() + '-' + #end.getTime()")
    public List<SLAViolationDTO> getSLAViolationsByAdjusterAndDateRange(Long adjusterId, Date start, Date end) {
        try {
            validateDateRange(start, end);
            List<SLAViolation> list = repository.findByAdjusterIdAndViolationDateBetween(adjusterId, start, end);
            return mapAndEnrichList(list, "adjuster range");
        } catch (Exception e) {
            return handleFetchException(e);
        }
    }

    @Override
    @Cacheable(key = "'claim-' + #claimId")
    public List<SLAViolationDTO> getSLAViolationsByClaim(Long claimId) {
        try {
            if (claimId == null) throw new InvalidInputException("Claim ID is required.");
            List<SLAViolation> list = repository.findByClaimId(claimId);
            return mapAndEnrichList(list, "claim ID: " + claimId);
        } catch (Exception e) {
            return handleFetchException(e);
        }
    }

    @Override
    @Cacheable(key = "'type-' + #violationType")
    public List<SLAViolationDTO> getViolationsByType(String violationType) {
        try {
            if (violationType == null || violationType.isBlank()) throw new InvalidInputException("Type is required.");
            List<SLAViolation> list = repository.findByViolationType(violationType);
            return mapAndEnrichList(list, "type: " + violationType);
        } catch (Exception e) {
            return handleFetchException(e);
        }
    }

    @Override
    @Cacheable(key = "'overdue-' + #days")
    public List<SLAViolationDTO> getViolationsByDaysOverdueGreaterThan(int days) {
        try {
            if (days < 0) throw new InvalidInputException("Days cannot be negative.");
            List<SLAViolation> list = repository.findByDaysOverdueGreaterThan(days);
            return mapAndEnrichList(list, "overdue days > " + days);
        } catch (Exception e) {
            return handleFetchException(e);
        }
    }

    @Override
    @Cacheable(key = "'severity-' + #severity")
    public List<SLAViolationDTO> getViolationsBySeverity(String severity) {
        try {
            if (severity == null) throw new InvalidInputException("Severity is required.");
            int minDays, maxDays;
            switch (severity.toUpperCase()) {
                case "LOW":      minDays = 1;  maxDays = 2;    break;
                case "MEDIUM":   minDays = 3;  maxDays = 5;    break;
                case "HIGH":     minDays = 6;  maxDays = 10;   break;
                case "CRITICAL": minDays = 11; maxDays = 9999; break;
                default: throw new InvalidInputException("Invalid severity: " + severity);
            }
            List<SLAViolation> list = repository.findBySeverityRange(minDays, maxDays);
            return mapAndEnrichList(list, "severity: " + severity);
        } catch (Exception e) {
            return handleFetchException(e);
        }
    }

    @Override
    @Cacheable(key = "'range-' + #start.getTime() + '-' + #end.getTime()")
    public List<SLAViolationDTO> getViolationsByDateRange(Date start, Date end) {
        try {
            validateDateRange(start, end);
            List<SLAViolation> list = repository.findByViolationDateBetween(start, end);
            return mapAndEnrichList(list, "date range");
        } catch (Exception e) {
            return handleFetchException(e);
        }
    }

    @Override
    @Cacheable(key = "'total-overdue-' + #claimId")
    public Integer getTotalDaysOverdueByClaim(Long claimId) {
        if (claimId == null) throw new InvalidInputException("Claim ID required.");
        return repository.findTotalDaysOverdueByClaim(claimId);
    }

    @Override
    @Cacheable(key = "'count-' + #adjusterId + '-' + #start.getTime() + '-' + #end.getTime()")
    public Long countViolationsByAdjusterAndPeriod(Long adjusterId, Date start, Date end) {
        validateDateRange(start, end);
        return repository.countViolationsByAdjusterAndPeriod(adjusterId, start, end);
    }

    @Override
    @Cacheable(key = "'escalations-' + #adjusterId")
    public List<SLAViolationDTO> getEscalationCandidatesByAdjuster(Long adjusterId) {
        try {
            List<SLAViolation> list = repository.findEscalationCandidatesByAdjuster(adjusterId);
            return mapAndEnrichList(list, "escalations for adjuster " + adjusterId);
        } catch (Exception e) {
            return handleFetchException(e);
        }
    }

    @Override
    @CacheEvict(allEntries = true)
    public SLAViolationDTO updateSLAViolation(Long violationId, SLAViolationDTO dto) {
        try {
            validateViolationInput(dto);
            SLAViolation existing = repository.findById(violationId).orElseThrow(() -> new ResourceNotFoundException("SLA Violation not found: " + violationId));
            existing.setViolationType(dto.getViolationType());
            existing.setSlaTargetDays(dto.getSlaTargetDays());
            existing.setActualDays(dto.getActualDays());
            existing.setViolationDate(dto.getViolationDate());
            existing.setAdjusterId(dto.getAdjusterId());
            existing.setClaimId(dto.getClaimId());

            SLAViolation updated = repository.save(existing);
            return computeAndEnrich(updated);
        } catch (Exception e) {
            throw new DatabaseOperationException("Error updating SLA violation: " + e.getMessage());
        }
    }

    @Override
    @CacheEvict(allEntries = true)
    public SLAViolationDTO updateEscalationStatus(Long violationId, boolean escalated) {
        SLAViolation entity = repository.findById(violationId).orElseThrow(() -> new ResourceNotFoundException("Violation not found: " + violationId));
        SLAViolationDTO dto = computeAndEnrich(entity);
        dto.setEscalated(escalated);
        return dto;
    }

    @Override
    @CacheEvict(allEntries = true)
    public SLAViolationDTO changeViolationSeverity(Long violationId, String newLevel) {
        SLAViolation entity = repository.findById(violationId).orElseThrow(() -> new ResourceNotFoundException("Violation not found: " + violationId));
        return computeAndEnrich(entity);
    }

    @Override
    @CacheEvict(allEntries = true)
    public void deleteViolation(Long violationId) {
        if (violationId == null) throw new InvalidInputException("ID required.");
        if (!repository.existsById(violationId)) throw new ResourceNotFoundException("Not found.");
        repository.deleteById(violationId);
    }

    private SLAViolationDTO computeAndEnrich(SLAViolation entity) {
        SLAViolationDTO dto = modelMapper.map(entity, SLAViolationDTO.class);

        int daysOverdue = entity.getActualDays() - entity.getSlaTargetDays();
        dto.setDaysOverdue(daysOverdue);

        // Assign Severity Label
        String severity = (daysOverdue <= 2) ? "LOW" :
                (daysOverdue <= 5) ? "MEDIUM" :
                        (daysOverdue <= 10) ? "HIGH" : "CRITICAL";
        dto.setSeverity(severity);

        // Determine Escalation Status
        long totalViolations = repository.countAllViolationsByAdjuster(entity.getAdjusterId());
        boolean escalated = daysOverdue > ESCALATION_DAYS_THRESHOLD || totalViolations > ESCALATION_COUNT_THRESHOLD;
        dto.setEscalated(escalated);

        return dto;
    }



    private List<SLAViolationDTO> mapAndEnrichList(List<SLAViolation> list, String context) {
        if (list.isEmpty()) throw new ResourceNotFoundException("No violations found for " + context);
        List<SLAViolationDTO> dtos = new ArrayList<>();
        for (SLAViolation v : list) dtos.add(computeAndEnrich(v));
        return dtos;
    }

    private void validateDateRange(Date start, Date end) {
        if (start == null || end == null) throw new InvalidInputException("Dates required.");
        if (start.after(end)) throw new InvalidInputException("Start date cannot be after end date.");
    }

    private List<SLAViolationDTO> handleFetchException(Exception e) {
        if (e instanceof InvalidInputException || e instanceof ResourceNotFoundException) throw (RuntimeException) e;
        throw new DatabaseOperationException("Fetch error: " + e.getMessage());
    }

    /**
     * Creates a default SLA tracking violation for a claim that was just ingested.
     * Bypasses the breach-validation gate (which requires actualDays > slaTargetDays)
     * because the record is generated automatically — not entered manually.
     *
     * <p>claimRef is a String (e.g. "CLM-2026-AUTO-001") whereas the SLAViolation
     * entity stores a Long claimId. We derive a stable Long via hashCode so the
     * same claimRef always maps to the same numeric ID across calls.</p>
     */
    /**
     * Auto-generates a real SLA violation using the elapsed days computed from
     * the claim's incident date in the payload JSON.
     *
     * <p>Severity is driven by {@code daysOverdue = actualDays - slaTargetDays}:</p>
     * <ul>
     *   <li>≤ 2 days  → LOW</li>
     *   <li>≤ 5 days  → MEDIUM</li>
     *   <li>≤ 10 days → HIGH</li>
     *   <li>> 10 days → CRITICAL  (fires a notification to MANAGER + EXECUTIVE)</li>
     * </ul>
     */
    @Override
    @CacheEvict(allEntries = true)
    public SLAViolationDTO autoGenerateSlaViolation(String claimRef, Long adjusterId,
                                                    Integer actualDays, Integer slaTargetDays) {
        if (claimRef == null || claimRef.isBlank()) {
            throw new com.demo.exception.InvalidInputException("claimRef is required for auto-generation.");
        }
        try {
            // Derive a stable positive Long from the string claim reference.
            long numericClaimId = Math.abs((long) claimRef.hashCode()) % 9_999_999L + 1L;

            int target = (slaTargetDays != null && slaTargetDays > 0) ? slaTargetDays : 30;
            // Use real elapsed days when provided; fall back to target+1 (LOW breach placeholder).
            int actual = (actualDays != null && actualDays >= 1) ? actualDays : target + 1;

            SLAViolation entity = new SLAViolation();
            entity.setClaimId(numericClaimId);
            entity.setAdjusterId(adjusterId != null ? adjusterId : 1L);
            entity.setViolationType("CLAIM_ASSESSMENT");
            entity.setSlaTargetDays(target);
            entity.setActualDays(actual);
            entity.setViolationDate(new Date());

            SLAViolation saved = repository.save(entity);
            SLAViolationDTO result = computeAndEnrich(saved);

            log.info("Auto-generated SLA violation for claimRef '{}' → violationId={}, " +
                     "actualDays={}, slaTarget={}, daysOverdue={}, severity={}",
                    claimRef, saved.getViolationId(), actual, target,
                    result.getDaysOverdue(), result.getSeverity());

            // Fire notification for CRITICAL violations (severity computed inside computeAndEnrich).
            if ("CRITICAL".equals(result.getSeverity())) {
                sendCriticalViolationNotification(result);
            }

            return result;
        } catch (Exception e) {
            throw new DatabaseOperationException("Error auto-generating SLA violation for '" + claimRef + "': " + e.getMessage());
        }
    }

    private void validateViolationInput(SLAViolationDTO dto) {
        if (dto == null) throw new InvalidInputException("Data is null.");
        if (dto.getActualDays() <= dto.getSlaTargetDays()) {
            throw new InvalidInputException("No breach detected. Actual days must exceed target days.");
        }
    }
}