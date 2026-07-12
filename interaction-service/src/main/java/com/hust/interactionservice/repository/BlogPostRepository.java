package com.hust.interactionservice.repository;

import com.hust.interactionservice.entity.BlogPost;
import com.hust.interactionservice.entity.enums.BlogStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BlogPostRepository extends MongoRepository<BlogPost, String> {

    Optional<BlogPost> findBySlug(String slug);

    boolean existsBySlug(String slug);

    // Guest / Student: View published blogs
    Page<BlogPost> findByStatus(BlogStatus status, Pageable pageable);

    Page<BlogPost> findByStatusAndTopicId(BlogStatus status, String topicId, Pageable pageable);

    @Query("{ 'status': ?0, 'topic.slug': ?1 }")
    Page<BlogPost> findByStatusAndTopicSlug(BlogStatus status, String topicSlug, Pageable pageable);

    // Instructor: view own blogs
    Page<BlogPost> findByCreatedBy(String createdBy, Pageable pageable);

    Page<BlogPost> findByCreatedByAndStatus(String createdBy, BlogStatus status, Pageable pageable);

    Page<BlogPost> findByCreatedByAndTopicId(String createdBy, String topicId, Pageable pageable);

    Page<BlogPost> findByCreatedByAndStatusAndTopicId(String createdBy, BlogStatus status, String topicId, Pageable pageable);
}
