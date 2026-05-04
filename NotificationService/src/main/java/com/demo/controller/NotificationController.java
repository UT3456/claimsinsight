package com.demo.controller;

import com.demo.dto.ApiResponse;
import com.demo.dto.NotificationDispatchRequestDTO;
import com.demo.dto.NotificationRequestDTO;
import com.demo.dto.NotificationResponseDTO;
import com.demo.dto.NotificationStatusUpdateDTO;
import com.demo.enums.NotificationCategory;
import com.demo.enums.NotificationStatus;
//import io.swagger.v3.oas.annotations.Operation;
//import io.swagger.v3.oas.annotations.Parameter;
//import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST API contract for Module 9 — Notifications &amp; Alerts
 */
@RequestMapping("/api/notifications")
public interface NotificationController {

    @PostMapping
    ResponseEntity<ApiResponse<NotificationResponseDTO>> createNotification(
            @Valid @RequestBody NotificationRequestDTO request);

    @PostMapping("/dispatch")
    ResponseEntity<ApiResponse<Long>> dispatchNotification(
            @Valid @RequestBody NotificationDispatchRequestDTO request);

    @GetMapping("/user/{userId}")
    ResponseEntity<ApiResponse<List<NotificationResponseDTO>>> getNotificationsForUser(
             @PathVariable Long userId);

    @GetMapping("/user/{userId}/status/{status}")
    ResponseEntity<ApiResponse<List<NotificationResponseDTO>>> getByStatus(
            @PathVariable Long userId,
            @PathVariable NotificationStatus status);

    @GetMapping("/user/{userId}/category/{category}")
    ResponseEntity<ApiResponse<List<NotificationResponseDTO>>> getByCategory(
            @PathVariable Long userId,
            @PathVariable NotificationCategory category);

    @GetMapping("/{notificationId}")
    ResponseEntity<ApiResponse<NotificationResponseDTO>> getById(
            @PathVariable Long notificationId);

    @PatchMapping("/{notificationId}/status")
    ResponseEntity<ApiResponse<NotificationResponseDTO>> updateStatus(
            @PathVariable Long notificationId,
            @Valid @RequestBody NotificationStatusUpdateDTO dto);

    @PatchMapping("/user/{userId}/mark-all-read")
    ResponseEntity<ApiResponse<String>> markAllAsRead(
            @PathVariable Long userId);

    @GetMapping("/unread-count/{userId}")
    ResponseEntity<ApiResponse<Long>> countUnread(
            @PathVariable Long userId);

    @DeleteMapping("/{notificationId}")
    ResponseEntity<ApiResponse<String>> deleteNotification(
            @PathVariable Long notificationId);
}
