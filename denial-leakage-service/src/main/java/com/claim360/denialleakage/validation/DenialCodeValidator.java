package com.claim360.denialleakage.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class DenialCodeValidator implements
        ConstraintValidator<ValidateDenialCode, String> {

    // Pattern: 2 uppercase letters + hyphen + one or more digits
    private static final String DENIAL_CODE_PATTERN = "^[A-Z]{2}-\\d+$";

    @Override
    public boolean isValid(String value,
                           ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return true;
        }
        return value.matches(DENIAL_CODE_PATTERN);
    }
}