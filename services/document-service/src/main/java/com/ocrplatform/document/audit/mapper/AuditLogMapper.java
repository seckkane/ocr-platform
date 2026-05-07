package com.ocrplatform.document.audit.mapper;

import com.ocrplatform.document.audit.entity.AuditLog;
import com.ocrplatform.document.model.dto.response.AuditLogResponse;
import org.springframework.stereotype.Component;

/**
 * Mapper Entity ↔ DTO pour les audit logs.
 * <p>
 * Mapper manuel pour rester simple. Si la complexité augmente (10+ DTO,
 * mapping conditionnel), on peut switcher vers MapStruct ultérieurement.
 */
@Component
public class AuditLogMapper {

    public AuditLogResponse toResponse(AuditLog entity) {
        return AuditLogResponse.builder()
                .id(entity.getId())
                .occurredAt(entity.getOccurredAt())
                .eventType(entity.getEventType())
                .severity(entity.getSeverity())
                .actorId(entity.getActorId())
                .resourceType(entity.getResourceType())
                .resourceId(entity.getResourceId())
                .message(entity.getMessage())
                .details(entity.getDetails())
                .correlationId(entity.getCorrelationId())
                .build();
    }
}
