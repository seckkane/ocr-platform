package com.ocrplatform.document.controller;

import com.ocrplatform.document.model.dto.response.DocumentResponse;
import com.ocrplatform.document.model.dto.response.PageResponse;
import com.ocrplatform.document.model.entity.Document;
import com.ocrplatform.document.service.DocumentService;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

/**
 * API publique de gestion des documents.
 * <p>
 * Sécurité (Phase 7) : sera protégée par Keycloak. Pour l'instant ownerId
 * est passé en paramètre — sera dérivé du JWT plus tard.
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
public class DocumentController {

    private static final int MAX_PAGE_SIZE = 100;

    private final DocumentService documentService;

    /**
     * Upload d'un document via multipart/form-data.
     * <p>
     * Champs attendus :
     * <ul>
     *     <li>{@code file} : le fichier (PDF, image, etc.)</li>
     *     <li>{@code ownerId} : identifiant de l'utilisateur propriétaire</li>
     * </ul>
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DocumentResponse> upload(
            @RequestPart("file") MultipartFile file,
            @RequestPart("ownerId") @NotBlank String ownerId
    ) {
        Document doc = documentService.upload(file, ownerId);

        // 201 Created + header Location vers la nouvelle ressource
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(doc.getId())
                .toUri();

        return ResponseEntity.created(location).body(DocumentResponse.from(doc));
    }

    @GetMapping("/{id}")
    public ResponseEntity<DocumentResponse> getById(@PathVariable String id) {
        Document doc = documentService.getById(id);
        return ResponseEntity.ok(DocumentResponse.from(doc));
    }

    /**
     * Liste paginée des documents d'un owner.
     */
    @GetMapping
    public ResponseEntity<PageResponse<DocumentResponse>> listByOwner(
            @RequestParam @NotBlank String ownerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        int safePage = Math.max(page, 0);

        Pageable pageable = PageRequest.of(safePage, safeSize);
        Page<Document> result = documentService.listByOwner(ownerId, pageable);
        Page<DocumentResponse> mapped = result.map(DocumentResponse::from);

        return ResponseEntity.ok(PageResponse.from(mapped));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        documentService.delete(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}