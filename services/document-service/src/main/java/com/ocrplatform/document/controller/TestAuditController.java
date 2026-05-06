package com.ocrplatform.document.controller;

import com.ocrplatform.document.audit.event.DocumentFailedEvent;
import com.ocrplatform.document.audit.event.DocumentUploadedEvent;
import com.ocrplatform.document.audit.enums.AuditEventType;
import com.ocrplatform.document.model.entity.Document;
import com.ocrplatform.document.model.enums.DocumentStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * <b>Controller utilitaire DEV/TEST uniquement</b> — non chargé en profile prod.
 * <p>
 * Permet de déclencher manuellement des events d'audit pour valider :
 * <ul>
 *   <li>Le pipeline event-driven async (publish → listener → service → BDD)</li>
 *   <li>Le format des messages d'audit en cas de bug suspecté</li>
 *   <li>Une démo rapide à un collègue ou pour onboarding</li>
 * </ul>
 * <p>
 * <b>NE PAS appeler depuis du code métier réel</b> — passer par les services
 * et les events publiés par eux ({@link com.ocrplatform.document.audit.event}).
 *
 * @see com.ocrplatform.document.audit.event.DocumentUploadedEvent
 * @see com.ocrplatform.document.audit.event.DocumentFailedEvent
 */
@Profile("dev")
@Slf4j
@RestController
@RequestMapping("/api/test/audit")
@RequiredArgsConstructor
public class TestAuditController {

    private final ApplicationEventPublisher eventPublisher;

    /** Simule un upload réussi : publie un DocumentUploadedEvent. */
    @GetMapping("/upload-success")
    public String triggerUploadSuccess() {
        // Crée un Document fake (pas persisté en BDD, juste pour l'event)
        Document fakeDocument = Document.builder()
                .id(UUID.randomUUID().toString())
                .originalName("facture-test.pdf")
                .contentType("application/pdf")
                .sizeBytes(123456L)
                .storageKey("2026/05/" + UUID.randomUUID() + ".pdf")
                .status(DocumentStatus.UPLOADED)
                .ownerId("user-test-001")
                .build();

        log.info("Publishing DocumentUploadedEvent for document {}", fakeDocument.getId());
        eventPublisher.publishEvent(new DocumentUploadedEvent(this, fakeDocument));

        return "[SUCCESS] DocumentUploadedEvent published for document " + fakeDocument.getId();
    }

    /** Simule un upload qui a échoué. */
    @GetMapping("/upload-failure")
    public String triggerUploadFailure() {
        String fakeDocumentId = UUID.randomUUID().toString();

        log.info("Publishing DocumentFailedEvent for document {}", fakeDocumentId);
        eventPublisher.publishEvent(new DocumentFailedEvent(
                this,
                AuditEventType.DOCUMENT_UPLOAD_FAILED,
                fakeDocumentId,
                "user-test-001",
                "MinIO storage is unavailable",
                "STG-003"
        ));

        return "[FAILURE] DocumentFailedEvent published for document " + fakeDocumentId;
    }
}
