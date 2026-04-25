package com.demo.dto;

import com.demo.enums.NotificationCategory;
import com.demo.enums.UserRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Set;

/**
 * Used for: POST /api/notifications/dispatch
 * Fan-out notification request to one or more users by role and/or user IDs.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationDispatchRequestDTO {

    private Set<Long> targetUserIds;

    private Set<UserRole> targetRoles;

    @NotBlank(message = "Notification title is required")
    @Size(max = 200, message = "Notification title must not exceed 200 characters")
    private String title;

    @NotBlank(message = "Notification message is required")
    private String message;

    @NotNull(message = "Notification category is required")
    private NotificationCategory category;

    @Size(max = 50, message = "Reference ID must not exceed 50 characters")
    private String referenceId;

    public boolean hasTargets() {
        return (targetUserIds != null && !targetUserIds.isEmpty())
                || (targetRoles != null && !targetRoles.isEmpty());
    }
}