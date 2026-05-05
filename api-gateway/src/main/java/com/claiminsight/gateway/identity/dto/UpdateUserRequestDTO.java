package com.claiminsight.gateway.identity.dto;

import com.claiminsight.gateway.identity.model.Role;
import lombok.Data;

/**
 * Payload for PATCH /api/users/{id}.
 * All fields are optional — only the ones provided are updated.
 */
@Data
public class UpdateUserRequestDTO {
    /** New role to assign, or null to leave unchanged. */
    private Role role;

    /** Enable / disable the account, or null to leave unchanged. */
    private Boolean enabled;
}
