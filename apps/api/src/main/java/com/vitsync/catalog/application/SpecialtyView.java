package com.vitsync.catalog.application;

import java.util.UUID;

/** Especialidad tal y como sale del módulo: sin id interno ni entidad (decisión D5). */
public record SpecialtyView(UUID publicId, String slug, String name, String description, String kind,
                            long doctorCount) {
}
