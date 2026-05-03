package com.ocrplatform.document.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Metriques metier custom exposees sur /actuator/prometheus.
 * <p>
 * Toutes les metriques sont prefixees {@code ocr.document.*} pour etre
 * regroupables dans Grafana.
 */
@Configuration
public class MetricsConfig {

    @Bean
    public Counter documentsUploadedCounter(MeterRegistry registry) {
        return Counter.builder("ocr.document.uploaded.total")
                .description("Total number of successful document uploads")
                .register(registry);
    }

    @Bean
    public Counter documentsFailedCounter(MeterRegistry registry) {
        return Counter.builder("ocr.document.upload.failed.total")
                .description("Total number of failed document uploads")
                .register(registry);
    }

    @Bean
    public Timer documentUploadTimer(MeterRegistry registry) {
        return Timer.builder("ocr.document.upload.duration")
                .description("Time taken to upload a document end-to-end")
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(registry);
    }

    @Bean
    public DistributionSummary documentSizeSummary(MeterRegistry registry) {
        return DistributionSummary.builder("ocr.document.size.bytes")
                .description("Distribution of uploaded document sizes")
                .baseUnit("bytes")
                .publishPercentiles(0.5, 0.95)
                .register(registry);
    }
}