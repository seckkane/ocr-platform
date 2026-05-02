package com.ocrplatform.document.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Classe de base pour toutes les entités JPA.
 * <p>
 * Apporte automatiquement :
 * <ul>
 *     <li>Un identifiant UUID (généré côté Java, indépendant de la BDD)</li>
 *     <li>Les timestamps d'audit ({@code createdAt}, {@code updatedAt}) gérés par Spring Data Auditing</li>
 *     <li>Une implémentation correcte de {@code equals}/{@code hashCode} basée sur l'ID</li>
 * </ul>
 *
 * <h3>Pourquoi UUID en String et pas Long auto-incrémenté ?</h3>
 * <ul>
 *     <li>Pas de collision en architecture distribuée (microservices)</li>
 *     <li>Génération côté application (pas besoin d'aller-retour BDD)</li>
 *     <li>Pas de leak du nombre de records via les IDs prévisibles</li>
 *     <li>Compatible multi-BDD sans changement de schéma</li>
 * </ul>
 *
 * <h3>Pourquoi {@code @MappedSuperclass} et pas {@code @Inheritance} ?</h3>
 * Pas de table commune pour BaseEntity : chaque entity concrete a ses propres colonnes
 * (factorisation au niveau Java seulement, pas SQL).
 *
 * <h3>Pourquoi {@code hashCode()} retourne une constante ?</h3>
 * Pattern Vlad Mihalcea : si on basait {@code hashCode} sur l'ID, et qu'on mettait
 * l'entity dans un {@code HashSet} avant le persist, son hash changerait après le persist
 * → corruption silencieuse du Set. Hash constant = stable, au prix d'une dégradation
 * acceptable sur les très grandes collections (peu commun en pratique).
 *
 * @see <a href="https://vladmihalcea.com/the-best-way-to-implement-equals-hashcode-and-tostring-with-jpa-and-hibernate/">Vlad Mihalcea — equals/hashCode in JPA</a>
 */

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {

    @Id
    @Column(name = "id", length = 36, nullable = false, updatable = false)
    private String id = UUID.randomUUID().toString();

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BaseEntity that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}