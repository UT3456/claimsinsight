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
}