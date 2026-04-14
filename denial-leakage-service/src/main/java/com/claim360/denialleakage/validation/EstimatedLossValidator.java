package com.claim360.denialleakage.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class EstimatedLossValidator implements
        ConstraintValidator<ValidateEstimatedLoss, Double> {

    @Override
    public boolean isValid(Double value,
                           ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        return value > 0.0;
    }
}