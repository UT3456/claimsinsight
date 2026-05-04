package com.demo.services;

import com.demo.dto.NotificationRequestDTO;
import com.demo.dto.NotificationResponseDTO;
import com.demo.dto.NotificationStatusUpdateDTO;
import com.demo.dto.NotificationDispatchRequestDTO;
import com.demo.enums.NotificationCategory;
import com.demo.enums.NotificationStatus;

import java.util.List;

/**
 * Contract for all notification operations.
 */
public interface NotificationService {

    NotificationResponseDTO createNotification(NotificationRequestDTO request);

    long dispatchNotification(NotificationDispatchRequestDTO request);

    List<NotificationResponseDTO> getNotificationsForUser(Long userId);

    List<NotificationResponseDTO> getByUserIdAndStatus(Long userId, NotificationStatus status);

    List<NotificationResponseDTO> getByUserIdAndCategory(Long userId, NotificationCategory category);

    NotificationResponseDTO getById(Long notificationId);

    NotificationResponseDTO updateStatus(Long notificationId, NotificationStatusUpdateDTO dto);

    int markAllAsRead(Long userId);

    long countUnread(Long userId);

    void deleteNotification(Long notificationId);

    void generateRiskAlerts();

    void generatePerformanceAlerts();

    void generateAgingAlerts();
}
