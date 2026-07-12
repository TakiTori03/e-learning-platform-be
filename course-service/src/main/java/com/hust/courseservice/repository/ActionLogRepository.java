package com.hust.courseservice.repository;

import com.hust.courseservice.entity.ActionLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ActionLogRepository extends MongoRepository<ActionLog, String> {
    Page<ActionLog> findAllByCourseIdOrderByCreatedAtDesc(String courseId, Pageable pageable);
    
    List<ActionLog> findAllByCourseIdOrderByCreatedAtDesc(String courseId);
    
    void deleteAllByCourseIdIn(List<String> courseIds);
}
