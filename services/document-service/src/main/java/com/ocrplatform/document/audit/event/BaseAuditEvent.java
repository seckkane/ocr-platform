package com.ocrplatform.document.audit.event;

import com.ocrplatform.document.audit.enums.AuditEventType;
import com.ocrplatform.document.audit.enums.AuditSeverity;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Classe parente de tous les événements d'audit.
 * <p>
 * Hérite de {@link ApplicationEvent} pour être publié via {@link org.springframework.context.ApplicationEventPublisher}.
 * <p>
 * <b>Pattern</b> : chaque event concret (ex: {@link DocumentUploadedEvent}) hérite de cette classe
 * et peut surcharger les valeurs par défaut (severity, message, details).
 *
 * <h3>Cycle de vie d'un event</h3>
 * <ol>
 *   <li>Un service métier crée l'event : {@code new DocumentUploadedEvent(this, document)}</li>
 *   <li>Le service publie : {@code eventPublisher.publishEvent(event)}</li>
 *   <li>Spring distribue l'event aux {@code @EventListener} qui matchent le type</li>
 *   <li>Le {@code AuditEventListener} persiste un {@link com.ocrplatform.document.audit.entity.AuditLog}</li>
 * </ol>
 */
@Getter
public abstract class BaseAuditEvent extends ApplicationEvent {

    /** Timestamp d'occurrence de l'événement (rempli automatiquement). */
    private final Instant occurredAt;

    /** Type d'événement (cf. {@link AuditEventType}). */
    private final AuditEventType eventType;

    /** Sévérité (par défaut celle de l'event type, surchargeable). */
    private final AuditSeverity severity;

    /** Identifiant de l'acteur (user_id Keycloak, ou "system"). */
    private final String actorId;

    /** Type de la ressource concernée (ex: "DOCUMENT"). */
    private final String resourceType;

    /** Identifiant de la ressource concernée. */
    private final String resourceId;

    /** Message lisible par un humain. */
    private final String message;

    /** Détails contextuels libres (sera sérialisé en JSON dans la BDD). */
    private final Map<String, Object> details;

    /**
     * Constructeur protégé : seuls les events concrets peuvent l'appeler.
     *
     * @param source       l'objet qui publie l'event (typiquement un Service Spring)
     * @param eventType    type métier de l'événement
     * @param actorId      acteur (user_id ou "system")
     * @param resourceType type de la ressource (ex: "DOCUMENT", peut être null)
     * @param resourceId   id de la ressource (peut être null)
     * @param message      message lisible (peut être null)
     * @param details      détails contextuels (peut être null, devient une map vide)
     */
    protected BaseAuditEvent(Object source,
                             AuditEventType eventType,
                             String actorId,
                             String resourceType,
                             String resourceId,
                             String message,
                             Map<String, Object> details) {
        super(source);
        this.occurredAt = Instant.now();
        this.eventType = eventType;
        this.severity = eventType.getDefaultSeverity();
        this.actorId = actorId != null ? actorId : "system";
        this.resourceType = resourceType;
        this.resourceId = resourceId;
        this.message = message;
        this.details = details != null ? new HashMap<>(details) : new HashMap<>();
    }
}