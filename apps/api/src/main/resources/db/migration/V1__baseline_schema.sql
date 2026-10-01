-- =====================================================================================
-- V1__baseline_schema.sql — esquema base de VitSync 2.0
--
-- Origen: docs/GUIA_CONSTRUCCION.md, Fase 1.4 ("Tablas núcleo").
-- El orden de creación difiere del de la guía porque aquí las FK tienen que resolverse:
-- los catálogos (specialties, facilities) se crean antes que doctor_profiles.
--
-- REGLA: esta migración no se edita nunca una vez mergeada. Cualquier corrección va en
-- una V2+ nueva (guía, Fase 2.2).
--
-- Rollback manual: DROP SCHEMA public CASCADE; CREATE SCHEMA public;
-- (aceptable solo en local; en entornos con datos se escribe el DOWN explícito)
--
-- Pendientes del ERD para migraciones posteriores, cuando entren sus funcionalidades:
--   diseases, disease_treatments, care_assignments  (Fase 5+)
-- =====================================================================================

-- citext: email case-insensitive sin funciones en los índices ni LOWER() en cada WHERE.
CREATE EXTENSION IF NOT EXISTS citext;


-- ============================= catálogo =============================
-- Va primero porque doctor_profiles.specialty_id lo referencia.

CREATE TABLE specialties (
    id          bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    public_id   uuid NOT NULL UNIQUE,
    code        varchar(40)  NOT NULL UNIQUE,
    slug        varchar(80)  NOT NULL UNIQUE,
    name        text         NOT NULL,
    description text,
    kind        varchar(20)  NOT NULL
                CHECK (kind IN ('MEDICAL','SURGICAL','DIAGNOSTIC','GENERAL','UNIT')),
    icon_key    text,
    active      boolean      NOT NULL DEFAULT true,
    created_at  timestamptz  NOT NULL DEFAULT now(),
    updated_at  timestamptz  NOT NULL DEFAULT now()
);

CREATE TABLE facilities (
    id          bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    public_id   uuid NOT NULL UNIQUE,
    name        text NOT NULL,
    address     text NOT NULL,
    city        text NOT NULL,
    postal_code varchar(10) NOT NULL,
    phone       varchar(20),
    image_key   text,
    active      boolean NOT NULL DEFAULT true
);


-- ============================= identidad =============================

CREATE TABLE users (
    id                 bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    public_id          uuid        NOT NULL UNIQUE,
    email              citext      NOT NULL UNIQUE,
    password_hash      text        NOT NULL,
    role               varchar(20) NOT NULL
                       CHECK (role IN ('PATIENT','DOCTOR','ADMIN')),
    status             varchar(20) NOT NULL DEFAULT 'PENDING_VERIFICATION'
                       CHECK (status IN ('PENDING_VERIFICATION','ACTIVE','SUSPENDED','DEACTIVATED','ANONYMIZED')),
    first_name         text        NOT NULL,
    last_name          text        NOT NULL,
    -- NIF cifrado + blind index: permite WHERE nif_hash = ? sin descifrar la tabla entera.
    nif_encrypted      text        NOT NULL,
    nif_hash           char(64)    NOT NULL UNIQUE,
    phone_encrypted    text,
    birth_date         date        NOT NULL,
    locale             varchar(10) NOT NULL DEFAULT 'es-ES',
    email_verified_at  timestamptz,
    two_factor_enabled boolean     NOT NULL DEFAULT false,
    created_at         timestamptz NOT NULL DEFAULT now(),
    updated_at         timestamptz NOT NULL DEFAULT now(),
    deleted_at         timestamptz,
    version            bigint      NOT NULL DEFAULT 0
);
CREATE INDEX idx_users_role_status ON users (role, status) WHERE deleted_at IS NULL;


-- ===================== perfiles (sin herencia JPA, ADR-0003) =====================

CREATE TABLE patient_profiles (
    user_id                     bigint PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    health_card_encrypted       text,
    blood_type_encrypted        text,
    allergies_encrypted         text,
    conditions_encrypted        text,
    emergency_contact_encrypted text,
    created_at                  timestamptz NOT NULL DEFAULT now(),
    updated_at                  timestamptz NOT NULL DEFAULT now(),
    version                     bigint NOT NULL DEFAULT 0
);

