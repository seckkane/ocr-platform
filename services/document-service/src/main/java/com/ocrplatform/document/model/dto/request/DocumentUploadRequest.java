package com.ocrplatform.document.model.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Métadonnées additionnelles fournies à l'upload d'un document.
 * Le fichier lui-même arrive en {@code MultipartFile}, séparé.
 */
public record DocumentUploadRequest(

        @NotBlank(message = "ownerId is required")
        @Size(max = 100, message = "ownerId must be at most 100 characters")
        String ownerId,

        @Size(max = 500, message = "tags must be at most 500 characters")
        String tags
) {
}