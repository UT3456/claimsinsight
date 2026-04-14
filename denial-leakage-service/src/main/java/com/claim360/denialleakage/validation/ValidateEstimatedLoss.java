package com.claim360.denialleakage.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


@Constraint(validatedBy = EstimatedLossValidator.class)
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface ValidateEstimatedLoss {

    String message() default
            "{com.claim360.denialleakage.dto.LeakageFlagRequest.estimatedLoss.error}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}