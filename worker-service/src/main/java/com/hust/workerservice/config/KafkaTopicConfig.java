package com.hust.workerservice.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

import com.hust.commonlibrary.constants.KafkaTopics;

/**
 * Cấu hình tự động khởi tạo các Kafka Topic trên Broker nếu chưa tồn tại.
 * Giúp tránh lỗi UNKNOWN_TOPIC_OR_PART ở phía Consumer (Python) khi khởi chạy hệ thống lần đầu.
 */
@Configuration
public class KafkaTopicConfig {

    @Bean
    public NewTopic sttRequestsTopic() {
        return TopicBuilder.name(KafkaTopics.STT_REQUESTS)
                .partitions(1)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic sttResultsTopic() {
        return TopicBuilder.name(KafkaTopics.STT_RESULTS)
                .partitions(1)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic pdfParserRequestsTopic() {
        return TopicBuilder.name(KafkaTopics.PDF_PARSER_REQUESTS)
                .partitions(1)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic pdfParserResultsTopic() {
        return TopicBuilder.name(KafkaTopics.PDF_PARSER_RESULTS)
                .partitions(1)
                .replicas(1)
                .build();
    }
}
