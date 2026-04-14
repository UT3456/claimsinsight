package com.claim360.fraudrisk.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Custom validation annotation for Claim ID fields.
 * Ensures the value follows the pattern: CLM-[digits]
 * Example valid values: CLM-001, CLM-123, CLM-9999
 */
@Constraint(validatedBy = ClaimIdValidator.class)
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface ValidateClaimId {

    String message() default
            "{com.claim360.fraudrisk.dto.RiskIndicatorRequest.claimId.error}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}