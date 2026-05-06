package com.ocrplatform.document.audit.enums;

/**
 * Niveau de sévérité d'un événement d'audit.
 * <p>
 * Permet de filtrer rapidement les events critiques côté admin
 * (afficher seulement les WARN+ ou ERROR).
 */
public enum AuditSeverity {
    /** Événement normal, attendu (cycle de vie nominal). */
    INFO,
    /** Événement anormal mais pas bloquant (format invalide, retry, etc.). */
    WARN,
    /** Erreur critique nécessitant attention (échec OCR, exception, etc.). */
    ERROR
}
