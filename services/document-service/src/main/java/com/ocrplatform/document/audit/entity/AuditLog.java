package com.ocrplatform.document.audit.entity;

import com.ocrplatform.document.audit.converter.MapToJsonConverter;
import com.ocrplatform.document.audit.enums.AuditEventType;
import com.ocrplatform.document.audit.enums.AuditSeverity;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Entrée d'audit log : trace un événement business du service.
 * <p>
 * Cette entity est volontairement <b>indépendante</b> de {@link com.ocrplatform.document.model.entity.BaseEntity} :
 * <ul>
 *     <li>L'audit log a sa propre temporalité ({@code occurredAt}, pas {@code createdAt})</li>
 *     <li>Pas de {@code updatedAt} : un audit log est <b>immuable</b> par essence</li>
 *     <li>Pas de FK vers les autres entités : on garde l'audit même si la ressource est supprimée</li>
 * </ul>
 *
 * @see AuditEventType
 * @see AuditSeverity
 */
@Entity
@Table(name = "audit_log", indexes = {
        @Index(name = "idx_audit_log_occurred_at", columnList = "occurred_at"),
        @Index(name = "idx_audit_log_event_type",  columnList = "event_type"),
        @Index(name = "idx_audit_log_severity",    columnList = "severity"),
        @Index(name = "idx_audit_log_actor",       columnList = "actor_id"),
        @Index(name = "idx_audit_log_resource",    columnList = "resource_type, resource_id"),
        @Index(name = "idx_audit_log_correlation", columnList = "correlation_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class AuditLog {

    @Id
    @Column(name = "id", length = 36, nullable = false, updatable = false)
    @Builder.Default //permet l'affectation des valeurs par defaut
    private String id = UUID.randomUUID().toString();

    @Column(name = "occurred_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant occurredAt = Instant.now();

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 100, updatable = false)
    private AuditEventType eventType;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false, length = 20, updatable = false)
    @Builder.Default
    private AuditSeverity severity = AuditSeverity.INFO;

    @Column(name = "actor_id", nullable = false, length = 100, updatable = false)
    @Builder.Default
    private String actorId = "system";

    @Column(name = "resource_type", length = 100, updatable = false)
    private String resourceType;

    @Column(name = "resource_id", length = 100, updatable = false)
    private String resourceId;

    @Column(name = "message", length = 500, updatable = false)
    private String message;

    /**
     * Détails contextuels libres, sérialisés en JSON via {@link MapToJsonConverter}.
     */
    @Convert(converter = MapToJsonConverter.class)
    @Column(name = "details", columnDefinition = "JSON", updatable = false)
    @Builder.Default
    private Map<String, Object> details = new HashMap<>();

    @Column(name = "correlation_id", length = 100, updatable = false)
    private String correlationId;

    // ============================================================================
    // equals / hashCode (cf. BaseEntity, même pattern)
    // ============================================================================

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AuditLog that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
