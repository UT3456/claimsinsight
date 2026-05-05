package com.claiminsight.gateway.identity.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** Payload for POST /api/auth/refresh. */
@Data
public class RefreshRequestDTO {

    @NotBlank(message = "Refresh token is required")
    private String refreshToken;
}
