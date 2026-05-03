package com.ocrplatform.document.model.dto.response;

import com.ocrplatform.document.model.entity.Document;
import com.ocrplatform.document.model.enums.DocumentStatus;
import lombok.Builder;

import java.time.Instant;

/**
 * Vue publique d'un document. Découple l'API de la structure BDD.
 */
@Builder
public record DocumentResponse(
        String id,
        String originalName,
        String contentType,
        Long sizeBytes,
        DocumentStatus status,
        String ownerId,
        Instant createdAt,
        Instant updatedAt
) {
    public static DocumentResponse from(Document doc) {
        return DocumentResponse.builder()
                .id(doc.getId())
                .originalName(doc.getOriginalName())
                .contentType(doc.getContentType())
                .sizeBytes(doc.getSizeBytes())
                .status(doc.getStatus())
                .ownerId(doc.getOwnerId())
                .createdAt(doc.getCreatedAt())
                .updatedAt(doc.getUpdatedAt())
                .build();
    }
}