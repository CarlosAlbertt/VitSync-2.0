package com.vitsync.catalog.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.Objects;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Centro médico: tabla {@code facilities} (V1). */
@Entity
@Table(name = "facilities")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Facility {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_id", nullable = false, unique = true, updatable = false)
    private UUID publicId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String address;

    @Column(nullable = false)
    private String city;

    @Column(name = "postal_code", nullable = false, length = 10)
    private String postalCode;

    @Column(length = 20)
    private String phone;

    @Column(name = "image_key")
    private String imageKey;

    @Column(nullable = false)
    private boolean active;

    public static Facility create(String name, String address, String city, String postalCode, String phone) {
        Facility f = new Facility();
        f.publicId = UUID.randomUUID();
        f.name = Objects.requireNonNull(name);
        f.address = Objects.requireNonNull(address);
        f.city = Objects.requireNonNull(city);
        f.postalCode = Objects.requireNonNull(postalCode);
        f.phone = phone;
        f.active = true;
        return f;
    }

    /** Centro cerrado: deja de listarse, pero las citas antiguas lo conservan. */
    public void deactivate() {
        active = false;
    }

    @Override
    public boolean equals(Object o) {
        return this == o || (o instanceof Facility other && publicId != null && publicId.equals(other.publicId));
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(publicId);
    }

    @Override
    public String toString() {
        return "Facility[publicId=" + publicId + "]";
    }
}
