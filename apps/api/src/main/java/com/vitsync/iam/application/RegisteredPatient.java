package com.vitsync.iam.application;

import java.util.UUID;

/** Resultado del registro: lo mínimo para responder, sin exponer la entidad. */
public record RegisteredPatient(UUID publicId, String email) {
}
