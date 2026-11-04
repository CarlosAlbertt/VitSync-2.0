package com.vitsync.iam.api.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** La contraseña cumple {@code PasswordPolicy}: 12+ caracteres y 72 bytes como máximo. */
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = PasswordValidator.class)
public @interface ValidPassword {

    String message() default "must be at least 12 characters and at most 72 bytes";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
