package com.demo.dto;


import com.demo.enums.NotificationCategory;
import jakarta.validation.constraints.*;
import lombok.*;

/**
 * Used for: POST /api/notifications
 * Validation error messages come from src/main/resources/error.properties
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationRequestDTO {

    @NotNull(message = "{notification.userId.required}")
    @Positive(message = "{notification.userId.positive}")
    private Long userId;

    @NotBlank(message = "{notification.title.required}")
    @Size(max = 200, message = "{notification.title.size}")
    private String title;

    @NotBlank(message = "{notification.message.required}")
    private String message;

    @NotNull(message = "{notification.category.required}")
    private NotificationCategory category;

    @Size(max = 50, message = "{notification.referenceId.size}")
    private String referenceId;   // e.g. "CLM-2024-00123"
}