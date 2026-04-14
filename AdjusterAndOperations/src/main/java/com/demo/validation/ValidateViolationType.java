package com.demo.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Constraint(validatedBy = ViolationTypeValidator.class)
@Retention(RUNTIME)
@Target(FIELD)
public @interface ValidateViolationType {
    String message() default "Invalid Violation Type provided";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}