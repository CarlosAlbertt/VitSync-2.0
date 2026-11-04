package com.vitsync.iam.infrastructure;

import com.vitsync.iam.domain.CodePurpose;
import com.vitsync.iam.domain.OneTimeCode;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface OneTimeCodeRepository extends JpaRepository<OneTimeCode, Long> {

    Optional<OneTimeCode> findByUserIdAndPurposeAndConsumedAtIsNull(Long userId, CodePurpose purpose);

    /**
     * Borra el código vivo antes de emitir otro (el índice único solo admite uno).
     *
     * <p>Tiene que ser un DELETE directo ({@code @Modifying}) y no {@code delete(entity)}:
     * al hacer flush, Hibernate ejecuta los INSERT antes que los DELETE, y el código nuevo
     * chocaría con el viejo en el índice único.
     */
    @Modifying
    @Query("DELETE FROM OneTimeCode c WHERE c.userId = :userId AND c.purpose = :purpose AND c.consumedAt IS NULL")
    void deleteActive(Long userId, CodePurpose purpose);
}
