package com.demo.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.Arrays;
import java.util.List;

public class ViolationTypeValidator implements ConstraintValidator<ValidateViolationType, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        // Define your specific allowed violation types here
        List<String> allowedTypes = Arrays.asList(
                "UNDERWRITING_DELAY",
                "CLAIM_ASSESSMENT",
                "DOCUMENT_MISSING",
                "FINAL_SETTLEMENT"
        );

        if (value == null) return false;

        return allowedTypes.contains(value.toUpperCase());
    }
}