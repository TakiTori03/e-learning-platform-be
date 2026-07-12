package com.hust.learningservice.repository;

import com.hust.learningservice.entity.Discussion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DiscussionRepository extends MongoRepository<Discussion, String> {
    Page<Discussion> findByLessonIdAndParentIdIsNull(String lessonId, Pageable pageable);
    Page<Discussion> findBySectionIdAndParentIdIsNull(String sectionId, Pageable pageable);
    
    List<Discussion> findByLessonId(String lessonId);
    List<Discussion> findBySectionId(String sectionId);
    List<Discussion> findByParentId(String parentId);
    List<Discussion> findByRootId(String rootId);
    List<Discussion> findByRootIdIn(List<String> rootIds);
    long countByParentId(String parentId);
    void deleteByRootId(String rootId);
    void deleteByParentId(String parentId);
}
