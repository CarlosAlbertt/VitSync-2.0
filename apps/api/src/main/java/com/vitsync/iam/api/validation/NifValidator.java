package com.vitsync.iam.api.validation;

import com.vitsync.iam.domain.Nif;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/** La regla vive en el dominio ({@link Nif}); esto solo la conecta con Bean Validation. */
public class NifValidator implements ConstraintValidator<ValidNif, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        return value == null || Nif.isValid(value);
    }
}
