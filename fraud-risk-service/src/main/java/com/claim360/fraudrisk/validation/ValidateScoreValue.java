package com.claim360.fraudrisk.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Custom validation annotation for Score Value fields.
 * Ensures the value is between 0.0 and 100.0 inclusive.
 */
@Constraint(validatedBy = ScoreValueValidator.class)
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface ValidateScoreValue {

    String message() default
            "{com.claim360.fraudrisk.dto.RiskScoreRequest.scoreValue.error}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}