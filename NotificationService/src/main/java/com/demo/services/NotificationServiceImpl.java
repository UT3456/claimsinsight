package com.demo.services;

import com.demo.dto.NotificationRequestDTO;
import com.demo.dto.NotificationResponseDTO;
import com.demo.dto.NotificationStatusUpdateDTO;
import com.demo.dto.NotificationDispatchRequestDTO;
import com.demo.dto.UserSyncRequestDTO;
import com.demo.entities.*;
import com.demo.enums.NotificationCategory;
import com.demo.enums.NotificationStatus;
import com.demo.enums.UserRole;
import com.demo.exception.NotificationNotFoundException;
import com.demo.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;

    @PersistenceContext
    private EntityManager entityManager;

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

                long created = 0;
                LocalDateTime now = LocalDateTime.now();
                for (Long userId : recipientIds) {
                        Notification n = Notification.builder()
                                        .userId(userId)
                                        .title(request.getTitle())
                                        .message(request.getMessage())
                                        .category(request.getCategory())
                                        .referenceId(request.getReferenceId())
                                        .status(NotificationStatus.UNREAD)
                                        .createdDate(now)
                                        .build();
                        notificationRepository.save(n);
                        created++;
                }

                return created;
        }

    /**
     * Upsert by explicit userId. We bypass JPA's IDENTITY generation with a native
     * {@code INSERT ... ON DUPLICATE KEY UPDATE} so the caller's userId is honored
     * verbatim — the api-gateway owns identity and this table mirrors it.
     *
     * <p>MySQL-specific SQL. H2 (test profile) supports {@code MERGE INTO}; because
     * this path is only exercised against the real users table in prod-like runs,
     * we keep a single MySQL statement here and rely on H2 being driven through
     * repository seeding in tests instead.
     */
    @Override
    @Transactional
    public void syncUser(UserSyncRequestDTO request) {
        if (request == null || request.getUserId() == null) return;
        boolean active = request.getIsActive() == null || request.getIsActive();
        entityManager.createNativeQuery(
                "INSERT INTO users (user_id, name, email, role, is_active) " +
                "VALUES (:userId, :name, :email, :role, :active) " +
                "ON DUPLICATE KEY UPDATE name = VALUES(name), email = VALUES(email), " +
                "role = VALUES(role), is_active = VALUES(is_active)")
                .setParameter("userId", request.getUserId())
                .setParameter("name", request.getName())
                .setParameter("email", request.getEmail())
                .setParameter("role", request.getRole())
                .setParameter("active", active)
                .executeUpdate();
        log.info("User sync upsert: userId={}, role={}", request.getUserId(), request.getRole());
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
     * **/
    @Override
    @Scheduled(fixedDelay = 300_000)   // every 5 minutes
    @Transactional
    public void generateRiskAlerts() {
        log.debug("Scheduler running: generateRiskAlerts");

        List<RiskScore> highRiskScores = entityManager
                .createQuery("SELECT r FROM RiskScore r WHERE r.scoreValue >= :threshold", RiskScore.class)
                .setParameter("threshold", RISK_SCORE_THRESHOLD)
                .getResultList();

        List<User> fraudUsers = entityManager
                .createQuery("SELECT u FROM User u WHERE u.role = :role AND u.isActive = true", User.class)
                .setParameter("role", UserRole.FRAUD)
                .getResultList();

        for (RiskScore score : highRiskScores) {
            String title   = "High Risk Claim Detected — " + score.getClaimId();
            String message = String.format(
                    "Claim %s has a fraud risk score of %d (%s). Immediate review required.",
                    score.getClaimId(), score.getScoreValue(), score.getRiskLevel());

            // Per-user dedup: late-joiners still get their own copy, and we
            // never double-fire for a user who already has an UNREAD for this claim.
            boolean fired = false;
            for (User user : fraudUsers) {
                boolean alreadyAlerted = notificationRepository
                        .existsByUserIdAndReferenceIdAndCategoryAndStatus(
                                user.getUserId(), score.getClaimId(),
                                NotificationCategory.RISK, NotificationStatus.UNREAD);
                if (alreadyAlerted) continue;
                saveSystemNotification(user.getUserId(), title, message,
                        NotificationCategory.RISK, score.getClaimId());
                fired = true;
            }
            if (fired) {
                log.warn("RISK alert fired for claim={} score={}", score.getClaimId(), score.getScoreValue());
            }
        }
    }

    /**
     * PERFORMANCE ALERT — runs every 10 minutes.
     */
    @Override
    @Scheduled(fixedDelay = 600_000)   // every 10 minutes
    @Transactional
    public void generatePerformanceAlerts() {
        log.debug("Scheduler running: generatePerformanceAlerts");

        LocalDate startOfMonth = LocalDate.now().withDayOfMonth(1);

        // Get adjuster IDs with violations this month
        List<Long> adjusterIds = entityManager
                .createQuery("SELECT DISTINCT s.adjusterId FROM SlaViolation s WHERE s.violationDate >= :start", Long.class)
                .setParameter("start", startOfMonth)
                .getResultList();

        List<User> managers = entityManager
                .createQuery("SELECT u FROM User u WHERE u.role = :role AND u.isActive = true", User.class)
                .setParameter("role", UserRole.MANAGER)
                .getResultList();

        for (Long adjusterId : adjusterIds) {
            long violationCount = (long) entityManager
                    .createQuery("SELECT COUNT(s) FROM SlaViolation s WHERE s.adjusterId = :id AND s.violationDate >= :start")
                    .setParameter("id", adjusterId)
                    .setParameter("start", startOfMonth)
                    .getSingleResult();

            if (violationCount < SLA_VIOLATION_THRESHOLD) continue;

            String referenceId = "ADJ-" + adjusterId;
            String title   = "SLA Breach Threshold Exceeded — Adjuster #" + adjusterId;
            String message = String.format(
                    "Adjuster %d has breached SLA on %d claims this month. Threshold is %d. Please review workload.",
                    adjusterId, violationCount, SLA_VIOLATION_THRESHOLD);

            boolean fired = false;
            for (User manager : managers) {
                boolean alreadyAlerted = notificationRepository
                        .existsByUserIdAndReferenceIdAndCategoryAndStatus(
                                manager.getUserId(), referenceId,
                                NotificationCategory.PERFORMANCE, NotificationStatus.UNREAD);
                if (alreadyAlerted) continue;
                saveSystemNotification(manager.getUserId(), title, message,
                        NotificationCategory.PERFORMANCE, referenceId);
                fired = true;
            }
            if (fired) {
                log.warn("PERFORMANCE alert fired for adjusterId={} violations={}", adjusterId, violationCount);
            }
        }
    }

    /**
     * AGING ALERT — runs daily at midnight.
     */
    @Override
    @Scheduled(cron = "0 0 0 * * *")   // midnight daily
    @Transactional
    public void generateAgingAlerts() {
        log.debug("Scheduler running: generateAgingAlerts");

        List<AgingRecord> criticalAging = entityManager
                .createQuery("SELECT a FROM AgingRecord a WHERE a.agingBucket IN :buckets AND a.status IN :statuses", AgingRecord.class)
                .setParameter("buckets", CRITICAL_AGING_BUCKETS)
                .setParameter("statuses", STUCK_STATUSES)
                .getResultList();

        List<User> analysts = entityManager
                .createQuery("SELECT u FROM User u WHERE u.role = :role AND u.isActive = true", User.class)
                .setParameter("role", UserRole.ANALYST)
                .getResultList();

        for (AgingRecord record : criticalAging) {
            String title   = "Critically Aged Claim — " + record.getClaimId();
            String message = String.format(
                    "Claim %s has been %s for %d days (bucket: %s). Status is still %s. Immediate action required.",
                    record.getClaimId(), record.getStatus(), record.getAgingDays(),
                    record.getAgingBucket(), record.getStatus());

            boolean fired = false;
            for (User analyst : analysts) {
                boolean alreadyAlerted = notificationRepository
                        .existsByUserIdAndReferenceIdAndCategoryAndStatus(
                                analyst.getUserId(), record.getClaimId(),
                                NotificationCategory.AGING, NotificationStatus.UNREAD);
                if (alreadyAlerted) continue;
                saveSystemNotification(analyst.getUserId(), title, message,
                        NotificationCategory.AGING, record.getClaimId());
                fired = true;
            }
            if (fired) {
                log.warn("AGING alert fired for claim={} days={}", record.getClaimId(), record.getAgingDays());
            }
        }
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
        notificationRepository.save(n);
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
