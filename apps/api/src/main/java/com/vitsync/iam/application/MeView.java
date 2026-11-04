package com.vitsync.iam.application;

import com.vitsync.iam.domain.User;
import java.util.UUID;

/**
 * Lo que el usuario autenticado ve de sí mismo. Lo devuelve la capa application para que la
 * entidad no salga del módulo ni llegue al controlador (decisión D5).
 *
 * <p>Sin NIF: es un dato que solo hace falta en el alta, y no viaja si no hace falta.
 */
public record MeView(UUID id, String email, String role, String firstName, String lastName,
                     String phone, String locale, boolean emailVerified) {

    static MeView of(User u) {
        return new MeView(u.getPublicId(), u.getEmail(), u.getRole().name(), u.getFirstName(),
                u.getLastName(), u.getPhone(), u.getLocale(), u.isEmailVerified());
    }
}