CREATE TABLE doctor_profiles (
    user_id        bigint PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    license_number varchar(32) NOT NULL UNIQUE,
    specialty_id   bigint REFERENCES specialties(id) ON DELETE SET NULL,
    bio            text,
    photo_key      text,                       -- clave en el bucket S3, no URL
    active         boolean NOT NULL DEFAULT true,
    created_at     timestamptz NOT NULL DEFAULT now(),
    updated_at     timestamptz NOT NULL DEFAULT now(),
    version        bigint NOT NULL DEFAULT 0
);
CREATE INDEX idx_doctor_profiles_specialty ON doctor_profiles (specialty_id) WHERE active;

CREATE TABLE doctor_facilities (
    doctor_id   bigint NOT NULL REFERENCES doctor_profiles(user_id) ON DELETE CASCADE,
    facility_id bigint NOT NULL REFERENCES facilities(id) ON DELETE CASCADE,
    PRIMARY KEY (doctor_id, facility_id)
);


-- ============================= agenda =============================

CREATE TABLE schedule_rules (          -- disponibilidad semanal recurrente
    id           bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    doctor_id    bigint NOT NULL REFERENCES doctor_profiles(user_id) ON DELETE CASCADE,
    facility_id  bigint NOT NULL REFERENCES facilities(id),
    weekday      smallint NOT NULL CHECK (weekday BETWEEN 1 AND 7),   -- ISO-8601
    start_time   time NOT NULL,
    end_time     time NOT NULL,
    slot_minutes smallint NOT NULL DEFAULT 20 CHECK (slot_minutes BETWEEN 5 AND 120),
    valid_from   date NOT NULL,
    valid_to     date,
    CHECK (end_time > start_time)
);

CREATE TABLE schedule_exceptions (     -- vacaciones, festivos, bloqueos puntuales
    id        bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    doctor_id bigint NOT NULL REFERENCES doctor_profiles(user_id) ON DELETE CASCADE,
    starts_at timestamptz NOT NULL,
    ends_at   timestamptz NOT NULL,
    reason    text,
    CHECK (ends_at > starts_at)
);


-- ============================= citas =============================

CREATE TABLE appointments (
    id           bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    public_id    uuid NOT NULL UNIQUE,
    patient_id   bigint NOT NULL REFERENCES patient_profiles(user_id),
    doctor_id    bigint NOT NULL REFERENCES doctor_profiles(user_id),
    facility_id  bigint REFERENCES facilities(id),
    starts_at    timestamptz NOT NULL,
    duration_min smallint NOT NULL DEFAULT 20,
    modality     varchar(20) NOT NULL CHECK (modality IN ('IN_PERSON','TELEMEDICINE')),
    status       varchar(20) NOT NULL DEFAULT 'SCHEDULED'
                 CHECK (status IN ('SCHEDULED','CONFIRMED','COMPLETED','CANCELLED','NO_SHOW')),
    reason       text,
    meeting_url  text,
    cancelled_at timestamptz,
    cancelled_by bigint REFERENCES users(id),
    created_at   timestamptz NOT NULL DEFAULT now(),
    updated_at   timestamptz NOT NULL DEFAULT now(),
    version      bigint NOT NULL DEFAULT 0
);

-- La garantía real anti-doble-reserva vive en la BD, no en el servicio (se hereda de v1).
CREATE UNIQUE INDEX ux_appointments_doctor_slot_active
    ON appointments (doctor_id, starts_at)
    WHERE status <> 'CANCELLED';

CREATE INDEX idx_appointments_patient_time ON appointments (patient_id, starts_at DESC);
CREATE INDEX idx_appointments_doctor_time  ON appointments (doctor_id,  starts_at DESC);


-- ============================= clínico =============================

CREATE TABLE medical_reports (
    id              bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    public_id       uuid NOT NULL UNIQUE,
    patient_id      bigint NOT NULL REFERENCES patient_profiles(user_id),
    doctor_id       bigint REFERENCES doctor_profiles(user_id),
    title           text NOT NULL,
    kind            varchar(30) NOT NULL
                    CHECK (kind IN ('LAB','DIAGNOSIS','IMAGING','PRESCRIPTION','OTHER')),
    issued_on       date NOT NULL,
    notes_encrypted text,
    created_at      timestamptz NOT NULL DEFAULT now(),
    deleted_at      timestamptz
);
CREATE INDEX idx_reports_patient ON medical_reports (patient_id, issued_on DESC)
    WHERE deleted_at IS NULL;

