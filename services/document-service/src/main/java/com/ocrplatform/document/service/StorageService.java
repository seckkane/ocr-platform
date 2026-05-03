package com.ocrplatform.document.service;

import java.io.InputStream;

/**
 * Abstraction du stockage objet. Permet de switcher MinIO ↔ S3 ↔ local
 * sans toucher au code métier.
 */
public interface StorageService {

    /**
     * Stocke un fichier et retourne la clé de stockage générée.
     *
     * @param inputStream contenu du fichier
     * @param originalFilename nom original (utilisé pour générer la clé)
     * @param contentType MIME type
     * @param sizeBytes taille en octets
     * @return la clé de stockage (chemin logique dans le bucket)
     */
    String store(InputStream inputStream, String originalFilename, String contentType, long sizeBytes);

    /**
     * Récupère un fichier par sa clé.
     */
    InputStream retrieve(String storageKey);

    /**
     * Supprime un fichier par sa clé.
     */
    void delete(String storageKey);
}
