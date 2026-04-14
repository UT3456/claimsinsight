package com.claim360.denialleakage.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class ClaimIdValidator implements
        ConstraintValidator<ValidateClaimId, String> {

    private static final String CLAIM_ID_PATTERN = "^CLM-\\d+$";

    @Override
    public boolean isValid(String value,
                           ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return true;
        }
        return value.matches(CLAIM_ID_PATTERN);
    }
}