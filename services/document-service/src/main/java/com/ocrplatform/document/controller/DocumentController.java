package com.ocrplatform.document.controller;

import com.ocrplatform.document.exception.BusinessRuleException;
import com.ocrplatform.document.exception.ErrorCode;
import com.ocrplatform.document.model.dto.response.DocumentResponse;
import com.ocrplatform.document.model.dto.response.PageResponse;
import com.ocrplatform.document.model.entity.Document;
import com.ocrplatform.document.security.AuthenticatedUser;
import com.ocrplatform.document.service.DocumentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.io.InputStream;
import java.net.URI;

/**
 * API publique de gestion des documents.
 * <p>
 * Securisee par JWT Keycloak (cf. {@link com.ocrplatform.document.config.SecurityConfig}).
 * Le {@code ownerId} est derive du sub du JWT.
 * <p>
 * Regles d'acces :
 * <ul>
 *     <li>{@code USER} : peut uploader, lister, lire, supprimer <b>ses propres</b> documents</li>
 *     <li>{@code ADMIN} : acces a tous les documents (pour moderation)</li>
 * </ul>
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
@Tag(name = "Documents", description = "Upload, telechargement et gestion des documents")
public class DocumentController {

    private static final int MAX_PAGE_SIZE = 100;

    private final DocumentService documentService;

    /**
     * Upload d'un document via multipart/form-data.
     * Le {@code ownerId} est extrait du JWT (sub).
     */
    @Operation(summary = "Upload un nouveau document",
            description = "Upload un fichier (PDF, image, TXT) et persiste ses metadonnees. "
                    + "Retourne 201 Created avec le DocumentResponse et un header Location.")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<DocumentResponse> upload(@RequestPart("file") MultipartFile file) {
        String ownerId = AuthenticatedUser.currentActorId();
        Document doc = documentService.upload(file, ownerId);

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(doc.getId())
                .toUri();

        return ResponseEntity.created(location).body(DocumentResponse.from(doc));
    }

    /**
     * Recupere le contenu binaire (download).
     */
    @GetMapping("/{id}/content")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<InputStreamResource> download(@PathVariable String id) {
        Document doc = documentService.getById(id);
        ensureOwnerOrAdmin(doc);

        InputStream content = documentService.getContent(id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(doc.getContentType()))
                .contentLength(doc.getSizeBytes())
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + doc.getOriginalName() + "\"")
                .body(new InputStreamResource(content));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<DocumentResponse> getById(@PathVariable String id) {
        Document doc = documentService.getById(id);
        ensureOwnerOrAdmin(doc);
        return ResponseEntity.ok(DocumentResponse.from(doc));
    }

    /**
     * Liste paginee des documents de l'utilisateur courant.
     * Un ADMIN peut passer {@code ?ownerId=xxx} pour lister ceux d'un autre user.
     */
    @GetMapping
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<PageResponse<DocumentResponse>> list(
            @RequestParam(required = false) String ownerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        int safePage = Math.max(page, 0);

        String currentUser = AuthenticatedUser.currentActorId();

        // Seul un ADMIN peut consulter les documents d'un autre user
        String targetOwner;
        if (ownerId != null && !ownerId.equals(currentUser)) {
            if (!AuthenticatedUser.hasRole("ADMIN")) {
                throw new BusinessRuleException(ErrorCode.BUSINESS_RULE_VIOLATION,
                        "Only admins can list documents of other users");
            }
            targetOwner = ownerId;
        } else {
            targetOwner = currentUser;
        }

        Pageable pageable = PageRequest.of(safePage, safeSize);
        Page<Document> result = documentService.listByOwner(targetOwner, pageable);
        Page<DocumentResponse> mapped = result.map(DocumentResponse::from);

        return ResponseEntity.ok(PageResponse.from(mapped));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        Document doc = documentService.getById(id);
        ensureOwnerOrAdmin(doc);
        documentService.delete(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    /**
     * Verifie que l'utilisateur courant est le owner du document, OU qu'il est admin.
     */
    private void ensureOwnerOrAdmin(Document doc) {
        String currentUser = AuthenticatedUser.currentActorId();
        if (doc.getOwnerId().equals(currentUser)) return;
        if (AuthenticatedUser.hasRole("ADMIN")) return;
        throw new BusinessRuleException(ErrorCode.BUSINESS_RULE_VIOLATION,
                "You don't have access to this document");
    }
}
