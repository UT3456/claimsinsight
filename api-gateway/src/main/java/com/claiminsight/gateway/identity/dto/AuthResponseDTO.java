package com.claiminsight.gateway.identity.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Response returned after successful authentication or registration. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponseDTO {

    private String accessToken;
    /** Opaque token used to obtain a new access token without re-login. */
    private String refreshToken;
    private String tokenType;
    private long   expiresIn;
    private String username;
    private String name;
    private String role;
    private String phone;
}
