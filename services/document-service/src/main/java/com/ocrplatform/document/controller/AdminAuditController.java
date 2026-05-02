package com.ocrplatform.document.controller;

import com.ocrplatform.document.audit.entity.AuditLog;
import com.ocrplatform.document.audit.enums.AuditEventType;
import com.ocrplatform.document.audit.enums.AuditSeverity;
import com.ocrplatform.document.audit.mapper.AuditLogMapper;
import com.ocrplatform.document.audit.repository.AuditLogRepository;
import com.ocrplatform.document.model.dto.response.AuditLogResponse;
import com.ocrplatform.document.model.dto.response.PageResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoint d'administration pour consulter les audit logs.
 * <p>
 * <b>Sécurité (Phase 7)</b> : sera protégé par {@code @PreAuthorize("hasRole('ADMIN')")}
 * une fois Keycloak intégré. Pour l'instant, libre d'accès en dev.
 */

@Profile("!prod")
@Slf4j
@RestController
@RequestMapping("/api/admin/audit")
@RequiredArgsConstructor
public class AdminAuditController {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;

    private final AuditLogRepository auditLogRepository;
    private final AuditLogMapper auditLogMapper;

    /**
     * Liste paginée des audit logs avec filtres optionnels.
     * <p>
     * Si plusieurs filtres sont passés, ils sont appliqués selon la priorité :
     * resource > correlation > eventType > severity > all.
     *
     * @param eventType     filtre par type d'événement (optionnel)
     * @param severity      filtre par sévérité (optionnel)
     * @param resourceType  filtre par type de ressource (optionnel, doit être combiné avec resourceId)
     * @param resourceId    filtre par id de ressource (optionnel, doit être combiné avec resourceType)
     * @param correlationId filtre par correlationId (optionnel)
     * @param page          numéro de page (défaut 0)
     * @param size          taille de page (défaut 20, max 100)
     * @return page d'audit logs au format DTO
     */
    @GetMapping
    public ResponseEntity<PageResponse<AuditLogResponse>> list(
            @RequestParam(required = false) AuditEventType eventType,
            @RequestParam(required = false) AuditSeverity severity,
            @RequestParam(required = false) String resourceType,
            @RequestParam(required = false) String resourceId,
            @RequestParam(required = false) String correlationId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        // Cap la taille de page pour éviter qu'un client demande 10000 éléments
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        int safePage = Math.max(page, 0);

        Pageable pageable = PageRequest.of(safePage, safeSize);
        log.debug("Listing audit logs: page={}, size={}, eventType={}, severity={}, resource={}/{}, correlationId={}",
                safePage, safeSize, eventType, severity, resourceType, resourceId, correlationId);

        Page<AuditLog> result = applyFilters(eventType, severity, resourceType, resourceId, correlationId, pageable);

        Page<AuditLogResponse> mapped = result.map(auditLogMapper::toResponse);
        return ResponseEntity.ok(PageResponse.from(mapped));
    }

    /**
     * Sélectionne le bon repository method selon les filtres présents.
     * <p>
     * Logique simple : on prend le filtre le plus spécifique. Pour des combinaisons
     * complexes (filtres multiples ANDés), on passerait à JPA Specifications ou
     * Querydsl en Phase ultérieure.
     */
    private Page<AuditLog> applyFilters(AuditEventType eventType,
                                        AuditSeverity severity,
                                        String resourceType,
                                        String resourceId,
                                        String correlationId,
                                        Pageable pageable) {

        if (resourceType != null && resourceId != null) {
            return auditLogRepository.findByResourceTypeAndResourceIdOrderByOccurredAtDesc(
                    resourceType, resourceId, pageable);
        }

        if (correlationId != null) {
            // findByCorrelationIdOrderByOccurredAtAsc retourne List, on wrap manuellement
            var list = auditLogRepository.findByCorrelationIdOrderByOccurredAtAsc(correlationId);
            return new org.springframework.data.domain.PageImpl<>(list, pageable, list.size());
        }

        if (eventType != null) {
            return auditLogRepository.findByEventTypeOrderByOccurredAtDesc(eventType, pageable);
        }

        if (severity != null) {
            return auditLogRepository.findBySeverityOrderByOccurredAtDesc(severity, pageable);
        }

        // Pas de filtre : tout retourner (avec pagination)
        return auditLogRepository.findAll(pageable);
    }
}
