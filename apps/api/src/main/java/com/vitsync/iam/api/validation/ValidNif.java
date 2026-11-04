package com.vitsync.iam.api.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * El campo es un NIF (DNI) o NIE con la letra de control correcta.
 *
 * <p>Una anotación de Bean Validation propia: así el error sale en la lista {@code errors}
 * del 400 junto a los demás campos, igual que un {@code @NotBlank}. {@code null} se da por
 * válido (convención de Bean Validation): la obligatoriedad la pone {@code @NotBlank}.
 */
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = NifValidator.class)
public @interface ValidNif {

    String message() default "must be a valid NIF or NIE";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
