package com.vitsync.catalog.api;

import com.vitsync.catalog.api.dto.DoctorResponse;
import com.vitsync.catalog.api.mapper.CatalogMapper;
import com.vitsync.catalog.application.DoctorCatalog;
import com.vitsync.common.web.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.util.UUID;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Cuadro médico: {@code /api/v1/doctors}. Público, sin token (SecurityConfig). */
// @SecurityRequirements vacío: público, sin el candado de Bearer que el contrato pone por defecto.
@SecurityRequirements
@RestController
@RequestMapping("/api/v1/doctors")
@Tag(name = "Catalog", description = "Public catalog: specialties, facilities and doctors")
public class DoctorController {

    private final DoctorCatalog catalog;
    private final CatalogMapper mapper;

    public DoctorController(DoctorCatalog catalog, CatalogMapper mapper) {
        this.catalog = catalog;
        this.mapper = mapper;
    }

    @GetMapping
    @Operation(summary = "List the doctors open to the public",
            description = "Sorted by last name. Filters are applied in the database; an unknown specialty or "
                    + "facility returns an empty page, not an error.")
    @ApiResponse(responseCode = "200", description = "A page of doctors")
    @ApiResponse(responseCode = "400", description = "VALIDATION_FAILED or MALFORMED_REQUEST (facility is not a UUID)",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    public PageResponse<DoctorResponse> list(
            @Parameter(description = "Specialty slug, e.g. cardiologia") @RequestParam(required = false)
            @Size(max = 80) String specialty,
            @Parameter(description = "Facility publicId") @RequestParam(required = false) UUID facility,
            @Parameter(description = "Zero-based page number") @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return PageResponse.of(catalog.search(specialty, facility, page, size).map(mapper::toResponse));
    }
}
