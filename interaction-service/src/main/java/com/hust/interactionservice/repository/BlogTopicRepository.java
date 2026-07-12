package com.hust.interactionservice.repository;

import com.hust.interactionservice.entity.BlogTopic;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.Optional;

public interface BlogTopicRepository extends MongoRepository<BlogTopic, String> {
    Optional<BlogTopic> findBySlug(String slug);
    boolean existsBySlug(String slug);
    boolean existsByName(String name);
}
