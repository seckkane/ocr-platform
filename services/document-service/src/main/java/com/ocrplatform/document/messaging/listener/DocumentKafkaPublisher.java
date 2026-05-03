package com.ocrplatform.document.messaging.listener;

import com.ocrplatform.document.audit.event.DocumentUploadedEvent;
import com.ocrplatform.document.filter.CorrelationIdFilter;
import com.ocrplatform.document.messaging.event.DocumentUploadedKafkaEvent;
import com.ocrplatform.document.messaging.producer.DocumentKafkaProducer;
import com.ocrplatform.document.model.entity.Document;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Pont entre les events Spring locaux et Kafka.
 * <p>
 * Ecoute {@link DocumentUploadedEvent} et publie un {@link DocumentUploadedKafkaEvent}
 * sur le topic {@code documents.uploaded}.
 *
 * <h3>Pourquoi {@link TransactionalEventListener} avec AFTER_COMMIT ?</h3>
 * On ne veut publier sur Kafka <b>que si</b> la transaction metier (insert BDD) a reussi.
 * Si elle rollback, l'event Kafka <b>ne doit pas</b> partir : sinon les autres
 * microservices recoivent un event pour un document qui n'existe pas en BDD.
 * <p>
 * Difference avec l'audit : l'audit est best-effort meme en cas d'echec
 * ({@code @EventListener} simple), Kafka non (besoin du commit).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DocumentKafkaPublisher {

    private final DocumentKafkaProducer producer;

    @Async("auditExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onDocumentUploaded(DocumentUploadedEvent event) {
        // Le Document est dans la "source" de l'ApplicationEvent. On a defini
        // DocumentUploadedEvent avec source=this (DocumentServiceImpl), donc
        // on doit recuperer doc autrement. Mais on a stocke les details dans
        // event.getDetails(). On va passer par une approche plus propre :
        // recuperer doc via le resourceId.
        // -> Refactor leger ci-dessous.

        String documentId = event.getResourceId();
        String correlationId = MDC.get(CorrelationIdFilter.CORRELATION_ID_MDC_KEY);

        DocumentUploadedKafkaEvent kafkaEvent = new DocumentUploadedKafkaEvent(
                documentId,
                (String) event.getDetails().get("originalName"),
                (String) event.getDetails().get("contentType"),
                ((Number) event.getDetails().get("sizeBytes")).longValue(),
                "documents",
                (String) event.getDetails().get("storageKey"),
                event.getActorId(),
                event.getOccurredAt(),
                correlationId
        );

        log.debug("Bridging Spring event -> Kafka for document {}", documentId);
        producer.publishUploaded(kafkaEvent);
    }
}
