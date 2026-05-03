package com.ocrplatform.document;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.containers.MinIOContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import com.ocrplatform.document.config.TestSecurityConfig;
import org.springframework.context.annotation.Import;

/**
 * Classe de base pour les tests d'integration.
 * <p>
 * Demarre 3 containers via Testcontainers, partages entre tous les tests
 * qui heritent de cette classe (statiques + reuse).
 */

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
@Import(TestSecurityConfig.class)
public abstract class AbstractIntegrationTest {

    static final MySQLContainer<?> MYSQL = new MySQLContainer<>(
            DockerImageName.parse("mysql:8.4"))
            .withDatabaseName("documents_db")
            .withUsername("test")
            .withPassword("test")
            .withReuse(true);

    static final MinIOContainer MINIO = new MinIOContainer(
            DockerImageName.parse("minio/minio:latest"))
            .withUserName("minioadmin")
            .withPassword("minioadmin123")
            .withReuse(true);

    static final KafkaContainer KAFKA = new KafkaContainer(
            DockerImageName.parse("apache/kafka:3.8.0"));

    static {
        MYSQL.start();
        MINIO.start();
        KAFKA.start();
    }

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry r) {
        // MySQL
        r.add("spring.datasource.url", MYSQL::getJdbcUrl);
        r.add("spring.datasource.username", MYSQL::getUsername);
        r.add("spring.datasource.password", MYSQL::getPassword);
        r.add("spring.datasource.driver-class-name", MYSQL::getDriverClassName);

        // MinIO
        r.add("storage.endpoint", () -> "http://" + MINIO.getHost() + ":" + MINIO.getFirstMappedPort());
        r.add("storage.access-key", () -> "minioadmin");
        r.add("storage.secret-key", () -> "minioadmin123");
        r.add("storage.bucket", () -> "documents-test");

        // Kafka
        r.add("spring.kafka.bootstrap-servers", KAFKA::getBootstrapServers);
    }
}