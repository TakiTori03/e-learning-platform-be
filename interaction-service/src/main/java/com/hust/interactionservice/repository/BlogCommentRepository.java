package com.hust.interactionservice.repository;

import com.hust.interactionservice.entity.BlogComment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BlogCommentRepository extends MongoRepository<BlogComment, String> {

    Page<BlogComment> findByBlogIdAndParentCommentIdIsNull(String blogId, Pageable pageable);

    List<BlogComment> findByParentCommentIdIn(List<String> parentCommentIds);

    long countByBlogId(String blogId);

    void deleteByBlogId(String blogId);
}
