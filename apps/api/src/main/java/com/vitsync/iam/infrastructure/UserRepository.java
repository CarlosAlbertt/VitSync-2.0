package com.vitsync.iam.infrastructure;

import com.vitsync.iam.domain.User;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Acceso a {@code users}. Solo lo usa la capa {@code application} del módulo (ArchitectureTest).
 *
 * <p>Los emails llegan ya normalizados (ver {@code Emails}); el NIF se busca siempre por su
 * blind index, nunca por la columna cifrada.
 */
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Optional<User> findByPublicId(UUID publicId);

    boolean existsByEmailOrNifHash(String email, String nifHash);

    boolean existsByEmail(String email);
}
