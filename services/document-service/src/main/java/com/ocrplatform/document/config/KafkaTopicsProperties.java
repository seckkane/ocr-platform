package com.ocrplatform.document.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Noms des topics Kafka, configurables par profile.
 */
@Configuration
@ConfigurationProperties(prefix = "ocr.kafka.topics")
@Data
public class KafkaTopicsProperties {
    private String documentUploaded;
}