package com.vitsync.iam.infrastructure;

import com.vitsync.iam.domain.RefreshToken;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    /**
     * Lo mismo, pero con {@code SELECT ... FOR UPDATE}: bloquea la fila hasta el final de la
     * transacción. Dos canjes simultáneos del mismo token se ponen en fila: el primero rota,
     * y el segundo ya lo ve revocado. Sin el bloqueo, los dos podrían rotarlo a la vez y
     * dejar dos sesiones vivas de un mismo token.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM RefreshToken t WHERE t.tokenHash = :tokenHash")
    Optional<RefreshToken> findForUpdate(String tokenHash);

    /** Revoca de golpe todos los tokens vivos de una familia (logout, reuso detectado). */
    @Modifying
    @Query("UPDATE RefreshToken t SET t.revokedAt = :now WHERE t.familyId = :familyId AND t.revokedAt IS NULL")
    int revokeFamily(UUID familyId, Instant now);

    /** Revoca todas las sesiones de un usuario (cambio de contraseña). */
    @Modifying
    @Query("UPDATE RefreshToken t SET t.revokedAt = :now WHERE t.user.id = :userId AND t.revokedAt IS NULL")
    int revokeAllForUser(Long userId, Instant now);
}
