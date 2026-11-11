package com.vitsync.iam.infrastructure;

import com.vitsync.iam.domain.DoctorProfile;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface DoctorProfileRepository extends JpaRepository<DoctorProfile, Long> {

    List<DoctorProfile> findByLicenseNumberIn(Collection<String> licenseNumbers);

    /** Fila del recuento: proyección por interfaz, Spring Data la rellena por alias. */
    interface SpecialtyCount {
        Long getSpecialtyId();

        long getTotal();
    }

    /**
     * Médicos que se pueden consultar, agrupados por especialidad, en una sola consulta para
     * todas las especialidades de la página (ADR-0009: siempre por lote).
     *
     * <p>Cuenta solo médicos con perfil activo y cuenta activa y no borrada: los mismos que
     * aparecerán en el cuadro médico.
     */
    @Query("""
            select d.specialtyId as specialtyId, count(d) as total
            from DoctorProfile d join d.user u
            where d.specialtyId in :specialtyIds
              and d.active = true
              and u.status = com.vitsync.iam.domain.UserStatus.ACTIVE
              and u.deletedAt is null
            group by d.specialtyId
            """)
    List<SpecialtyCount> countListedBySpecialty(Collection<Long> specialtyIds);
}
