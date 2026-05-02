package com.ocrplatform.document.audit.listener;

import com.ocrplatform.document.audit.event.BaseAuditEvent;
import com.ocrplatform.document.audit.service.AuditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Listener Spring qui écoute tous les {@link BaseAuditEvent} et les délègue
 * à l'{@link AuditService} pour persistance.
 * <p>
 * <b>Asynchrone</b> : annoté {@code @Async("auditExecutor")} pour ne pas bloquer
 * le thread métier qui publie l'event. L'executor est défini dans
 * {@link com.ocrplatform.document.config.AsyncConfig}.
 *
 * <h3>Pourquoi un seul listener pour tous les events ?</h3>
 * On utilise le polymorphisme : le listener accepte la classe parente
 * {@link BaseAuditEvent} et catche tous les events qui en héritent.
 * Pas besoin d'un listener par type d'event — l'audit est uniforme.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuditEventListener {

    private final AuditService auditService;

    /**
     * Reçoit tous les événements d'audit et les persiste de manière asynchrone.
     * <p>
     * Pas de propagation d'exception : un échec ici ne fait pas planter le caller
     * (cf. {@link AuditService#record}).
     */
    @Async("auditExecutor")
    @EventListener
    public void handleAuditEvent(BaseAuditEvent event) {
        log.debug("Received audit event: type={}, source={}",
                event.getEventType(),
                event.getSource().getClass().getSimpleName());
        auditService.record(event);
    }
}