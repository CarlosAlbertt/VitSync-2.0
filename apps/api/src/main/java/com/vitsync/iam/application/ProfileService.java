package com.vitsync.iam.application;

import com.vitsync.common.error.BusinessException;
import com.vitsync.common.error.ErrorCode;
import com.vitsync.iam.domain.User;
import com.vitsync.iam.infrastructure.UserRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Casos de uso de {@code /api/v1/me}: el usuario autenticado consulta y edita sus datos.
 *
 * <p>El usuario sale siempre del token ({@code sub}), nunca de un parámetro: así no hay forma
 * de pedir los datos de otro (el IDOR sistémico V03 de v1).
 */
@Service
public class ProfileService {

    private final UserRepository users;

    public ProfileService(UserRepository users) {
        this.users = users;
    }

    @Transactional(readOnly = true)
    public MeView me(UUID publicId) {
        return MeView.of(load(publicId));
    }

    @Transactional
    public MeView update(UUID publicId, String firstName, String lastName, String phone, String locale) {
        User user = load(publicId);
        user.updateProfile(firstName, lastName, phone, locale);
        return MeView.of(user);
    }

    // Token válido de un usuario que ya no existe (borrado tras emitirlo): se trata como
    // no autenticado, no como 404, para no confirmar nada sobre ese id.
    private User load(UUID publicId) {
        return users.findByPublicId(publicId)
                .filter(User::canLogIn)
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHENTICATED));
    }
}
