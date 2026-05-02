package com.ocrplatform.document.audit.service;

import com.ocrplatform.document.audit.entity.AuditLog;
import com.ocrplatform.document.audit.event.BaseAuditEvent;
import com.ocrplatform.document.audit.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implémentation par défaut de {@link AuditService}.
 * <p>
 * <b>Transactionalité</b> : chaque audit s'exécute dans une transaction <b>séparée</b>
 * ({@link Propagation#REQUIRES_NEW}). Pourquoi ? Pour que :
 * <ul>
 *     <li>L'audit soit persisté <b>même si la transaction métier rollback</b></li>
 *     <li>Un échec d'audit ne fasse <b>jamais</b> rollback la transaction métier</li>
 * </ul>
 *
 * <b>Robustesse</b> : on catche tout pour qu'un échec d'audit ne propage <b>jamais</b>
 * une exception au caller. L'audit est un best-effort : on log et on continue.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditServiceImpl implements AuditService {

    private static final String CORRELATION_ID_KEY = "correlationId";

    private final AuditLogRepository auditLogRepository;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AuditLog record(BaseAuditEvent event) {
        try {
            // Récupère le correlationId depuis le MDC (rempli par le filter HTTP — Phase 5)
            String correlationId = MDC.get(CORRELATION_ID_KEY);

            AuditLog auditLog = AuditLog.builder()
                    .occurredAt(event.getOccurredAt())
                    .eventType(event.getEventType())
                    .severity(event.getSeverity())
                    .actorId(event.getActorId())
                    .resourceType(event.getResourceType())
                    .resourceId(event.getResourceId())
                    .message(event.getMessage())
                    .details(event.getDetails())
                    .correlationId(correlationId)
                    .build();

            AuditLog saved = auditLogRepository.save(auditLog);
            log.debug("Audit log persisted: type={}, severity={}, resource={}/{}",
                    saved.getEventType(), saved.getSeverity(),
                    saved.getResourceType(), saved.getResourceId());
            return saved;

        } catch (Exception e) {
            // Audit best-effort : on ne fait JAMAIS planter le caller à cause d'un échec d'audit.
            // En revanche, on log en ERROR pour qu'un sysadmin puisse le voir.
            log.error("Failed to persist audit log for event {}: {}",
                    event.getEventType(), e.getMessage(), e);
            return null;
        }
    }
}