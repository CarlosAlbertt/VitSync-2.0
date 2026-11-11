package com.vitsync.common.web;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import org.springframework.data.domain.Page;

/**
 * Forma común de toda respuesta paginada: {@code {content: [...], page: {...}}}.
 *
 * <p>Es la misma forma que usa Spring Data cuando serializa una página "por DTO", y la que
 * ya espera el frontend ({@code Page<T>} en {@code shared/api/types.ts}). Se escribe a mano
 * en vez de devolver {@code Page} directamente porque la serialización de {@code PageImpl}
 * no es un contrato estable: cambia entre versiones y arrastra campos internos.
 */
public record PageResponse<T>(List<T> content, PageMetadata page) {

    /** {@code number} empieza en 0, igual que el parámetro {@code page} de la petición. */
    public record PageMetadata(
            @Schema(example = "0") int number,
            @Schema(example = "20") int size,
            @Schema(example = "12") long totalElements,
            @Schema(example = "1") int totalPages) {
    }

    public static <T> PageResponse<T> of(Page<T> page) {
        return new PageResponse<>(page.getContent(),
                new PageMetadata(page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages()));
    }
}
