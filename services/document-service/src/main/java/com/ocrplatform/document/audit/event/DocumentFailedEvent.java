package com.ocrplatform.document.audit.event;

import com.ocrplatform.document.audit.enums.AuditEventType;

import java.util.Map;

/**
 * Événement publié quand une opération sur un document échoue.
 * <p>
 * Utilisé pour : échec d'upload, échec d'OCR, échec d'indexation, etc.
 * Le {@link AuditEventType} précis est passé en paramètre.
 *
 * @see BaseAuditEvent
 */
public class DocumentFailedEvent extends BaseAuditEvent {

    /**
     * @param source       l'objet qui publie l'event
     * @param eventType    type d'échec (DOCUMENT_UPLOAD_FAILED, DOCUMENT_OCR_FAILED, ...)
     * @param documentId   id du document concerné (peut être null si l'upload a échoué avant la sauvegarde)
     * @param actorId      acteur
     * @param errorMessage message d'erreur lisible
     * @param errorCode    code erreur applicatif (ex: STG-001)
     */
    public DocumentFailedEvent(Object source,
                               AuditEventType eventType,
                               String documentId,
                               String actorId,
                               String errorMessage,
                               String errorCode) {
        super(
                source,
                eventType,
                actorId,
                "DOCUMENT",
                documentId,
                errorMessage,
                Map.of(
                        "errorCode",   errorCode != null ? errorCode : "UNKNOWN",
                        "errorReason", errorMessage != null ? errorMessage : "Unknown error"
                )
        );
    }
}
