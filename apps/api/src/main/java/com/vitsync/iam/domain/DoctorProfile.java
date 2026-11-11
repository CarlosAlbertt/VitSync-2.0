package com.vitsync.iam.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * Perfil profesional del médico: tabla {@code doctor_profiles}. Igual que
 * {@link PatientProfile}: PK = FK a {@code users(id)}, sin herencia (ADR-0003).
 *
 * <p>La especialidad se guarda por id ({@code specialty_id}) y no como entidad: es del módulo
 * {@code catalog}, y entre módulos se referencia por id (ADR-0009). Entra ahora
 * porque el médico de demostración del perfil {@code local} necesita su fila: un usuario
 * {@code DOCTOR} sin perfil es justo el fallo de v1 que el ADR quiere evitar.
 */
@Entity
@Table(name = "doctor_profiles")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DoctorProfile {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "license_number", nullable = false, unique = true, length = 32)
    private String licenseNumber;

    @Column(name = "specialty_id")
    private Long specialtyId;

    private String bio;

    @Column(name = "photo_key")
    private String photoKey;

    @Column(nullable = false)
    private boolean active;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(nullable = false)
    private long version;

    public static DoctorProfile of(User user, String licenseNumber) {
        DoctorProfile d = new DoctorProfile();
        d.user = Objects.requireNonNull(user);
        d.licenseNumber = Objects.requireNonNull(licenseNumber);
        d.active = true;
        return d;
    }

    @Override
    public boolean equals(Object o) {
        return this == o || (o instanceof DoctorProfile other && userId != null && userId.equals(other.userId));
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "DoctorProfile[userId=" + userId + "]";
    }
}
