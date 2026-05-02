package com.ocrplatform.document.model.entity;

import com.ocrplatform.document.exception.BusinessRuleException;
import com.ocrplatform.document.exception.ErrorCode;
import com.ocrplatform.document.model.enums.DocumentStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.DynamicUpdate;

/**
 * Métadonnées d'un document dans la plateforme.
 * <p>
 * Le fichier binaire lui-même est stocké dans MinIO et référencé via
 * {@link #storageBucket} et {@link #storageKey}. Cette entity ne stocke
 * que les méta-données et le statut dans le pipeline OCR.
 *
 * @see DocumentStatus
 */
@Entity
@Table(name = "documents", indexes = {
        @Index(name = "idx_documents_owner",      columnList = "owner_id"),
        @Index(name = "idx_documents_status",     columnList = "status"),
        @Index(name = "idx_documents_created_at", columnList = "created_at")
})
@DynamicUpdate //Hibernate n’update que les champs modifiés
@Getter
@Setter
@NoArgsConstructor
//n'ignore pas les champs du parent
@SuperBuilder
//évite d’afficher clé de stockage (sécurité/logs)
@ToString(callSuper = true, exclude = {"storageKey"})
public class Document extends BaseEntity {

    @Column(name = "original_name", nullable = false, length = 255)
    private String originalName;

    @Column(name = "content_type", nullable = false, length = 100)
    private String contentType;

    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;

    @Column(name = "storage_bucket", nullable = false, length = 100)
    @Builder.Default
    private String storageBucket = "documents";

    @Column(name = "storage_key", nullable = false, length = 500)
    private String storageKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    @Builder.Default
    private DocumentStatus status = DocumentStatus.UPLOADED;

    @Column(name = "owner_id", nullable = false, length = 100)
    private String ownerId;

    // ============================================================================
    // Logique métier (DDD-light : l'entity protège ses invariants)
    // ============================================================================

    /**
     * Fait évoluer le statut du document.
     *
     * @throws BusinessRuleException si la transition n'est pas autorisée
     *                               (cf. {@link DocumentStatus#canTransitionTo})
     */
    public void transitionTo(DocumentStatus target) {
        if (!this.status.canTransitionTo(target)) {
            throw new BusinessRuleException(
                    ErrorCode.BUSINESS_RULE_VIOLATION,
                    "Invalid status transition: %s -> %s".formatted(this.status, target)
            );
        }
        this.status = target;
    }
}