package com.ocrplatform.document.service;

import com.ocrplatform.document.config.StorageProperties;
import com.ocrplatform.document.exception.ErrorCode;
import com.ocrplatform.document.exception.StorageException;
import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * Implémentation MinIO du {@link StorageService}.
 * <p>
 * Convention de clé : {@code yyyy/MM/dd/<uuid>.<ext>} pour partitionner naturellement
 * par date (utile pour archivage / cleanup futur).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StorageServiceImpl implements StorageService {

    private static final DateTimeFormatter DATE_PREFIX = DateTimeFormatter.ofPattern("yyyy/MM/dd");

    private final MinioClient minioClient;
    private final StorageProperties props;


    /**
     * Crée le bucket au démarrage s'il n'existe pas.
     * Idempotent : safe à exécuter à chaque démarrage.
     */
    @PostConstruct
    // Exécute cette méthode automatiquement après l’initialisation du bean
    // Vérifier que le bucket MinIO existe
    // Bucket = dossier principal dans MinIO
    void ensureBucketExists() {
        try {
            boolean exists = minioClient.bucketExists(
                    BucketExistsArgs.builder().bucket(props.getBucket()).build());
            if (!exists) {
                minioClient.makeBucket(
                        MakeBucketArgs.builder().bucket(props.getBucket()).build());
                log.info("Created MinIO bucket '{}'", props.getBucket());
            } else {
                log.info("MinIO bucket '{}' already exists", props.getBucket());
            }
        } catch (Exception e) {
            log.error("Failed to ensure MinIO bucket exists: {}", e.getMessage(), e);
            // ensureBucketExists()
            throw new StorageException(ErrorCode.STORAGE_UNAVAILABLE, "MinIO unavailable at startup")
                    .withDetail("endpoint", props.getEndpoint());
        }
    }

    @Override
    public String store(InputStream inputStream, String originalFilename, String contentType, long sizeBytes) {
        String storageKey = generateKey(originalFilename);
        try {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(props.getBucket())
                    .object(storageKey)
                    .stream(inputStream, sizeBytes, -1)
                    .contentType(contentType)
                    .build());
            log.debug("Stored object: bucket={}, key={}, size={}", props.getBucket(), storageKey, sizeBytes);
            return storageKey;
        } catch (Exception e) {
            log.error("Failed to store object {}: {}", storageKey, e.getMessage(), e);
            // delete()
            throw new StorageException(ErrorCode.STORAGE_UPLOAD_FAILED, "Failed to delete file")
                    .withDetail("storageKey", storageKey)
                    .withDetail("reason", e.getMessage());
        }
    }

    @Override
    public InputStream retrieve(String storageKey) {
        try {
            return minioClient.getObject(GetObjectArgs.builder()
                    .bucket(props.getBucket())
                    .object(storageKey)
                    .build());
        } catch (Exception e) {
            log.error("Failed to retrieve object {}: {}", storageKey, e.getMessage(), e);
            // store()
            throw new StorageException(ErrorCode.STORAGE_UPLOAD_FAILED, "Failed to store file")
                    .withDetail("storageKey", storageKey);
        }
    }

    @Override
    public void delete(String storageKey) {
        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(props.getBucket())
                    .object(storageKey)
                    .build());
            log.debug("Deleted object: key={}", storageKey);
        } catch (Exception e) {
            log.error("Failed to delete object {}: {}", storageKey, e.getMessage(), e);
            // retrieve()
            throw new StorageException(ErrorCode.STORAGE_DOWNLOAD_FAILED, "Failed to retrieve file")
                    .withDetail("storageKey", storageKey);
        }
    }

    /**
     * Génère une clé unique au format {@code yyyy/MM/dd/<uuid>.<ext>}.
     */
    private String generateKey(String originalFilename) {
        String datePrefix = LocalDate.now().format(DATE_PREFIX);
        String extension = extractExtension(originalFilename);
        return "%s/%s%s".formatted(datePrefix, UUID.randomUUID(), extension);
    }

    private String extractExtension(String filename) {
        if (filename == null) return "";
        int dot = filename.lastIndexOf('.');
        return dot >= 0 ? filename.substring(dot) : "";
    }
}








