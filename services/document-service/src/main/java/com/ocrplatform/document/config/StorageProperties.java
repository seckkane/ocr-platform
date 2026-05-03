package com.ocrplatform.document.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration du stockage objet (MinIO ou S3-compatible).
 * Bindé sur le préfixe {@code storage.*} dans application.yml.
 */
@Configuration
@ConfigurationProperties(prefix = "storage")
@Data
public class StorageProperties {
    private String endpoint;
    private String accessKey;
    private String secretKey;
    private String bucket;
}