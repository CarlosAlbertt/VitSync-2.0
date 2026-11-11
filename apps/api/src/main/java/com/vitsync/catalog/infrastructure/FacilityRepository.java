package com.vitsync.catalog.infrastructure;

import com.vitsync.catalog.domain.Facility;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface FacilityRepository extends JpaRepository<Facility, Long> {

    Page<Facility> findByActiveTrue(Pageable pageable);

    Optional<Facility> findByName(String name);

    /**
     * Asigna un médico a un centro ({@code doctor_facilities}, tabla de {@code catalog}).
     * Idempotente: si ya estaba asignado, no hace nada.
     *
     * <p>Es una tabla de enlace sin datos propios, así que no merece entidad. El médico se
     * referencia por id (ADR-0009); la FK a {@code doctor_profiles} garantiza que existe.
     */
    @Modifying
    @Query(value = """
            INSERT INTO doctor_facilities (doctor_id, facility_id) VALUES (:doctorId, :facilityId)
            ON CONFLICT DO NOTHING
            """, nativeQuery = true)
    void assignDoctor(Long doctorId, Long facilityId);
}
