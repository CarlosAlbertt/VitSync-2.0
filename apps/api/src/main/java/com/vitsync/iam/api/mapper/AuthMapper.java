package com.vitsync.iam.api.mapper;

import com.vitsync.iam.api.dto.RegisterRequest;
import com.vitsync.iam.api.dto.RegistrationResponse;
import com.vitsync.iam.application.RegisterPatientCommand;
import com.vitsync.iam.application.RegisteredPatient;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Traducción DTO HTTP ↔ tipos de la capa application (decisión D5).
 *
 * <p>MapStruct genera la implementación al compilar ({@code AuthMapperImpl} en
 * {@code target/generated-sources}): es código normal, depurable y sin reflexión.
 * {@code componentModel = "spring"} la registra como bean para inyectarla.
 */
@Mapper(componentModel = "spring")
public interface AuthMapper {

    RegisterPatientCommand toCommand(RegisterRequest request);

    @Mapping(target = "status", constant = "PENDING_VERIFICATION")
    RegistrationResponse toResponse(RegisteredPatient registered);
}
