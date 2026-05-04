package com.demo.service;

import com.demo.dto.SLAViolationDTO;

import java.util.Date;
import java.util.List;

/**
 * Service Interface for tracking and analyzing Service Level Agreement (SLA) Violations.
 */
public interface SLAViolationService {

    List<SLAViolationDTO> getAllViolations();

    SLAViolationDTO recordSLAViolation(SLAViolationDTO dto);

    List<SLAViolationDTO> getSLAViolationsByAdjuster(Long adjusterId);

    List<SLAViolationDTO> getSLAViolationsByAdjusterAndDateRange(Long adjusterId, Date start, Date end);

    List<SLAViolationDTO> getSLAViolationsByClaim(Long claimId);

    List<SLAViolationDTO> getViolationsByType(String violationType);

    List<SLAViolationDTO> getViolationsByDaysOverdueGreaterThan(int days);

    List<SLAViolationDTO> getViolationsBySeverity(String severity);

    List<SLAViolationDTO> getViolationsByDateRange(Date start, Date end);

    Integer getTotalDaysOverdueByClaim(Long claimId);

    Long countViolationsByAdjusterAndPeriod(Long adjusterId, Date start, Date end);

    List<SLAViolationDTO> getEscalationCandidatesByAdjuster(Long adjusterId);

    public SLAViolationDTO updateSLAViolation(Long violationId, SLAViolationDTO dto);

    public SLAViolationDTO updateEscalationStatus(Long violationId, boolean escalated);

    public SLAViolationDTO changeViolationSeverity(Long violationId, String newLevel);

    void deleteViolation(Long violationId);

    /**
     * Creates an SLA tracking record automatically when a claim is ingested.
     * Uses sensible defaults (CLAIM_ASSESSMENT type, 30-day target, 31 actual days)
     * so a LOW-severity breach is recorded and appears on the SLA violations page
     * immediately after a claim enters the system.
     *
     * @param claimRef  string claim ID from data-ingestion-service (e.g. "CLM-2026-AUTO-001")
     * @param adjusterId adjuster to assign; falls back to 1 when null
     */
    /**
     * @param claimRef    string claim ID from ingestion service
     * @param adjusterId  adjuster to assign (defaults to 1 when null)
     * @param actualDays  real elapsed days from incident date to today (defaults to 31 when null)
     * @param slaTargetDays SLA deadline in days (defaults to 30 when null)
     */
    SLAViolationDTO autoGenerateSlaViolation(String claimRef, Long adjusterId,
                                             Integer actualDays, Integer slaTargetDays);
}