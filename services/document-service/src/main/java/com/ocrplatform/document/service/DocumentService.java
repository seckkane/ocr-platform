package com.ocrplatform.document.service;

import com.ocrplatform.document.model.entity.Document;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

/**
 * Service métier principal pour la gestion du cycle de vie des documents.
 * <p>
 * Responsabilités :
 * <ul>
 *     <li>Validation des fichiers uploadés (taille, type, etc.)</li>
 *     <li>Stockage des fichiers dans le système de stockage objet (ex: MinIO)</li>
 *     <li>Persistance des métadonnées en base de données</li>
 *     <li>Gestion du cycle de vie (statut, suppression, récupération)</li>
 *     <li>Publication d'événements (audit, traitement OCR, etc.)</li>
 * </ul>
 *
 * <p><b>Note :</b> Cette interface ne gère pas directement la sécurité (ex: Keycloak),
 * qui doit être traitée en amont (controller / gateway).
 */
public interface DocumentService {

    /**
     * Upload un document.
     *
     * <p>Étapes :
     * <ol>
     *     <li>Validation du fichier (non vide, type MIME, taille max...)</li>
     *     <li>Stockage du fichier dans le stockage objet (ex: MinIO)</li>
     *     <li>Création et sauvegarde de l'entité {@link Document}</li>
     *     <li>Publication d'un événement (ex: DocumentUploadedEvent)</li>
     * </ol>
     *
     * @param file     fichier à uploader (multipart HTTP)
     * @param ownerId  identifiant du propriétaire (utilisateur)
     * @return document persistant avec ID généré et métadonnées complètes
     *
     * @throws IllegalArgumentException si le fichier est invalide
     * @throws RuntimeException         si erreur de stockage ou base de données
     */
    Document upload(MultipartFile file, String ownerId);

    /**
     * Récupère le contenu binaire d’un document depuis le stockage objet.
     *
     * <p><b>Important :</b> Le caller est responsable de fermer le stream
     * pour éviter les fuites mémoire.
     *
     * @param id identifiant du document
     * @return InputStream du fichier (lecture en streaming)
     *
     * @throws RuntimeException si le document n'existe pas ou si le stockage est inaccessible
     */
    InputStream getContent(String id);

    /**
     * Récupère un document par son identifiant.
     *
     * @param id identifiant unique du document
     * @return entité {@link Document}
     *
     * @throws RuntimeException si le document n'existe pas
     */
    Document getById(String id);

    /**
     * Liste les documents d’un utilisateur avec pagination.
     *
     * @param ownerId identifiant du propriétaire
     * @param pageable paramètres de pagination (page, taille, tri)
     * @return page de documents
     */
    Page<Document> listByOwner(String ownerId, Pageable pageable);

    /**
     * Supprime un document.
     *
     * <p>Étapes :
     * <ul>
     *     <li>Suppression du fichier dans le stockage objet</li>
     *     <li>Suppression des métadonnées en base</li>
     *     <li>Publication éventuelle d’un événement d’audit</li>
     * </ul>
     *
     * @param id identifiant du document
     *
     * @throws RuntimeException si le document n'existe pas ou si la suppression échoue
     */
    void delete(String id);
}
