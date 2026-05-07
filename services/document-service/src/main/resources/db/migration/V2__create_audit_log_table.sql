-- ============================================================================
-- V2 — Création de la table audit_log
-- ----------------------------------------------------------------------------
-- Trace les événements business du service (uploads, échecs, transitions, etc.).
-- Source de vérité pour le dashboard admin et les investigations.
--
-- Pattern : audit log polymorphe (resource_type + resource_id),
-- pas de FK pour préserver les audits même si la ressource est supprimée.
--
-- Détails contextuels en JSON pour flexibilité (ajout de champs sans migration).
-- ============================================================================

CREATE TABLE audit_log (
    -- Identifiant unique (UUID)
    id              VARCHAR(36) NOT NULL,

    -- Quand l'événement s'est produit
    occurred_at     DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
                    COMMENT 'Timestamp UTC de l''événement',

    -- Type d'événement (enum côté Java : DOCUMENT_UPLOADED, DOCUMENT_FAILED, etc.)
    event_type      VARCHAR(100) NOT NULL
                    COMMENT 'Type d''événement (cf. AuditEventType enum)',

    -- Sévérité (pour filtrer rapidement les WARN/ERROR)
    severity        VARCHAR(20) NOT NULL DEFAULT 'INFO'
                    COMMENT 'INFO, WARN, ERROR',

    -- Qui a déclenché l'événement
    -- (sub du JWT Keycloak en general, "system" si c'est un job interne)
    actor_id        VARCHAR(100) NOT NULL DEFAULT 'system'
                    COMMENT 'Identifiant de l''acteur (user_id ou "system")',

    -- Sur quelle ressource (polymorphe : DOCUMENT, USER, etc.)
    resource_type   VARCHAR(100) NULL
                    COMMENT 'Type de la ressource concernée (DOCUMENT, USER, ...)',
    resource_id     VARCHAR(100) NULL
                    COMMENT 'Identifiant de la ressource concernée',

    -- Message lisible par un humain
    message         VARCHAR(500) NULL
                    COMMENT 'Description courte de l''événement',

    -- Détails contextuels en JSON (flexible : ajouter des champs sans migration)
    details         JSON NULL
                    COMMENT 'Détails contextuels (taille fichier, raison échec, ...)',

    -- ID de corrélation pour relier les events d'une même requête HTTP
    -- (sera rempli automatiquement depuis le MDC -> Phase 5)
    correlation_id  VARCHAR(100) NULL
                    COMMENT 'CorrelationId de la requête HTTP qui a déclenché l''event',

    -- Contraintes
    PRIMARY KEY (id),

    -- Index pour requêtes fréquentes (timeline admin, filtres)
    INDEX idx_audit_log_occurred_at      (occurred_at),
    INDEX idx_audit_log_event_type       (event_type),
    INDEX idx_audit_log_severity         (severity),
    INDEX idx_audit_log_actor            (actor_id),
    INDEX idx_audit_log_resource         (resource_type, resource_id),
    INDEX idx_audit_log_correlation      (correlation_id)
)
    ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_unicode_ci
    COMMENT = 'Audit log des événements business du service';
