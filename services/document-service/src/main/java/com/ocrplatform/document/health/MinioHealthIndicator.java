package com.ocrplatform.document.health;

import com.ocrplatform.document.config.StorageProperties;
import io.minio.BucketExistsArgs;
import io.minio.MinioClient;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Health check custom pour MinIO.
 * Expose l'etat sous {@code /actuator/health/minio}.
 * <p>
 * Le bean s'appelle "minio", donc le sous-chemin est /actuator/health/minio.
 */
@Component("minio")
@RequiredArgsConstructor
public class MinioHealthIndicator implements HealthIndicator {

    private final MinioClient minioClient;
    private final StorageProperties props;

    @Override
    public Health health() {
        try {
            boolean exists = minioClient.bucketExists(
                    BucketExistsArgs.builder().bucket(props.getBucket()).build());
            if (exists) {
                return Health.up()
                        .withDetail("endpoint", props.getEndpoint())
                        .withDetail("bucket", props.getBucket())
                        .build();
            }
            return Health.down()
                    .withDetail("endpoint", props.getEndpoint())
                    .withDetail("bucket", props.getBucket())
                    .withDetail("reason", "Bucket does not exist")
                    .build();
        } catch (Exception e) {
            return Health.down()
                    .withDetail("endpoint", props.getEndpoint())
                    .withDetail("error", e.getMessage())
                    .build();
        }
    }
}
