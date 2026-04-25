package com.claiminsight.metrics.client.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

/**
 * Mirrors NotificationService dispatch endpoint payload.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationDispatchRequestDTO {
    private Set<Long> targetUserIds;
    private Set<String> targetRoles;
    private String title;
    private String message;
    private String category;
    private String referenceId;
}
