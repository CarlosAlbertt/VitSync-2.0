package com.vitsync.iam.domain;

import com.vitsync.common.crypto.EncryptedStringConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

/**
 * Identidad y autenticación de una persona: tabla {@code users}.
 *
 * <p><b>Sin herencia JPA</b> (ADR-0003): paciente, médico y administrador no son subclases;
 * el rol es una columna y los datos propios de cada rol van en tablas de perfil 1:1
 * ({@link PatientProfile}). Autenticar solo toca esta tabla.
 *
 * <p>Sin {@code @Data} ni setters públicos genéricos (CONTRIBUTING, P4 de v1): el estado
 * cambia solo por métodos con nombre de negocio ({@link #activate()}, ...), que son los que
 * protegen las reglas.
 *
 * <p>{@code equals}/{@code hashCode} por {@code publicId}: se asigna al crear el objeto y no
 * cambia nunca, a diferencia del {@code id}, que es {@code null} hasta el primer INSERT.
 */
@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // JPA lo necesita; nadie más debe usarlo
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Lo único que sale al exterior (decisión D6). El id interno no aparece nunca en la API.
    @Column(name = "public_id", nullable = false, updatable = false, unique = true)
    private UUID publicId;

    // citext en la BD: se declara para que Hibernate valide el tipo en vez de esperar varchar.
    @Column(nullable = false, unique = true, columnDefinition = "citext")
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserStatus status;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    // El NIF se guarda cifrado (ADR-0005). La entidad trabaja con el valor en claro; el
    // conversor cifra al escribir y descifra al leer.
    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "nif_encrypted", nullable = false)
    private String nif;

    // Blind index del NIF: permite detectar duplicados con un índice único sin descifrar.
    // char(64) en la BD: SqlTypes.CHAR para que la validación del esquema no espere varchar.
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "nif_hash", nullable = false, unique = true, length = 64)
    private String nifHash;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "phone_encrypted")
    private String phone;

    @Column(name = "birth_date", nullable = false)
    private LocalDate birthDate;

    @Column(nullable = false, length = 10)
    private String locale;

    @Column(name = "email_verified_at")
    private Instant emailVerifiedAt;

    @Column(name = "two_factor_enabled", nullable = false)
    private boolean twoFactorEnabled;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    // Bloqueo optimista: dos actualizaciones simultáneas del mismo usuario no se pisan;
    // la segunda falla en vez de sobrescribir en silencio.
    @Version
    @Column(nullable = false)
    private long version;

    /**
     * Alta de un paciente. Es la única forma de crear un {@code User} paciente: nace pendiente
     * de verificar el email. El {@link PatientProfile} se crea a la vez, en el mismo servicio
     * transaccional (ADR-0003).
     *
     * @param email    ya normalizado ({@link Emails#normalize})
     * @param nif      ya normalizado ({@link Nif#normalize}) y validado
     * @param nifHash  blind index del NIF
     */
    public static User newPatient(String email, String passwordHash, String firstName, String lastName,
                                  String nif, String nifHash, String phone, LocalDate birthDate) {
        User u = new User();
        u.publicId = UUID.randomUUID();
        u.email = Objects.requireNonNull(email);
        u.passwordHash = Objects.requireNonNull(passwordHash);
        u.role = Role.PATIENT;
        u.status = UserStatus.PENDING_VERIFICATION;
        u.firstName = Objects.requireNonNull(firstName);
        u.lastName = Objects.requireNonNull(lastName);
        u.nif = Objects.requireNonNull(nif);
        u.nifHash = Objects.requireNonNull(nifHash);
        u.phone = phone;
        u.birthDate = Objects.requireNonNull(birthDate);
        u.locale = "es-ES";
        return u;
    }

    /** Email verificado: la cuenta queda activa. Idempotente si ya lo estaba. */
    public void activate(Instant now) {
        if (status == UserStatus.PENDING_VERIFICATION) {
            status = UserStatus.ACTIVE;
            emailVerifiedAt = now;
        }
    }

    public boolean isEmailVerified() {
        return emailVerifiedAt != null;
    }

    @Override
    public boolean equals(Object o) {
        return this == o || (o instanceof User other && publicId != null && publicId.equals(other.publicId));
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(publicId);
    }

    // Sin datos personales: un toString() acaba en los logs.
    @Override
    public String toString() {
        return "User[publicId=" + publicId + ", role=" + role + ", status=" + status + "]";
    }
}
