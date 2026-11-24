package com.vitsync.catalog.api;

import com.vitsync.catalog.api.dto.FacilityResponse;
import com.vitsync.catalog.api.mapper.CatalogMapper;
import com.vitsync.catalog.application.FacilityCatalog;
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
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Centros médicos: {@code /api/v1/facilities}. Público, sin token (SecurityConfig). */
// @SecurityRequirements vacío: público, sin el candado de Bearer que el contrato pone por defecto.
@SecurityRequirements
@RestController
@RequestMapping("/api/v1/facilities")
@Tag(name = "Catalog", description = "Public catalog: specialties, facilities and doctors")
public class FacilityController {

    private final FacilityCatalog catalog;
    private final CatalogMapper mapper;

    public FacilityController(FacilityCatalog catalog, CatalogMapper mapper) {
        this.catalog = catalog;
        this.mapper = mapper;
    }

    @GetMapping
    @Operation(summary = "List active facilities", description = "Sorted by city and then by name.")
    @ApiResponse(responseCode = "200", description = "A page of facilities")
    @ApiResponse(responseCode = "400", description = "VALIDATION_FAILED",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    public PageResponse<FacilityResponse> list(
            @Parameter(description = "Zero-based page number") @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return PageResponse.of(catalog.list(page, size).map(mapper::toResponse));
    }
}
