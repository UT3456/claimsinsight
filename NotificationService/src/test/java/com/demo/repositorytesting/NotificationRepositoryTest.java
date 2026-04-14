package com.demo.repositorytesting;


import com.demo.entities.*;
import com.demo.enums.NotificationCategory;
import com.demo.enums.NotificationStatus;
import com.demo.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Uses @DataJpaTest which spins up an in-memory H2 database.
 */
@DataJpaTest
@ActiveProfiles("test")
class NotificationRepositoryTest {

    @Autowired
    private NotificationRepository notificationRepository;

    private Notification riskNotification;
    private Notification agingNotification;
    private Notification readNotification;

    @BeforeEach
    void setUp() {
        notificationRepository.deleteAll();

        riskNotification = notificationRepository.save(Notification.builder()
                .userId(10L)
                .title("High Risk Claim — CLM-001")
                .message("Fraud score 75 detected for CLM-001")
                .category(NotificationCategory.RISK)
                .referenceId("CLM-001")
                .status(NotificationStatus.UNREAD)
                .createdDate(LocalDateTime.now())
                .build());

        agingNotification = notificationRepository.save(Notification.builder()
                .userId(10L)
                .title("Critically Aged Claim — CLM-002")
                .message("CLM-002 has been OPEN for 95 days")
                .category(NotificationCategory.AGING)
                .referenceId("CLM-002")
                .status(NotificationStatus.UNREAD)
                .createdDate(LocalDateTime.now())
                .build());

        readNotification = notificationRepository.save(Notification.builder()
                .userId(10L)
                .title("SLA Breach — ADJ-5")
                .message("Adjuster 5 breached SLA 3 times this month")
                .category(NotificationCategory.PERFORMANCE)
                .referenceId("ADJ-5")
                .status(NotificationStatus.READ)
                .createdDate(LocalDateTime.now())
                .readDate(LocalDateTime.now())
                .build());
    }

    @Test
    void findByUserId_Positive_ShouldReturnAllThreeNotifications() {
        List<Notification> result = notificationRepository.findByUserIdOrderByCreatedDateDesc(10L);
        assertThat(result).hasSize(3);
    }

    @Test
    void findByUserId_Negative_WhenUserHasNone_ShouldReturnEmptyList() {
        List<Notification> result = notificationRepository.findByUserIdOrderByCreatedDateDesc(999L);
        assertThat(result).isEmpty();
    }

    @Test
    void findByUserIdAndStatus_Positive_ShouldReturnTwoUnreadNotifications() {
        List<Notification> result = notificationRepository
                .findByUserIdAndStatusOrderByCreatedDateDesc(10L, NotificationStatus.UNREAD);
        assertThat(result).hasSize(2);
        assertThat(result).allMatch(n -> n.getStatus() == NotificationStatus.UNREAD);
    }

    @Test
    void findByUserIdAndStatus_Negative_WhenNoMatchingStatus_ShouldReturnEmpty() {
        List<Notification> result = notificationRepository
                .findByUserIdAndStatusOrderByCreatedDateDesc(10L, NotificationStatus.DISMISSED);
        assertThat(result).isEmpty();
    }

    @Test
    void findByUserIdAndCategory_Positive_ShouldReturnRiskNotification() {
        List<Notification> result = notificationRepository
                .findByUserIdAndCategoryOrderByCreatedDateDesc(10L, NotificationCategory.RISK);
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getReferenceId()).isEqualTo("CLM-001");
    }

    @Test
    void findByUserIdAndCategory_Negative_WhenCategoryNotPresent_ShouldReturnEmpty() {
        List<Notification> result = notificationRepository
                .findByUserIdAndCategoryOrderByCreatedDateDesc(10L, NotificationCategory.DENIAL);
        assertThat(result).isEmpty();
    }

    @Test
    void countByUserIdAndStatus_Positive_ShouldReturnTwoUnread() {
        long count = notificationRepository.countByUserIdAndStatus(10L, NotificationStatus.UNREAD);
        assertThat(count).isEqualTo(2L);
    }

    @Test
    void countByUserIdAndStatus_Negative_WhenNoActioned_ShouldReturnZero() {
        long count = notificationRepository.countByUserIdAndStatus(10L, NotificationStatus.ACTIONED);
        assertThat(count).isEqualTo(0L);
    }

    @Test
    void markAllAsRead_Positive_ShouldMarkTwoNotificationsAsRead() {
        int updated = notificationRepository.markAllAsRead(10L, LocalDateTime.now());
        assertThat(updated).isEqualTo(2);   // riskNotification + agingNotification were UNREAD

        long remaining = notificationRepository.countByUserIdAndStatus(10L, NotificationStatus.UNREAD);
        assertThat(remaining).isEqualTo(0L);
    }

    @Test
    void markAllAsRead_Negative_WhenUserHasNoUnread_ShouldReturnZero() {
        // Mark all first
        notificationRepository.markAllAsRead(10L, LocalDateTime.now());

        // Call again — nothing left to mark
        int updated = notificationRepository.markAllAsRead(10L, LocalDateTime.now());
        assertThat(updated).isEqualTo(0);
    }

    @Test
    void existsByReferenceIdAndCategoryAndStatus_Positive_WhenDuplicateExists_ShouldReturnTrue() {
        // CLM-001 already has an UNREAD RISK notification from setUp
        boolean exists = notificationRepository.existsByReferenceIdAndCategoryAndStatus(
                "CLM-001", NotificationCategory.RISK, NotificationStatus.UNREAD);
        assertThat(exists).isTrue();
    }

    @Test
    void existsByReferenceIdAndCategoryAndStatus_Negative_WhenNoDuplicate_ShouldReturnFalse() {
        // CLM-999 has no notification at all
        boolean exists = notificationRepository.existsByReferenceIdAndCategoryAndStatus(
                "CLM-999", NotificationCategory.RISK, NotificationStatus.UNREAD);
        assertThat(exists).isFalse();
    }

    @Test
    void existsByReferenceIdAndCategoryAndStatus_Negative_WhenSameClaimButDifferentCategory_ShouldReturnFalse() {
        // CLM-001 exists as RISK, but not as DENIAL
        boolean exists = notificationRepository.existsByReferenceIdAndCategoryAndStatus(
                "CLM-001", NotificationCategory.DENIAL, NotificationStatus.UNREAD);
        assertThat(exists).isFalse();
    }

    @Test
    void existsByReferenceIdAndCategoryAndStatus_Negative_WhenSameClaimButAlreadyRead_ShouldReturnFalse() {
        // ADJ-5 exists as PERFORMANCE but status is READ, not UNREAD
        boolean exists = notificationRepository.existsByReferenceIdAndCategoryAndStatus(
                "ADJ-5", NotificationCategory.PERFORMANCE, NotificationStatus.UNREAD);
        assertThat(exists).isFalse();
    }
}
