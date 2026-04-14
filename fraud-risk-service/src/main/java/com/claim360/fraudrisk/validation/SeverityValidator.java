package com.claim360.fraudrisk.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Arrays;
import java.util.List;

/**
 * Validator logic for @ValidateSeverity annotation.
 * Validates that the Severity is one of the allowed values.
 * Examples:
 *   VALID:   LOW, MEDIUM, HIGH
 *   INVALID: low, medium, CRITICAL, null, ""
 */
public class SeverityValidator implements
        ConstraintValidator<ValidateSeverity, String> {

    // Allowed severity levels — case sensitive
    private static final List<String> ALLOWED_SEVERITIES =
            Arrays.asList("LOW", "MEDIUM", "HIGH");

    @Override
    public boolean isValid(String value,
                           ConstraintValidatorContext context) {
        // Allow @NotBlank to handle null/blank separately
        if (value == null || value.isBlank()) {
            return true;
        }
        return ALLOWED_SEVERITIES.contains(value);
    }
}