package com.ocrplatform.document.audit.enums;

import lombok.Getter;

/**
 * Catégories d'événements business tracés dans l'audit log.
 * <p>
 * Chaque type porte une {@link AuditSeverity} par défaut, qui peut être
 * surchargée à la création de l'event si besoin (ex: un upload "normal"
 * en INFO, mais un upload échoué en ERROR).
 * <p>
 * Convention de nommage : {@code <RESSOURCE>_<ACTION_PASSEE>}.
 * <ul>
 *   <li>{@code DOCUMENT_UPLOADED} = un document a été uploadé</li>
 *   <li>{@code DOCUMENT_OCR_FAILED} = l'OCR d'un document a échoué</li>
 * </ul>
 */
@Getter
public enum AuditEventType {

    // ===== Documents : cycle de vie nominal =====
    DOCUMENT_UPLOADED       (AuditSeverity.INFO),
    DOCUMENT_OCR_STARTED    (AuditSeverity.INFO),
    DOCUMENT_OCR_COMPLETED  (AuditSeverity.INFO),
    DOCUMENT_INDEXED        (AuditSeverity.INFO),
    DOCUMENT_DELETED        (AuditSeverity.INFO),

    // ===== Documents : erreurs =====
    DOCUMENT_UPLOAD_FAILED  (AuditSeverity.ERROR),
    DOCUMENT_OCR_FAILED     (AuditSeverity.ERROR),
    DOCUMENT_INDEX_FAILED   (AuditSeverity.ERROR),

    // ===== Documents : avertissements =====
    DOCUMENT_INVALID_FORMAT (AuditSeverity.WARN),
    DOCUMENT_OVERSIZED      (AuditSeverity.WARN),

    // ===== Système =====
    SYSTEM_STARTUP          (AuditSeverity.INFO),
    SYSTEM_ERROR            (AuditSeverity.ERROR);

    private final AuditSeverity defaultSeverity;

    AuditEventType(AuditSeverity defaultSeverity) {
        this.defaultSeverity = defaultSeverity;
    }
}
