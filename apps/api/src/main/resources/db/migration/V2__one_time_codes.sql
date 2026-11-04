-- =====================================================================================
-- V2__one_time_codes.sql — códigos de un solo uso (verificación de email y recuperación)
--
-- V1 no tenía dónde guardarlos. Una sola tabla para los dos usos, distinguidos por
-- "purpose": tienen la misma forma (hash, caducidad, intentos) y las mismas reglas.
--
-- REGLA: esta migración no se edita nunca una vez mergeada. Cualquier corrección va en
-- una V3+ nueva.
-- =====================================================================================

CREATE TABLE one_time_codes (
    id          bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id     bigint      NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    purpose     varchar(30) NOT NULL
                CHECK (purpose IN ('EMAIL_VERIFICATION','PASSWORD_RESET')),
    -- HMAC-SHA256 del código (con el pepper del blind index), nunca el código: con la BD
    -- robada no se puede usar un código vigente, ni sacarlo por fuerza bruta sin el pepper.
    code_hash   char(64)    NOT NULL,
    attempts    integer     NOT NULL DEFAULT 0 CHECK (attempts >= 0),
    expires_at  timestamptz NOT NULL,
    consumed_at timestamptz,
    created_at  timestamptz NOT NULL DEFAULT now()
);

-- Como mucho un código vivo por usuario y propósito: pedir uno nuevo invalida el anterior.
-- Lo garantiza la BD, no el servicio.
CREATE UNIQUE INDEX ux_one_time_codes_active
    ON one_time_codes (user_id, purpose)
    WHERE consumed_at IS NULL;

-- outbox_messages.status nació en V1 sin CHECK. Se añade aquí (V1 no se toca): los únicos
-- estados que conoce el worker.
ALTER TABLE outbox_messages
    ADD CONSTRAINT ck_outbox_messages_status CHECK (status IN ('PENDING','SENT','FAILED'));
