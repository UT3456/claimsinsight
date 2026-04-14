package com.claim360.fraudrisk.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Validator logic for @ValidateClaimId annotation.
 * Validates that the Claim ID follows the pattern: CLM-[digits]
 * Examples:
 *   VALID:   CLM-001, CLM-123, CLM-9999
 *   INVALID: CLM001, C-001, claim-001, null, ""
 */
public class ClaimIdValidator implements
        ConstraintValidator<ValidateClaimId, String> {

    // Pattern: must start with CLM- followed by one or more digits
    private static final String CLAIM_ID_PATTERN = "^CLM-\\d+$";

    @Override
    public boolean isValid(String value,
                           ConstraintValidatorContext context) {
        // Allow @NotBlank to handle null/blank separately
        if (value == null || value.isBlank()) {
            return true;
        }
        return value.matches(CLAIM_ID_PATTERN);
    }
}