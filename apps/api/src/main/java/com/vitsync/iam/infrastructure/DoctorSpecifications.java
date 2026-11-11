package com.vitsync.iam.infrastructure;

import com.vitsync.iam.domain.DoctorProfile;
import com.vitsync.iam.domain.User;
import com.vitsync.iam.domain.UserStatus;
import jakarta.persistence.criteria.Join;
import java.util.Collection;
import org.springframework.data.jpa.domain.Specification;

/**
 * Filtros del cuadro médico como {@link Specification}: cada uno es un trozo de {@code WHERE}
 * que se combina con {@code and()}. Así los filtros opcionales se añaden solo si llegan, y todo
 * se resuelve en una consulta SQL (con su {@code count} para la paginación), nunca en memoria.
 */
public final class DoctorSpecifications {

    private DoctorSpecifications() {
    }

    /**
     * Médicos que se pueden mostrar al público: perfil activo, con especialidad asignada, y
     * cuenta activa y no borrada. Sin especialidad no hay ficha que enseñar.
     */
    public static Specification<DoctorProfile> listed() {
        return (root, query, cb) -> {
            Join<DoctorProfile, User> user = root.join("user");
            return cb.and(
                    cb.isTrue(root.get("active")),
                    cb.isNotNull(root.get("specialtyId")),
                    cb.equal(user.get("status"), UserStatus.ACTIVE),
                    cb.isNull(user.get("deletedAt")));
        };
    }

    public static Specification<DoctorProfile> withSpecialty(Long specialtyId) {
        return (root, query, cb) -> cb.equal(root.get("specialtyId"), specialtyId);
    }

    /** Solo estos médicos: los que otro módulo ya ha filtrado por algo suyo (p. ej. el centro). */
    public static Specification<DoctorProfile> amongIds(Collection<Long> doctorIds) {
        return (root, query, cb) -> root.get("userId").in(doctorIds);
    }
}
