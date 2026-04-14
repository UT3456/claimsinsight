package com.claim360.denialleakage.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Constraint(validatedBy = DenialCodeValidator.class)
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface ValidateDenialCode {

    String message() default
            "{com.claim360.denialleakage.dto.DenialPatternRequest.denialCode.error}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}