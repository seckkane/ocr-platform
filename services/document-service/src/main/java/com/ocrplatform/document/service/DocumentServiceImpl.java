package com.ocrplatform.document.service;

import com.ocrplatform.document.audit.enums.AuditEventType;
import com.ocrplatform.document.audit.event.DocumentFailedEvent;
import com.ocrplatform.document.audit.event.DocumentUploadedEvent;
import com.ocrplatform.document.exception.BusinessRuleException;
import com.ocrplatform.document.exception.DocumentNotFoundException;
import com.ocrplatform.document.exception.ErrorCode;
import com.ocrplatform.document.model.entity.Document;
import com.ocrplatform.document.model.enums.DocumentStatus;
import com.ocrplatform.document.repository.DocumentRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentServiceImpl implements DocumentService {

    /** Types MIME acceptés. À étendre selon les besoins. */
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "application/pdf",
            "image/png",
            "image/jpeg",
            "image/tiff",
            "text/plain"
    );

    /** Taille max : 50 MB. Doit matcher la config multipart de application.yml. */
    private static final long MAX_FILE_SIZE = 50L * 1024 * 1024;

    private final DocumentRepository documentRepository;
    private final StorageService storageService;
    private final ApplicationEventPublisher eventPublisher;

    private final Counter documentsUploadedCounter;
    private final Counter documentsFailedCounter;
    private final Timer documentUploadTimer;
    private final DistributionSummary documentSizeSummary;

    @Override
    @Transactional
    public Document upload(MultipartFile file, String ownerId) {
        return documentUploadTimer.record(() -> doUpload(file, ownerId));
    }

    private Document doUpload(MultipartFile file, String ownerId) {
        validate(file, ownerId);

        String storageKey = null;
        try {
            storageKey = storageService.store(
                    inputStream(file),
                    file.getOriginalFilename(),
                    file.getContentType(),
                    file.getSize()
            );

            Document doc = Document.builder()
                    .originalName(file.getOriginalFilename())
                    .contentType(file.getContentType())
                    .sizeBytes(file.getSize())
                    .storageKey(storageKey)
                    .status(DocumentStatus.UPLOADED)
                    .ownerId(ownerId)
                    .build();
            doc = documentRepository.save(doc);

            eventPublisher.publishEvent(new DocumentUploadedEvent(this, doc));

            // Metriques succes
            documentsUploadedCounter.increment();
            documentSizeSummary.record(doc.getSizeBytes());

            log.info("Document uploaded: id={}, owner={}, size={}",
                    doc.getId(), ownerId, doc.getSizeBytes());
            return doc;

        } catch (RuntimeException e) {
            documentsFailedCounter.increment();
            eventPublisher.publishEvent(new DocumentFailedEvent(
                    this,
                    AuditEventType.DOCUMENT_UPLOAD_FAILED,
                    null,
                    ownerId,
                    e.getMessage(),
                    e instanceof BusinessRuleException bre ? bre.getErrorCode().getCode() : "UNKNOWN"
            ));
            throw e;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public InputStream getContent(String id) {
        Document doc = getById(id);
        return storageService.retrieve(doc.getStorageKey());
    }

    @Override
    @Transactional(readOnly = true)
    public Document getById(String id) {
        return documentRepository.findById(id)
                .orElseThrow(() -> new DocumentNotFoundException(id));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Document> listByOwner(String ownerId, Pageable pageable) {
        return documentRepository.findByOwnerIdOrderByCreatedAtDesc(ownerId, pageable);
    }

    @Override
    @Transactional
    public void delete(String id) {
        Document doc = getById(id);
        try {
            storageService.delete(doc.getStorageKey());
        } catch (RuntimeException e) {
            // Continue : on supprime quand même la ligne BDD pour éviter les zombies.
            log.warn("Failed to delete storage key {} for document {}: {}",
                    doc.getStorageKey(), id, e.getMessage());
        }
        documentRepository.delete(doc);
        log.info("Document deleted: id={}", id);
    }

    // ===========================================================================
    // Validations privées
    // ===========================================================================

    private void validate(MultipartFile file, String ownerId) {
        if (ownerId == null || ownerId.isBlank()) {
            throw new BusinessRuleException(ErrorCode.VALIDATION_FAILED, "ownerId is required");
        }
        if (file == null || file.isEmpty()) {
            throw new BusinessRuleException(ErrorCode.DOCUMENT_EMPTY, "File is empty");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BusinessRuleException(ErrorCode.DOCUMENT_TOO_LARGE,
                    "File exceeds %d bytes".formatted(MAX_FILE_SIZE))
                    .withDetail("actualSize", file.getSize())
                    .withDetail("maxSize", MAX_FILE_SIZE);
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new BusinessRuleException(ErrorCode.DOCUMENT_INVALID_FORMAT,
                    "Unsupported content type: " + contentType)
                    .withDetail("allowed", ALLOWED_CONTENT_TYPES);
        }
    }

    private static java.io.InputStream inputStream(MultipartFile file) {
        try {
            return file.getInputStream();
        } catch (IOException e) {
            throw new BusinessRuleException(ErrorCode.STORAGE_UPLOAD_FAILED,
                    "Cannot read file content");
        }
    }
}
