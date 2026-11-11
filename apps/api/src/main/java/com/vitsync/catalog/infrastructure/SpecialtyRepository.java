package com.vitsync.catalog.infrastructure;

import com.vitsync.catalog.domain.Specialty;
import com.vitsync.catalog.domain.SpecialtyKind;
import java.util.Collection;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface SpecialtyRepository extends JpaRepository<Specialty, Long> {

    Optional<Specialty> findByCode(String code);

    /**
     * Especialidades activas filtradas en SQL, nunca en memoria.
     *
     * <p>Sin parámetros opcionales a {@code null} ({@code :x is null or ...}): PostgreSQL no
     * sabe de qué tipo es un {@code null} suelto y falla. Quien llama pasa siempre valores:
     * {@code "%"} si no hay texto y todos los tipos si no hay filtro de tipo.
     *
     * @param namePattern patrón {@code LIKE} sobre el nombre en minúsculas
     * @param slugPattern el mismo texto sin tildes, sobre el slug ("cardiologia" encuentra
     *                    "Cardiología")
     */
    @Query("""
            select s from Specialty s
            where s.active = true
              and s.kind in :kinds
              and (lower(s.name) like :namePattern escape '\\' or s.slug like :slugPattern escape '\\')
            """)
    Page<Specialty> searchActive(Collection<SpecialtyKind> kinds, String namePattern, String slugPattern,
                                 Pageable pageable);
}
