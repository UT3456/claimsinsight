package com.claim360.fraudrisk.service;

import com.claim360.fraudrisk.client.NotificationServiceClient;
import com.claim360.fraudrisk.client.dto.NotificationDispatchRequestDTO;
import com.claim360.fraudrisk.dto.CreateInvestigationRequestDTO;
import com.claim360.fraudrisk.dto.InvestigationDTO;
import com.claim360.fraudrisk.dto.UpdateInvestigationRequestDTO;
import com.claim360.fraudrisk.entity.Investigation;
import com.claim360.fraudrisk.enums.InvestigationStatus;
import com.claim360.fraudrisk.exception.ResourceNotFoundException;
import com.claim360.fraudrisk.repository.InvestigationRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * Investigation lifecycle service.
 *
 * Responsibilities:
 *   - open(): persist a new Investigation row and fire a notification to the
 *     SIU manager / fraud-team roles
 *   - update(): change status / assignee / notes (sets closedAt when CLOSED)
 *   - delete(): hard-delete (admin / cleanup only)
 *
 * NotificationServiceClient is @Autowired(required=false) so a temporary
 * outage in NotificationService cannot block escalation — the investigation
 * is still persisted, the notification dispatch is logged WARN.
 */
@Slf4j
@Service
public class InvestigationService {

    private final InvestigationRepository investigationRepository;

    @Autowired(required = false)
    private NotificationServiceClient notificationServiceClient;

    public InvestigationService(InvestigationRepository investigationRepository) {
        this.investigationRepository = investigationRepository;
    }

    @Transactional(readOnly = true)
    public List<InvestigationDTO> findAll() {
        return investigationRepository.findAll().stream()
                .map(InvestigationDTO::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<InvestigationDTO> findByStatus(InvestigationStatus status) {
        return investigationRepository.findByStatusOrderByOpenedAtDesc(status).stream()
                .map(InvestigationDTO::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<InvestigationDTO> findByClaim(String claimId) {
        return investigationRepository.findByClaimIdOrderByOpenedAtDesc(claimId).stream()
                .map(InvestigationDTO::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public InvestigationDTO findById(Long id) {
        return investigationRepository.findById(id)
                .map(InvestigationDTO::from)
                .orElseThrow(() -> new ResourceNotFoundException("Investigation " + id + " not found"));
    }

    /**
     * Opens a new investigation. Throws if the same claim already has an
     * open (NEW or UNDER_REVIEW) investigation — prevents two analysts
     * escalating the same claim in parallel.
     */
    @Transactional
    public InvestigationDTO open(CreateInvestigationRequestDTO request, String openedBy) {
        boolean alreadyOpen = investigationRepository.existsByClaimIdAndStatusIn(
                request.getClaimId(), List.of(InvestigationStatus.NEW, InvestigationStatus.UNDER_REVIEW));
        if (alreadyOpen) {
            throw new IllegalStateException(
                "Claim " + request.getClaimId() + " already has an open investigation. "
                + "Reopen the existing case instead of creating a duplicate.");
        }

        Investigation inv = Investigation.builder()
                .claimId(request.getClaimId())
                .riskScoreId(request.getRiskScoreId())
                .status(InvestigationStatus.NEW)
                .openedBy(openedBy != null ? openedBy : "unknown")
                .notes(request.getNotes())
                .build();

        Investigation saved = investigationRepository.save(inv);
        log.info("[siu] Investigation #{} opened on claim {} by {}",
                 saved.getInvestigationId(), saved.getClaimId(), saved.getOpenedBy());

        notifyFraudTeam(saved);

        return InvestigationDTO.from(saved);
    }

    @Transactional
    public InvestigationDTO update(Long id, UpdateInvestigationRequestDTO request) {
        Investigation inv = investigationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Investigation " + id + " not found"));

        if (request.getStatus() != null) {
            inv.setStatus(request.getStatus());
            // Stamp closedAt the first time we transition to CLOSED.
            if (request.getStatus() == InvestigationStatus.CLOSED && inv.getClosedAt() == null) {
                inv.setClosedAt(LocalDateTime.now());
            }
        }
        if (request.getAssignedTo() != null) inv.setAssignedTo(request.getAssignedTo());
        if (request.getNotes()      != null) inv.setNotes(request.getNotes());

        Investigation saved = investigationRepository.save(inv);
        log.info("[siu] Investigation #{} updated → status={} assignedTo={}",
                 id, saved.getStatus(), saved.getAssignedTo());

        return InvestigationDTO.from(saved);
    }

    @Transactional
    public void delete(Long id) {
        if (!investigationRepository.existsById(id)) {
            throw new ResourceNotFoundException("Investigation " + id + " not found");
        }
        investigationRepository.deleteById(id);
        log.info("[siu] Investigation #{} deleted", id);
    }

    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Fan-outs a Risk-category notification to the FRAUD and ADMIN role groups
     * so the SIU manager's bell lights up the moment an analyst escalates.
     * Fire-and-forget — failures are logged WARN but never block the escalation.
     */
    private void notifyFraudTeam(Investigation inv) {
        if (notificationServiceClient == null) {
            log.warn("[siu] NotificationService unavailable — escalation #{} created but no alert dispatched",
                     inv.getInvestigationId());
            return;
        }
        try {
            // Drop the role prefix on the actor — when an admin or fraud-team member
            // does the escalation, hard-coding "Analyst {name}" reads wrong. Just use
            // the username, which is unambiguous.
            String openedBy = inv.getOpenedBy() != null ? inv.getOpenedBy() : "an analyst";
            String body = "Claim " + inv.getClaimId() + " flagged for SIU review by " + openedBy + ".";
            if (inv.getNotes() != null && !inv.getNotes().isBlank()) {
                body += " Notes: " + inv.getNotes();
            }
            NotificationDispatchRequestDTO dispatch = NotificationDispatchRequestDTO.builder()
                    .targetRoles(Set.of("FRAUD", "ADMIN"))
                    .title("SIU escalation — " + inv.getClaimId())
                    .message(body)
                    .category("RISK")
                    .referenceId(inv.getClaimId())
                    .build();
            notificationServiceClient.dispatchNotification(dispatch);
            log.info("[siu] Notification dispatched to FRAUD+ADMIN for investigation #{}",
                     inv.getInvestigationId());
        } catch (Exception e) {
            log.warn("[siu] Notification dispatch failed for investigation #{} (non-fatal): {}",
                     inv.getInvestigationId(), e.getMessage());
        }
    }
}
