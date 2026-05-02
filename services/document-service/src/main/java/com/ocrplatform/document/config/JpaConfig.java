package com.ocrplatform.document.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Configuration JPA.
 * <p>
 * Active l'auditing automatique des entités JPA. Les champs annotés
 * {@code @CreatedDate} et {@code @LastModifiedDate} dans
 * {@link com.ocrplatform.document.model.entity.BaseEntity} seront
 * remplis automatiquement par Spring Data au persist / update.
 *
 * <h3>Pourquoi une classe de config dédiée ?</h3>
 * Plutôt que de mettre {@code @EnableJpaAuditing} sur la classe
 * {@code @SpringBootApplication}, on l'isole ici pour :
 * <ul>
 *     <li>Garder la classe Application minimaliste (single responsibility)</li>
 *     <li>Pouvoir désactiver l'auditing dans certains tests via {@code @MockBean}</li>
 *     <li>Faciliter l'ajout futur d'un {@code AuditorAware} (qui a modifié quoi)</li>
 * </ul>
 */
@Configuration
@EnableJpaAuditing
public class JpaConfig {
}