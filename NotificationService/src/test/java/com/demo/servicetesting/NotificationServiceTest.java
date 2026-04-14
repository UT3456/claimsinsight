package com.demo.servicetesting;

import com.demo.dto.NotificationRequestDTO;
import com.demo.dto.NotificationResponseDTO;
import com.demo.dto.NotificationStatusUpdateDTO;
import com.demo.entities.*;
import com.demo.enums.NotificationCategory;
import com.demo.enums.NotificationStatus;
import com.demo.exception.NotificationNotFoundException;
import com.demo.repository.NotificationRepository;
import com.demo.services.NotificationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @InjectMocks
    private NotificationServiceImpl notificationService;

    private Notification sampleNotification;
    private NotificationRequestDTO sampleRequest;

    @BeforeEach
    void setUp() {
        sampleNotification = Notification.builder()
                .notificationId(1L)
                .userId(10L)
                .title("High Risk Claim Detected — CLM-2024-001")
                .message("Claim CLM-2024-001 has a fraud risk score of 75 (HIGH). Immediate review required.")
                .category(NotificationCategory.RISK)
                .referenceId("CLM-2024-001")
                .status(NotificationStatus.UNREAD)
                .createdDate(LocalDateTime.now())
                .build();

        sampleRequest = NotificationRequestDTO.builder()
                .userId(10L)
                .title("High Risk Claim Detected — CLM-2024-001")
                .message("Claim CLM-2024-001 has a fraud risk score of 75.")
                .category(NotificationCategory.RISK)
                .referenceId("CLM-2024-001")
                .build();
    }

    @Test
    void createNotification_Positive_ShouldSaveAndReturnDTO() {
        when(notificationRepository.save(any(Notification.class))).thenReturn(sampleNotification);

        NotificationResponseDTO result = notificationService.createNotification(sampleRequest);

        assertThat(result).isNotNull();
        assertThat(result.getNotificationId()).isEqualTo(1L);
        assertThat(result.getUserId()).isEqualTo(10L);
        assertThat(result.getCategory()).isEqualTo(NotificationCategory.RISK);
        assertThat(result.getStatus()).isEqualTo(NotificationStatus.UNREAD);
        verify(notificationRepository, times(1)).save(any(Notification.class));
    }

    @Test
    void createNotification_Negative_WhenRepositoryThrows_ShouldPropagateException() {
        when(notificationRepository.save(any(Notification.class)))
                .thenThrow(new RuntimeException("DB connection failed"));

        assertThatThrownBy(() -> notificationService.createNotification(sampleRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("DB connection failed");
    }

    @Test
    void getNotificationsForUser_Positive_ShouldReturnListForUser() {
        when(notificationRepository.findByUserIdOrderByCreatedDateDesc(10L))
                .thenReturn(List.of(sampleNotification));

        List<NotificationResponseDTO> result = notificationService.getNotificationsForUser(10L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getUserId()).isEqualTo(10L);
    }

    @Test
    void getNotificationsForUser_Negative_WhenNoNotifications_ShouldReturnEmptyList() {
        when(notificationRepository.findByUserIdOrderByCreatedDateDesc(99L))
                .thenReturn(List.of());

        List<NotificationResponseDTO> result = notificationService.getNotificationsForUser(99L);

        assertThat(result).isEmpty();
    }

    @Test
    void getByUserIdAndStatus_Positive_ShouldReturnUnreadNotifications() {
        when(notificationRepository.findByUserIdAndStatusOrderByCreatedDateDesc(10L, NotificationStatus.UNREAD))
                .thenReturn(List.of(sampleNotification));

        List<NotificationResponseDTO> result = notificationService
                .getByUserIdAndStatus(10L, NotificationStatus.UNREAD);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStatus()).isEqualTo(NotificationStatus.UNREAD);
    }

    @Test
    void getByUserIdAndStatus_Negative_WhenNoMatchingStatus_ShouldReturnEmptyList() {
        when(notificationRepository.findByUserIdAndStatusOrderByCreatedDateDesc(10L, NotificationStatus.ACTIONED))
                .thenReturn(List.of());

        List<NotificationResponseDTO> result = notificationService
                .getByUserIdAndStatus(10L, NotificationStatus.ACTIONED);

        assertThat(result).isEmpty();
    }

    @Test
    void getByUserIdAndCategory_Positive_ShouldReturnRiskNotifications() {
        when(notificationRepository.findByUserIdAndCategoryOrderByCreatedDateDesc(10L, NotificationCategory.RISK))
                .thenReturn(List.of(sampleNotification));

        List<NotificationResponseDTO> result = notificationService
                .getByUserIdAndCategory(10L, NotificationCategory.RISK);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCategory()).isEqualTo(NotificationCategory.RISK);
    }

    @Test
    void getByUserIdAndCategory_Negative_WhenCategoryHasNoData_ShouldReturnEmptyList() {
        when(notificationRepository.findByUserIdAndCategoryOrderByCreatedDateDesc(10L, NotificationCategory.SYSTEM))
                .thenReturn(List.of());

        List<NotificationResponseDTO> result = notificationService
                .getByUserIdAndCategory(10L, NotificationCategory.SYSTEM);

        assertThat(result).isEmpty();
    }

    @Test
    void getById_Positive_ShouldReturnNotificationDTO() {
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(sampleNotification));

        NotificationResponseDTO result = notificationService.getById(1L);

        assertThat(result.getNotificationId()).isEqualTo(1L);
        assertThat(result.getTitle()).isEqualTo("High Risk Claim Detected — CLM-2024-001");
    }

    @Test
    void getById_Negative_WhenIdDoesNotExist_ShouldThrowNotificationNotFoundException() {
        when(notificationRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> notificationService.getById(999L))
                .isInstanceOf(NotificationNotFoundException.class)
                .hasMessageContaining("999");
    }

    @Test
    void updateStatus_Positive_WhenMovingToRead_ShouldSetReadDate() {
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(sampleNotification));
        when(notificationRepository.save(any())).thenReturn(sampleNotification);

        NotificationStatusUpdateDTO dto = new NotificationStatusUpdateDTO(NotificationStatus.READ);
        NotificationResponseDTO result = notificationService.updateStatus(1L, dto);

        assertThat(result).isNotNull();
        verify(notificationRepository).save(any(Notification.class));
    }

    @Test
    void updateStatus_Negative_WhenNotificationNotFound_ShouldThrowException() {
        when(notificationRepository.findById(999L)).thenReturn(Optional.empty());

        NotificationStatusUpdateDTO dto = new NotificationStatusUpdateDTO(NotificationStatus.READ);

        assertThatThrownBy(() -> notificationService.updateStatus(999L, dto))
                .isInstanceOf(NotificationNotFoundException.class)
                .hasMessageContaining("999");
    }

    @Test
    void markAllAsRead_Positive_ShouldReturnCountOfMarkedNotifications() {
        when(notificationRepository.markAllAsRead(eq(10L), any(LocalDateTime.class))).thenReturn(3);

        int count = notificationService.markAllAsRead(10L);

        assertThat(count).isEqualTo(3);
        verify(notificationRepository).markAllAsRead(eq(10L), any(LocalDateTime.class));
    }

    @Test
    void markAllAsRead_Negative_WhenUserHasNoUnread_ShouldReturnZero() {
        when(notificationRepository.markAllAsRead(eq(10L), any(LocalDateTime.class))).thenReturn(0);

        int count = notificationService.markAllAsRead(10L);

        assertThat(count).isEqualTo(0);
    }

    @Test
    void countUnread_Positive_ShouldReturnCorrectUnreadCount() {
        when(notificationRepository.countByUserIdAndStatus(10L, NotificationStatus.UNREAD)).thenReturn(5L);

        long count = notificationService.countUnread(10L);

        assertThat(count).isEqualTo(5L);
    }

    @Test
    void countUnread_Negative_WhenUserHasNoUnread_ShouldReturnZero() {
        when(notificationRepository.countByUserIdAndStatus(10L, NotificationStatus.UNREAD)).thenReturn(0L);

        long count = notificationService.countUnread(10L);

        assertThat(count).isEqualTo(0L);
    }

    @Test
    void deleteNotification_Positive_ShouldCallRepositoryDelete() {
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(sampleNotification));

        notificationService.deleteNotification(1L);

        verify(notificationRepository, times(1)).delete(sampleNotification);
    }

    @Test
    void deleteNotification_Negative_WhenIdNotFound_ShouldThrowNotificationNotFoundException() {
        when(notificationRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> notificationService.deleteNotification(999L))
                .isInstanceOf(NotificationNotFoundException.class)
                .hasMessageContaining("999");

        verify(notificationRepository, never()).delete(any());
    }
}
