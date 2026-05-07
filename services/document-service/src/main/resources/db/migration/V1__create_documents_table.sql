-- ============================================================================
-- V1 — Création de la table documents
-- ----------------------------------------------------------------------------
-- Stocke les métadonnées des documents uploadés.
-- Le fichier binaire lui-même est dans MinIO (référencé via storage_key).
-- ============================================================================

CREATE TABLE documents (
    -- Identifiant unique (UUID en string pour compatibilité multi-BDD)
    id              VARCHAR(36) NOT NULL,

    -- Métadonnées du fichier original
    original_name   VARCHAR(255) NOT NULL COMMENT 'Nom du fichier tel qu''uploadé',
    content_type    VARCHAR(100) NOT NULL COMMENT 'MIME type (ex: application/pdf)',
    size_bytes      BIGINT NOT NULL COMMENT 'Taille du fichier en octets',

    -- Référence MinIO (clé S3 dans le bucket "documents")
    storage_bucket  VARCHAR(100) NOT NULL DEFAULT 'documents',
    storage_key     VARCHAR(500) NOT NULL COMMENT 'Chemin de l''objet dans MinIO',

    -- Statut du document (pipeline OCR)
    status          VARCHAR(50) NOT NULL DEFAULT 'UPLOADED'
                    COMMENT 'UPLOADED, PROCESSING, OCR_DONE, INDEXED, FAILED',

    -- Propriétaire (sera lié à Keycloak plus tard)
    owner_id        VARCHAR(100) NOT NULL COMMENT 'ID utilisateur (sub du JWT Keycloak)',

    -- Audit
    created_at      DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at      DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),

    -- Contraintes
    PRIMARY KEY (id),
    INDEX idx_documents_owner (owner_id),
    INDEX idx_documents_status (status),
    INDEX idx_documents_created_at (created_at)
)
    ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_unicode_ci
    COMMENT = 'Métadonnées des documents (fichiers binaires dans MinIO)';
