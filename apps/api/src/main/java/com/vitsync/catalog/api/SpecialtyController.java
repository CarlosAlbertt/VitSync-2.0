package com.vitsync.catalog.api;

import com.vitsync.catalog.api.dto.SpecialtyResponse;
import com.vitsync.catalog.api.mapper.CatalogMapper;
import com.vitsync.catalog.application.SpecialtyCatalog;
import com.vitsync.catalog.domain.SpecialtyKind;
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
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Especialidades: {@code /api/v1/specialties}. Público, sin token (SecurityConfig): el
 * visitante consulta el catálogo antes de tener cuenta.
 */
// @SecurityRequirements vacío: público, sin el candado de Bearer que el contrato pone por defecto.
@SecurityRequirements
@RestController
@RequestMapping("/api/v1/specialties")
@Tag(name = "Catalog", description = "Public catalog: specialties, facilities and doctors")
public class SpecialtyController {

    private final SpecialtyCatalog catalog;
    private final CatalogMapper mapper;

    public SpecialtyController(SpecialtyCatalog catalog, CatalogMapper mapper) {
        this.catalog = catalog;
        this.mapper = mapper;
    }

    @GetMapping
    @Operation(summary = "List active specialties",
            description = "Sorted by name. The text search matches the name with or without accents.")
    @ApiResponse(responseCode = "200", description = "A page of specialties")
    @ApiResponse(responseCode = "400", description = "VALIDATION_FAILED or MALFORMED_REQUEST (unknown kind)",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    public PageResponse<SpecialtyResponse> list(
            @Parameter(description = "Free text over the name") @RequestParam(required = false) @Size(max = 80) String q,
            @RequestParam(required = false) SpecialtyKind kind,
            @Parameter(description = "Zero-based page number") @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return PageResponse.of(catalog.search(q, kind, page, size).map(mapper::toResponse));
    }
}
