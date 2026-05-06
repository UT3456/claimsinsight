package com.demo.services;

import com.demo.dto.NotificationRequestDTO;
import com.demo.dto.NotificationResponseDTO;
import com.demo.dto.NotificationStatusUpdateDTO;
import com.demo.dto.NotificationDispatchRequestDTO;
import com.demo.entities.*;
import com.demo.enums.NotificationCategory;
import com.demo.enums.NotificationStatus;
import com.demo.enums.UserRole;
import com.demo.exception.NotificationNotFoundException;
import com.demo.repository.NotificationRepository;
import com.demo.client.FraudRiskServiceClient;
import com.demo.client.AdjusterOperationsClient;
import com.demo.client.CostReserveAgingClient;
import com.demo.client.dto.RiskScoreDTO;
import com.demo.client.dto.SLAViolationDTO;
import com.demo.client.dto.AgingRecordDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationStreamService streamService;

    @PersistenceContext
    private EntityManager entityManager;

    /** Optional: null when fraud-risk-service is unreachable. Alerts are non-blocking. */
    @Autowired(required = false)
    private FraudRiskServiceClient fraudRiskServiceClient;

    /** Optional: null when AdjusterAndOperations is unreachable. Alerts are non-blocking. */
    @Autowired(required = false)
    private AdjusterOperationsClient adjusterOperationsClient;

    /** Optional: null when cost-reserve-service is unreachable. Alerts are non-blocking. */
    @Autowired(required = false)
    private CostReserveAgingClient costReserveAgingClient;

    private static final int  RISK_SCORE_THRESHOLD   = 61;

    private static final long SLA_VIOLATION_THRESHOLD = 3;

    private static final List<String> CRITICAL_AGING_BUCKETS = List.of("91-120", "120+");

    private static final List<String> STUCK_STATUSES = List.of("OPEN", "PENDING");


    @Override
    @Transactional
    @CacheEvict(value = "userNotifications", key = "#request.userId")
    public NotificationResponseDTO createNotification(NotificationRequestDTO request) {
        Notification notification = Notification.builder()
                .userId(request.getUserId())
                .title(request.getTitle())
                .message(request.getMessage())
                .category(request.getCategory())
                .referenceId(request.getReferenceId())
                .status(NotificationStatus.UNREAD)
                .createdDate(LocalDateTime.now())
                .build();

        Notification saved = notificationRepository.save(notification);
        streamService.broadcast(saved);   // SSE push
        return toDTO(saved);
    }

        @Override
        @Transactional
        @CacheEvict(value = "userNotifications", allEntries = true)
        public long dispatchNotification(NotificationDispatchRequestDTO request) {
                if (request == null || !request.hasTargets()) {
                        return 0;
                }

                Set<Long> recipientIds = new LinkedHashSet<>();

                if (request.getTargetUserIds() != null && !request.getTargetUserIds().isEmpty()) {
                        List<Long> ids = entityManager
                                        .createQuery("SELECT u.userId FROM User u WHERE u.userId IN :ids AND u.isActive = true", Long.class)
                                        .setParameter("ids", request.getTargetUserIds())
                                        .getResultList();
                        recipientIds.addAll(ids);
                }

                if (request.getTargetRoles() != null && !request.getTargetRoles().isEmpty()) {
                        List<Long> roleIds = entityManager
                                        .createQuery("SELECT u.userId FROM User u WHERE u.role IN :roles AND u.isActive = true", Long.class)
                                        .setParameter("roles", request.getTargetRoles())
                                        .getResultList();
                        recipientIds.addAll(roleIds);
                }

                if (recipientIds.isEmpty()) {
                        return 0;
                }

                // Build all notification entities then persist in a single batch INSERT.
                // With hibernate.jdbc.batch_size=50 this issues one JDBC batch per 50 rows
                // instead of one INSERT statement per recipient.
                LocalDateTime now = LocalDateTime.now();
                List<Notification> notifications = recipientIds.stream()
                        .map(userId -> Notification.builder()
                                .userId(userId)
                                .title(request.getTitle())
                                .message(request.getMessage())
                                .category(request.getCategory())
                                .referenceId(request.getReferenceId())
                                .status(NotificationStatus.UNREAD)
                                .createdDate(now)
                                .build())
                        .collect(java.util.stream.Collectors.toList());
                List<Notification> persisted = notificationRepository.saveAll(notifications);
                streamService.broadcastAll(persisted);   // SSE push to every recipient

                return persisted.size();
        }

    @Override
    @Cacheable(value = "userNotifications", key = "#userId")
    public List<NotificationResponseDTO> getNotificationsForUser(Long userId) {
        return notificationRepository.findByUserIdOrderByCreatedDateDesc(userId)
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Override
    public List<NotificationResponseDTO> getByUserIdAndStatus(Long userId, NotificationStatus status) {
        return notificationRepository.findByUserIdAndStatusOrderByCreatedDateDesc(userId, status)
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Override
    public List<NotificationResponseDTO> getByUserIdAndCategory(Long userId, NotificationCategory category) {
        return notificationRepository.findByUserIdAndCategoryOrderByCreatedDateDesc(userId, category)
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Override
    public NotificationResponseDTO getById(Long notificationId) {
        Notification notification = findOrThrow(notificationId);
        return toDTO(notification);
    }

    @Override
    @Transactional
    @CacheEvict(value = "userNotifications", key = "#result.userId")
    public NotificationResponseDTO updateStatus(Long notificationId, NotificationStatusUpdateDTO dto) {
        Notification notification = findOrThrow(notificationId);
        notification.setStatus(dto.getStatus());

        // When transitioning to READ, record the timestamp
        if (dto.getStatus() == NotificationStatus.READ && notification.getReadDate() == null) {
            notification.setReadDate(LocalDateTime.now());
        }

        Notification updated = notificationRepository.save(notification);
        return toDTO(updated);
    }

    @Override
    @Transactional
    @CacheEvict(value = "userNotifications", key = "#userId")
    public int markAllAsRead(Long userId) {
        int count = notificationRepository.markAllAsRead(userId, LocalDateTime.now());
        return count;
    }

    @Override
    public long countUnread(Long userId) {
        return notificationRepository.countByUserIdAndStatus(userId, NotificationStatus.UNREAD);
    }

    @Override
    @Transactional
    @CacheEvict(value = "userNotifications", allEntries = true)
    public void deleteNotification(Long notificationId) {
        Notification notification = findOrThrow(notificationId);
        notificationRepository.delete(notification);
    }
    /**
     * RISK ALERT — runs every 5 minutes.
     * Fetches high-risk scores directly from fraud-risk-service via Feign.
     * Skips silently if the client is unavailable (service down or not yet registered).
     */
    @Override
    @Scheduled(fixedDelay = 300_000)   // every 5 minutes
    @Transactional
    public void generateRiskAlerts() {
        log.debug("Scheduler running: generateRiskAlerts");
        if (fraudRiskServiceClient == null) {
            log.debug("FraudRiskServiceClient unavailable — skipping risk alert scan");
            return;
        }
        List<RiskScoreDTO> highRiskScores;
        try {
            highRiskScores = fraudRiskServiceClient.getRiskScoresAboveThreshold((double) RISK_SCORE_THRESHOLD);
        } catch (Exception e) {
            log.warn("Could not fetch risk scores from fraud-risk-service: {}", e.getMessage());
            return;
        }

        // Per notification policy:
        //   score ≥ 75       → FRAUD + ADMIN
        //   score ≥ 90 (critical) → FRAUD + ADMIN + MANAGER
        // Pre-load both audience sets so we don't re-query per claim.
        List<User> baseAudience = loadActiveUsers(List.of(UserRole.FRAUD, UserRole.ADMIN));
        List<User> criticalAudience = loadActiveUsers(List.of(UserRole.FRAUD, UserRole.ADMIN, UserRole.MANAGER));

        for (RiskScoreDTO score : highRiskScores) {
            boolean alreadyAlerted = notificationRepository.existsByReferenceIdAndCategoryAndStatus(
                    score.getClaimId(), NotificationCategory.RISK, NotificationStatus.UNREAD);
            if (alreadyAlerted) continue;

            boolean isCritical = score.getScoreValue() >= 90.0;
            String riskLabel = isCritical ? "CRITICAL"
                    : score.getScoreValue() >= 80.0 ? "HIGH"
                    : score.getScoreValue() >= 60.0 ? "MEDIUM" : "LOW";
            String title   = (isCritical ? "Critical risk claim — " : "High Risk Claim Detected — ") + score.getClaimId();
            String message = String.format(
                    "Claim %s has a fraud risk score of %.1f (%s). %s",
                    score.getClaimId(), score.getScoreValue(), riskLabel,
                    isCritical
                        ? "Manager review escalation triggered."
                        : "Immediate review required.");

            List<User> audience = isCritical ? criticalAudience : baseAudience;
            for (User user : audience) {
                saveSystemNotification(user.getUserId(), title, message,
                        NotificationCategory.RISK, score.getClaimId());
            }
            log.warn("RISK alert fired for claim={} score={} criticality={}",
                     score.getClaimId(), score.getScoreValue(), isCritical ? "CRITICAL" : "HIGH");
        }
    }

    /**
     * PERFORMANCE ALERT — runs every 10 minutes.
     * Fetches all SLA violations from AdjusterAndOperations via Feign, then
     * groups by adjusterId filtering to the current month in memory.
     * Skips silently if the client is unavailable.
     */
    @Override
    @Scheduled(fixedDelay = 600_000)   // every 10 minutes
    @Transactional
    public void generatePerformanceAlerts() {
        log.debug("Scheduler running: generatePerformanceAlerts");
        if (adjusterOperationsClient == null) {
            log.debug("AdjusterOperationsClient unavailable — skipping performance alert scan");
            return;
        }
        List<SLAViolationDTO> allViolations;
        try {
            allViolations = adjusterOperationsClient.getAllViolations();
        } catch (Exception e) {
            log.warn("Could not fetch SLA violations from AdjusterAndOperations: {}", e.getMessage());
            return;
        }

        LocalDate startOfMonth = LocalDate.now().withDayOfMonth(1);

        // Group violations by adjusterId, scoped to the current month.
        // violationDate null-safe: include the record if the date cannot be determined.
        Map<Long, Long> violationCounts = allViolations.stream()
                .filter(v -> v.getViolationDate() == null || !v.getViolationDate().isBefore(startOfMonth))
                .collect(Collectors.groupingBy(SLAViolationDTO::getAdjusterId, Collectors.counting()));

        // Per notification policy: SLA breach → MANAGER + ADMIN.
        List<User> audience = loadActiveUsers(List.of(UserRole.MANAGER, UserRole.ADMIN));

        for (Map.Entry<Long, Long> entry : violationCounts.entrySet()) {
            Long adjusterId     = entry.getKey();
            long violationCount = entry.getValue();
            if (violationCount < SLA_VIOLATION_THRESHOLD) continue;

            String referenceId = "ADJ-" + adjusterId;
            boolean alreadyAlerted = notificationRepository.existsByReferenceIdAndCategoryAndStatus(
                    referenceId, NotificationCategory.PERFORMANCE, NotificationStatus.UNREAD);
            if (alreadyAlerted) continue;

            String title   = "SLA Breach Threshold Exceeded — Adjuster #" + adjusterId;
            String message = String.format(
                    "Adjuster %d has breached SLA on %d claims this month. Threshold is %d. Please review workload.",
                    adjusterId, violationCount, SLA_VIOLATION_THRESHOLD);

            for (User user : audience) {
                saveSystemNotification(user.getUserId(), title, message,
                        NotificationCategory.PERFORMANCE, referenceId);
            }
            log.warn("PERFORMANCE alert fired for adjusterId={} violations={}", adjusterId, violationCount);
        }
    }

    /**
     * AGING ALERT — runs daily at midnight.
     * Fetches BUCKET_90_PLUS records directly from cost-reserve-service via Feign.
     * The local AgingRecord shadow table is no longer queried.
     * Skips silently if the client is unavailable.
     */
    @Override
    @Scheduled(cron = "0 0 0 * * *")   // midnight daily
    @Transactional
    public void generateAgingAlerts() {
        log.debug("Scheduler running: generateAgingAlerts");
        if (costReserveAgingClient == null) {
            log.debug("CostReserveAgingClient unavailable — skipping aging alert scan");
            return;
        }
        List<AgingRecordDTO> criticalAging;
        try {
            criticalAging = costReserveAgingClient.getAgingRecordsByBucket("BUCKET_90_PLUS");
        } catch (Exception e) {
            log.warn("Could not fetch aging records from cost-reserve-service: {}", e.getMessage());
            return;
        }

        // Per notification policy: aging 90+ → ANALYST + MANAGER + ADMIN.
        List<User> audience = loadActiveUsers(
                List.of(UserRole.ANALYST, UserRole.MANAGER, UserRole.ADMIN));

        for (AgingRecordDTO record : criticalAging) {
            boolean alreadyAlerted = notificationRepository.existsByReferenceIdAndCategoryAndStatus(
                    record.getClaimId(), NotificationCategory.AGING, NotificationStatus.UNREAD);
            if (alreadyAlerted) continue;

            String title   = "Critically Aged Claim — " + record.getClaimId();
            String message = String.format(
                    "Claim %s has been aging for %d days (bucket: %s). Immediate action required.",
                    record.getClaimId(), record.getAgingDays(), record.getAgingBucket());

            for (User user : audience) {
                saveSystemNotification(user.getUserId(), title, message,
                        NotificationCategory.AGING, record.getClaimId());
            }
            log.warn("AGING alert fired for claim={} days={}", record.getClaimId(), record.getAgingDays());
        }
    }

    /**
     * Loads every active user matching any of the given UserRoles.
     * Replaces the per-job inline JPQL so multi-role audiences can be
     * loaded in one query without each scheduled job re-implementing it.
     */
    private List<User> loadActiveUsers(List<UserRole> roles) {
        if (roles == null || roles.isEmpty()) return List.of();
        return entityManager
                .createQuery("SELECT u FROM User u WHERE u.role IN :roles AND u.isActive = true", User.class)
                .setParameter("roles", roles)
                .getResultList();
    }

    /** Reusable builder for system-generated notifications */
    private void saveSystemNotification(Long userId, String title, String message,
                                        NotificationCategory category, String referenceId) {
        Notification n = Notification.builder()
                .userId(userId)
                .title(title)
                .message(message)
                .category(category)
                .referenceId(referenceId)
                .status(NotificationStatus.UNREAD)
                .createdDate(LocalDateTime.now())
                .build();
        Notification saved = notificationRepository.save(n);
        streamService.broadcast(saved);   // SSE push
    }

    /** Find by ID or throw a meaningful exception */
    private Notification findOrThrow(Long id) {
        return notificationRepository.findById(id)
                .orElseThrow(() -> new NotificationNotFoundException("Notification not found with id: " + id));
    }

    /** Entity → DTO mapping */
    private NotificationResponseDTO toDTO(Notification n) {
        return NotificationResponseDTO.builder()
                .notificationId(n.getNotificationId())
                .userId(n.getUserId())
                .title(n.getTitle())
                .message(n.getMessage())
                .category(n.getCategory())
                .referenceId(n.getReferenceId())
                .status(n.getStatus())
                .createdDate(n.getCreatedDate())
                .readDate(n.getReadDate())
                .build();
    }
}
