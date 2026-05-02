package com.ocrplatform.document.audit.event;

import com.ocrplatform.document.audit.enums.AuditEventType;
import com.ocrplatform.document.model.entity.Document;

import java.util.Map;

/**
 * Événement publié quand un document a été uploadé avec succès.
 * <p>
 * Capture les méta-données utiles pour l'audit (taille, type MIME, owner).
 *
 * @see BaseAuditEvent
 */
public class DocumentUploadedEvent extends BaseAuditEvent {

    public DocumentUploadedEvent(Object source, Document document) {
        super(
                source,
                AuditEventType.DOCUMENT_UPLOADED,
                document.getOwnerId(),
                "DOCUMENT",
                document.getId(),
                "Document '%s' uploaded successfully".formatted(document.getOriginalName()),
                Map.of(
                        "originalName", document.getOriginalName(),
                        "contentType", document.getContentType(),
                        "sizeBytes",   document.getSizeBytes(),
                        "storageKey",  document.getStorageKey()
                )
        );
    }
}