CREATE TABLE report_files (
    id              bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    report_id       bigint NOT NULL REFERENCES medical_reports(id) ON DELETE CASCADE,
    storage_key     text NOT NULL,          -- clave en el bucket, no URL
    filename        text NOT NULL,
    content_type    varchar(100) NOT NULL,  -- detectado con Tika, no el que declara el cliente
    size_bytes      bigint NOT NULL,
    checksum_sha256 char(64) NOT NULL,
    uploaded_at     timestamptz NOT NULL DEFAULT now()
);


-- ============================= mensajería =============================

CREATE TABLE conversations (
    id         bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    public_id  uuid NOT NULL UNIQUE,
    created_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE conversation_members (
    conversation_id bigint NOT NULL REFERENCES conversations(id) ON DELETE CASCADE,
    user_id         bigint NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    last_read_at    timestamptz,
    PRIMARY KEY (conversation_id, user_id)
);

CREATE TABLE messages (
    id              bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    conversation_id bigint NOT NULL REFERENCES conversations(id) ON DELETE CASCADE,
    sender_id       bigint NOT NULL REFERENCES users(id),
    body_encrypted  text NOT NULL,
    sent_at         timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX idx_messages_conversation ON messages (conversation_id, sent_at DESC);


-- ===================== seguridad / cumplimiento =====================

CREATE TABLE refresh_tokens (
    id           bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id      bigint NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash   char(64) NOT NULL UNIQUE,     -- SHA-256 del token opaco; el token no se guarda
    family_id    uuid NOT NULL,                -- detección de reuso por familia (ADR-0004)
    expires_at   timestamptz NOT NULL,
    revoked_at   timestamptz,
    ip_address   inet,
    user_agent   varchar(512),
    last_used_at timestamptz,
    created_at   timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX idx_refresh_user ON refresh_tokens (user_id) WHERE revoked_at IS NULL;

-- Append-only y sin FK al usuario a propósito: la traza sobrevive al borrado RGPD.
CREATE TABLE audit_logs (
    id          bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    actor_ref   varchar(64),            -- public_id o seudónimo tras anonimizar
    action      varchar(48) NOT NULL,
    target_type varchar(48),
    target_ref  varchar(64),
    success     boolean NOT NULL,
    ip_address  inet,
    trace_id    varchar(32),            -- correlación con los logs de la app
    details     jsonb,
    occurred_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX idx_audit_actor  ON audit_logs (actor_ref, occurred_at DESC);
CREATE INDEX idx_audit_action ON audit_logs (action, occurred_at DESC);

CREATE TABLE user_consents (
    id             bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id        bigint NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    consent_type   varchar(40) NOT NULL,  -- HEALTH_DATA_PROCESSING, MARKETING, COOKIES...
    granted        boolean NOT NULL,
    policy_version varchar(20) NOT NULL,
    occurred_at    timestamptz NOT NULL DEFAULT now(),
    ip_address     inet
);

CREATE TABLE gdpr_requests (
    id            bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id       bigint NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    kind          varchar(20) NOT NULL CHECK (kind IN ('ACCESS','EXPORT','ERASURE')),
    status        varchar(20) NOT NULL DEFAULT 'PENDING',
    requested_at  timestamptz NOT NULL DEFAULT now(),
    completed_at  timestamptz,
    scheduled_for timestamptz            -- p. ej. anonimización tras el periodo legal
);


-- ===================== outbox (emails y eventos, fiables) =====================

CREATE TABLE outbox_messages (
    id              bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    type            varchar(60) NOT NULL,     -- APPOINTMENT_CONFIRMED, VERIFY_EMAIL...
    payload         jsonb NOT NULL,
    status          varchar(20) NOT NULL DEFAULT 'PENDING',
    attempts        smallint NOT NULL DEFAULT 0,
    next_attempt_at timestamptz NOT NULL DEFAULT now(),
    last_error      text,
    created_at      timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX idx_outbox_pending ON outbox_messages (next_attempt_at)
    WHERE status = 'PENDING';
