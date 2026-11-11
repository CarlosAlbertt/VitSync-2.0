package com.vitsync.catalog.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * Especialidad médica: tabla {@code specialties} (V1).
 *
 * <p>{@code code} es la clave estable para el código y los datos de demostración
 * ({@code CARDIOLOGY}); {@code slug} es la que va en las URLs del frontend
 * ({@code cardiologia}) y, al no llevar tildes, también sirve para buscar sin ellas.
 * Hacia fuera de la API solo viaja el {@code publicId}, nunca el {@code id} interno.
 */
@Entity
@Table(name = "specialties")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Specialty {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_id", nullable = false, unique = true, updatable = false)
    private UUID publicId;

    @Column(nullable = false, unique = true, length = 40, updatable = false)
    private String code;

    @Column(nullable = false, unique = true, length = 80)
    private String slug;

    @Column(nullable = false)
    private String name;

    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SpecialtyKind kind;

    @Column(name = "icon_key")
    private String iconKey;

    @Column(nullable = false)
    private boolean active;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public static Specialty create(String code, String slug, String name, String description, SpecialtyKind kind) {
        Specialty s = new Specialty();
        s.publicId = UUID.randomUUID();
        s.code = Objects.requireNonNull(code);
        s.slug = Objects.requireNonNull(slug);
        s.name = Objects.requireNonNull(name);
        s.description = description;
        s.kind = Objects.requireNonNull(kind);
        s.active = true;
        return s;
    }

    /** Retirada del catálogo: deja de listarse, pero las citas e informes antiguos la conservan. */
    public void deactivate() {
        active = false;
    }

    @Override
    public boolean equals(Object o) {
        return this == o || (o instanceof Specialty other && publicId != null && publicId.equals(other.publicId));
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(publicId);
    }

    @Override
    public String toString() {
        return "Specialty[code=" + code + "]";
    }
}
