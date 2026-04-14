package com.demo.dto;


import com.demo.enums.NotificationCategory;
import com.demo.enums.NotificationStatus;
import lombok.*;
import java.time.LocalDateTime;

/**
 * Returned by every read operation — GET, PATCH, etc.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationResponseDTO {

    private Long notificationId;
    private Long userId;
    private String title;
    private String message;
    private NotificationCategory category;
    private String referenceId;
    private NotificationStatus status;
    private LocalDateTime createdDate;
    private LocalDateTime readDate;
}
