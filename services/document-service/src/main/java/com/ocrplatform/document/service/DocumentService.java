package com.ocrplatform.document.service;

import com.ocrplatform.document.model.entity.Document;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

/**
 * Cœur métier du service : gestion du cycle de vie des documents.
 */
public interface DocumentService {

    /**
     * Upload un document : valide, stocke dans MinIO, persiste les métadonnées,
     * publie un event d'audit.
     */
    Document upload(MultipartFile file, String ownerId);

    Document getById(String id);

    Page<Document> listByOwner(String ownerId, Pageable pageable);

    void delete(String id);
}