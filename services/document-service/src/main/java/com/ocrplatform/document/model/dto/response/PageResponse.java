package com.ocrplatform.document.model.dto.response;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Wrapper de pagination pour les réponses API.
 * <p>
 * Convertit un {@link Page} Spring Data en un format JSON propre et stable.
 * Pourquoi ne pas exposer Page directement ? Parce que Spring Data inclut
 * beaucoup de métadonnées internes (sort, pageable, etc.) qu'on n'a pas
 * envie de leak côté client.
 */
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean hasNext,
        boolean hasPrevious
) {
    /**
     * Construit un {@link PageResponse} à partir d'un {@link Page} Spring Data.
     */
    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.hasNext(),
                page.hasPrevious()
        );
    }
}