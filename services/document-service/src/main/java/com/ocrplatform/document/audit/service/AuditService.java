package com.ocrplatform.document.audit.service;

import com.ocrplatform.document.audit.entity.AuditLog;
import com.ocrplatform.document.audit.event.BaseAuditEvent;

/**
 * Service d'audit.
 * <p>
 * Responsabilité : transformer un {@link BaseAuditEvent} en {@link AuditLog} persisté en BDD.
 * <p>
 * <b>Architecture</b> : ce service est appelé par {@link com.ocrplatform.document.audit.listener.AuditEventListener}
 * (asynchrone). Aucun service métier ne devrait l'appeler directement — passer par les events.
 */
public interface AuditService {

    /**
     * Persiste un événement d'audit en BDD à partir d'un {@link BaseAuditEvent}.
     *
     * @param event l'événement à persister
     * @return l'audit log créé
     */
    AuditLog record(BaseAuditEvent event);
}