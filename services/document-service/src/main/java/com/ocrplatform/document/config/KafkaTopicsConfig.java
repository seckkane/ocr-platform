package com.ocrplatform.document.config;

import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
@RequiredArgsConstructor
public class KafkaTopicsConfig {

    private final KafkaTopicsProperties topics;

    @Bean
    public NewTopic documentUploadedTopic() {
        return TopicBuilder.name(topics.getDocumentUploaded())
                .partitions(3)
                .replicas(1)
                .build();
    }
}
