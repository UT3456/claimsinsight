package com.demo.dto;

import com.demo.enums.NotificationStatus;
import jakarta.validation.constraints.NotNull;
import lombok.*;

/**
 * Used for: PATCH /api/notifications/{id}/status
 * Allows the UI to move a notification through its lifecycle:
 * UNREAD → READ / DISMISSED / ACTIONED
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationStatusUpdateDTO {

    @NotNull(message = "{notification.status.required}")
    private NotificationStatus status;
}