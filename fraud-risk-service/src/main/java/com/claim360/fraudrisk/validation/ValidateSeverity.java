package com.claim360.fraudrisk.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Custom validation annotation for Severity fields.
 * Ensures the value is one of: LOW, MEDIUM, HIGH
 */
@Constraint(validatedBy = SeverityValidator.class)
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface ValidateSeverity {

    String message() default
            "{com.claim360.fraudrisk.dto.RiskIndicatorRequest.severity.error}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}