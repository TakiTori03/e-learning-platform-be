package com.hust.orderservice.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

import com.hust.commonlibrary.constants.KafkaTopics;

@Configuration
public class KafkaProducerConfig {

    @Bean
    public NewTopic orderPaidTopic() {
        return TopicBuilder.name(KafkaTopics.ORDER_PAID)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic enrollmentSuccessTopic() {
        return TopicBuilder.name(KafkaTopics.ENROLLMENT_SUCCESS)
                .partitions(3)
                .replicas(1)
                .build();
    }
}
