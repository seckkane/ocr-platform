package com.ocrplatform.document.model.enums;

import java.util.Map;
import java.util.Set;

/**
 * États possibles d'un document dans le pipeline OCR.
 * <p>
 * <b>Flux nominal :</b>
 * <pre>
 *   UPLOADED → PROCESSING → OCR_DONE → INDEXED
 *      ↓           ↓            ↓
 *      └───────────┴────────────┴─────→ FAILED
 *                                          ↓
 *                                      PROCESSING (retry)
 * </pre>
 * <p>
 * L'enum porte la logique des transitions valides via {@link #canTransitionTo}
 * pour éviter les états incohérents (ex: passer de INDEXED à UPLOADED).
 * <p>
 * <b>Pattern</b> : encapsuler les invariants métier dans le type plutôt que
 * dans le service (DDD-light : "un état sait ce qu'il peut devenir").
 */
public enum DocumentStatus {

    /** Document uploadé avec succès, en attente de traitement OCR. */
    UPLOADED,

    /** Pipeline OCR en cours de traitement. */
    PROCESSING,

    /** OCR terminé avec succès, en attente d'indexation Elasticsearch. */
    OCR_DONE,

    /** Indexé dans Elasticsearch, prêt pour la recherche full-text. */
    INDEXED,

    /** Échec à une étape du pipeline (cause à investiguer dans audit log). */
    FAILED;

    /**
     * Indique si une transition de cet état vers {@code target} est autorisée.
     */
    public boolean canTransitionTo(DocumentStatus target) {
        return ALLOWED_TRANSITIONS.getOrDefault(this, Set.of()).contains(target);
    }

    /**
     * Indique si l'état est terminal (aucune transition possible vers un autre état du flux nominal).
     */
    public boolean isTerminal() {
        return this == INDEXED;
    }

    private static final Map<DocumentStatus, Set<DocumentStatus>> ALLOWED_TRANSITIONS = Map.of(
            UPLOADED,   Set.of(PROCESSING, FAILED), // apres uploaded, processing, failed
            PROCESSING, Set.of(OCR_DONE, FAILED),
            OCR_DONE,   Set.of(INDEXED, FAILED),
            INDEXED,    Set.of(),                  // état terminal de succès
            FAILED,     Set.of(PROCESSING)          // permet de retenter après un échec
    );
}