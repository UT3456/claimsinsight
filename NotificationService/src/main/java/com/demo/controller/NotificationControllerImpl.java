package com.demo.controller;


import com.demo.dto.ApiResponse;
import com.demo.dto.NotificationDispatchRequestDTO;
import com.demo.dto.NotificationRequestDTO;
import com.demo.dto.NotificationResponseDTO;
import com.demo.dto.NotificationStatusUpdateDTO;
import com.demo.enums.NotificationCategory;
import com.demo.enums.NotificationStatus;
import com.demo.services.NotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
public class NotificationControllerImpl implements NotificationController {

    private final NotificationService notificationService;

    @Override
    public ResponseEntity<ApiResponse<NotificationResponseDTO>> createNotification(@Valid NotificationRequestDTO request) {
        NotificationResponseDTO response = notificationService.createNotification(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Notification created successfully", response));
    }

    @Override
    public ResponseEntity<ApiResponse<Long>> dispatchNotification(@Valid NotificationDispatchRequestDTO request) {
        long created = notificationService.dispatchNotification(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Notifications dispatched successfully", created));
    }

    @Override
    public ResponseEntity<ApiResponse<List<NotificationResponseDTO>>> getNotificationsForUser(Long userId) {
        List<NotificationResponseDTO> list = notificationService.getNotificationsForUser(userId);
        return ResponseEntity.ok(ApiResponse.success("Notifications fetched successfully", list));
    }

    @Override
    public ResponseEntity<ApiResponse<List<NotificationResponseDTO>>> getByStatus(Long userId, NotificationStatus status) {
        List<NotificationResponseDTO> list = notificationService.getByUserIdAndStatus(userId, status);
        return ResponseEntity.ok(ApiResponse.success("Notifications fetched by status", list));
    }

    @Override
    public ResponseEntity<ApiResponse<List<NotificationResponseDTO>>> getByCategory(Long userId, NotificationCategory category) {
        List<NotificationResponseDTO> list = notificationService.getByUserIdAndCategory(userId, category);
        return ResponseEntity.ok(ApiResponse.success("Notifications fetched by category", list));
    }

    @Override
    public ResponseEntity<ApiResponse<NotificationResponseDTO>> getById(Long notificationId) {
        NotificationResponseDTO response = notificationService.getById(notificationId);
        return ResponseEntity.ok(ApiResponse.success("Notification fetched successfully", response));
    }

    @Override
    public ResponseEntity<ApiResponse<NotificationResponseDTO>> updateStatus(Long notificationId, @Valid NotificationStatusUpdateDTO dto) {
        NotificationResponseDTO response = notificationService.updateStatus(notificationId, dto);
        return ResponseEntity.ok(ApiResponse.success("Notification status updated", response));
    }

    @Override
    public ResponseEntity<ApiResponse<String>> markAllAsRead(Long userId) {
        int count = notificationService.markAllAsRead(userId);
        return ResponseEntity.ok(ApiResponse.success("Marked " + count + " notifications as read", null));
    }

    @Override
    public ResponseEntity<ApiResponse<Long>> countUnread(Long userId) {
        long count = notificationService.countUnread(userId);
        return ResponseEntity.ok(ApiResponse.success("Unread count fetched", count));
    }

    @Override
    public ResponseEntity<ApiResponse<String>> deleteNotification(Long notificationId) {
        notificationService.deleteNotification(notificationId);
        return ResponseEntity.ok(ApiResponse.success("Notification deleted successfully", null));
    }
}