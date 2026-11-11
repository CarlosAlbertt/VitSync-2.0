package com.vitsync.catalog.infrastructure;

import com.vitsync.catalog.domain.Facility;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface FacilityRepository extends JpaRepository<Facility, Long> {

    Page<Facility> findByActiveTrue(Pageable pageable);

    Optional<Facility> findByName(String name);

    Optional<Facility> findByPublicIdAndActiveTrue(UUID publicId);

    /** Médicos asignados a un centro. El resto del filtro (visibles, especialidad) lo hace iam. */
    @Query(value = "SELECT doctor_id FROM doctor_facilities WHERE facility_id = :facilityId", nativeQuery = true)
    List<Long> findDoctorIds(Long facilityId);

    /** Fila de "en qué centros activos pasa consulta cada médico". */
    interface DoctorFacilityRow {
        Long getDoctorId();

        UUID getPublicId();

        String getName();

        String getCity();
    }

    /**
     * Centros activos de todos los médicos de una página, en una sola consulta (ADR-0009).
     * Alias entre comillas: sin ellas PostgreSQL los pasa a minúsculas y la proyección no los
     * encuentra.
     */
    @Query(value = """
            SELECT df.doctor_id AS "doctorId", f.public_id AS "publicId", f.name AS "name", f.city AS "city"
            FROM doctor_facilities df JOIN facilities f ON f.id = df.facility_id
            WHERE df.doctor_id IN (:doctorIds) AND f.active
            ORDER BY f.city, f.name
            """, nativeQuery = true)
    List<DoctorFacilityRow> findActiveByDoctorIds(Collection<Long> doctorIds);

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
