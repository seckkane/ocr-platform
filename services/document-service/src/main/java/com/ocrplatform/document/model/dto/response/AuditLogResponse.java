package com.ocrplatform.document.model.dto.response;

import com.ocrplatform.document.audit.enums.AuditEventType;
import com.ocrplatform.document.audit.enums.AuditSeverity;
import lombok.Builder;

import java.time.Instant;
import java.util.Map;

/**
 * DTO de réponse pour un événement d'audit.
 * <p>
 * Représentation publique de l'entity {@link com.ocrplatform.document.audit.entity.AuditLog}.
 * Découple l'API de la structure BDD : changer le schéma BDD ne casse pas les clients API.
 * <p>
 * Utilisation d'un {@code record} Java 14+ : immuable, concis, parfait pour les DTO.
 */
@Builder
public record AuditLogResponse(
        String id,
        Instant occurredAt,
        AuditEventType eventType,
        AuditSeverity severity,
        String actorId,
        String resourceType,
        String resourceId,
        String message,
        Map<String, Object> details,
        String correlationId
) {
}
