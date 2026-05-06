package com.ocrplatform.document.audit.repository;

import com.ocrplatform.document.audit.entity.AuditLog;
import com.ocrplatform.document.audit.enums.AuditEventType;
import com.ocrplatform.document.audit.enums.AuditSeverity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

/**
 * Repository Spring Data JPA pour {@link AuditLog}.
 * <p>
 * Méthodes dérivées (Spring Data génère le SQL automatiquement à partir du nom).
 * Pas de requêtes JPQL custom pour l'instant — on les ajoutera si besoin pour des
 * agrégations (ex: count by event_type sur 24h).
 */
@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, String> {

    /**
     * Recherche paginée par type d'événement.
     */
    Page<AuditLog> findByEventTypeOrderByOccurredAtDesc(AuditEventType eventType, Pageable pageable);

    /**
     * Recherche paginée par sévérité (ex: tous les WARN+).
     */
    Page<AuditLog> findBySeverityOrderByOccurredAtDesc(AuditSeverity severity, Pageable pageable);

    /**
     * Recherche paginée des events liés à une ressource précise (ex: tous les events
     * d'un document donné).
     */
    Page<AuditLog> findByResourceTypeAndResourceIdOrderByOccurredAtDesc(
            String resourceType,
            String resourceId,
            Pageable pageable);

    /**
     * Tous les events d'une même requête HTTP (via correlationId).
     * Sans pagination : une requête a typiquement < 10 events.
     */
    List<AuditLog> findByCorrelationIdOrderByOccurredAtAsc(String correlationId);

    /**
     * Recherche dans une fenêtre temporelle (ex: events des dernières 24h).
     */
    Page<AuditLog> findByOccurredAtBetweenOrderByOccurredAtDesc(
            Instant from,
            Instant to,
            Pageable pageable);
}
