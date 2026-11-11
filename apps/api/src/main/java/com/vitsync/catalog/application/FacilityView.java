package com.vitsync.catalog.application;

import java.util.UUID;

/** Centro tal y como sale del módulo: sin id interno ni entidad (decisión D5). */
public record FacilityView(UUID publicId, String name, String address, String city, String postalCode,
                           String phone) {
}
