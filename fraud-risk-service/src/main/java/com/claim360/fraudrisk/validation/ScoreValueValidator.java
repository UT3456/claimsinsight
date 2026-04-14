package com.claim360.fraudrisk.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Validator logic for @ValidateScoreValue annotation.
 * Validates that the Score Value is between 0.0 and 100.0 inclusive.
 * Examples:
 *   VALID:   0.0, 50.0, 75.5, 100.0
 *   INVALID: -1.0, 100.1, 150.0, null
 */
public class ScoreValueValidator implements
        ConstraintValidator<ValidateScoreValue, Double> {

    private static final double MIN_SCORE = 0.0;
    private static final double MAX_SCORE = 100.0;

    @Override
    public boolean isValid(Double value,
                           ConstraintValidatorContext context) {
        // Allow @NotNull to handle null separately
        if (value == null) {
            return true;
        }
        return value >= MIN_SCORE && value <= MAX_SCORE;
    }
}