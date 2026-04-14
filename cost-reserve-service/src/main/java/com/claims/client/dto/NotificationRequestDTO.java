package com.claims.client.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Mirrors NotificationService's NotificationRequestDTO for Feign client use. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationRequestDTO {
    private Long userId;
    private String title;
    private String message;
    private String category;   // RISK, DENIAL, COST, PERFORMANCE, AGING, SYSTEM
    private String referenceId;
}
