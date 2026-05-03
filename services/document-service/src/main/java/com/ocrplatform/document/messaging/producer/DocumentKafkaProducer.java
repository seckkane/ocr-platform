package com.ocrplatform.document.messaging.producer;

import com.ocrplatform.document.config.KafkaTopicsProperties;
import com.ocrplatform.document.messaging.event.DocumentUploadedKafkaEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/**
 * Producer Kafka pour les events publics du document-service.
 * <p>
 * Patterns :
 * <ul>
 *     <li>Cle = documentId : garantit l'ordre par document (meme partition)</li>
 *     <li>{@link CompletableFuture} : ne bloque pas le thread appelant</li>
 *     <li>Callback : log succes/echec</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DocumentKafkaProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final KafkaTopicsProperties topics;

    public void publishUploaded(DocumentUploadedKafkaEvent event) {
        String topic = topics.getDocumentUploaded();
        String key = event.documentId();

        CompletableFuture<?> future = kafkaTemplate.send(topic, key, event);

        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Failed to publish to {} (key={}): {}", topic, key, ex.getMessage(), ex);
            } else {
                log.debug("Published to {} (key={})", topic, key);
            }
        });
    }
}