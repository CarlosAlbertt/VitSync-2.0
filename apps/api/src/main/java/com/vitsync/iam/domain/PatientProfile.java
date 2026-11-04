package com.vitsync.iam.domain;

import com.vitsync.common.crypto.EncryptedStringConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
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
 * Datos clínicos básicos del paciente: tabla {@code patient_profiles}.
 *
 * <p>Composición, no herencia (ADR-0003): la PK es a la vez la FK a {@code users(id)}, así
 * que PostgreSQL garantiza la relación 1:1. {@code @MapsId} le dice a JPA que el id de esta
 * entidad es el del {@link User} asociado.
 *
 * <p>Todos los campos clínicos van cifrados. En el registro se crea vacío; se rellena desde
 * el perfil del paciente en un sprint posterior.
 */
@Entity
@Table(name = "patient_profiles")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PatientProfile {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "health_card_encrypted")
    private String healthCard;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "blood_type_encrypted")
    private String bloodType;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "allergies_encrypted")
    private String allergies;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "conditions_encrypted")
    private String conditions;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "emergency_contact_encrypted")
    private String emergencyContact;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(nullable = false)
    private long version;

    public static PatientProfile emptyFor(User user) {
        PatientProfile p = new PatientProfile();
        p.user = Objects.requireNonNull(user);
        return p;
    }

    @Override
    public boolean equals(Object o) {
        return this == o || (o instanceof PatientProfile other && userId != null && userId.equals(other.userId));
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "PatientProfile[userId=" + userId + "]";
    }
}
