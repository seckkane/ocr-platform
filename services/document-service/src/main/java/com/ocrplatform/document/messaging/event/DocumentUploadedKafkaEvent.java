package com.ocrplatform.document.messaging.event;

import com.ocrplatform.document.model.entity.Document;

import java.time.Instant;

/**
 * Contrat publique de l'event "document uploaded" publie sur Kafka.
 * <p>
 * <b>Stable</b> : ce DTO est consomme par d'autres microservices (ocr-service,
 * search-service). Toute modification est <b>breaking</b> pour eux.
 * <p>
 * Pour evoluer : ajouter des champs optionnels uniquement, jamais en supprimer
 * ni en changer le type. Sinon, creer documents.uploaded.v2 et migrer.
 */
public record DocumentUploadedKafkaEvent(
        String documentId,
        String originalName,
        String contentType,
        Long sizeBytes,
        String storageBucket,
        String storageKey,
        String ownerId,
        Instant uploadedAt,
        String correlationId
) {
    public static DocumentUploadedKafkaEvent from(Document doc, String correlationId) {
        return new DocumentUploadedKafkaEvent(
                doc.getId(),
                doc.getOriginalName(),
                doc.getContentType(),
                doc.getSizeBytes(),
                doc.getStorageBucket(),
                doc.getStorageKey(),
                doc.getOwnerId(),
                doc.getCreatedAt(),
                correlationId
        );
    }
}
