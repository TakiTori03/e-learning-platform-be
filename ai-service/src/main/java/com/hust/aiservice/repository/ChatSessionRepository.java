package com.hust.aiservice.repository;

import com.hust.aiservice.entity.ChatSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ChatSessionRepository extends JpaRepository<ChatSession, String> {
    List<ChatSession> findByUserIdAndCourseIdOrderByUpdatedAtDesc(String userId, String courseId);

    @org.springframework.data.jpa.repository.Query("SELECT s FROM ChatSession s WHERE s.userId = :userId AND (:courseId IS NULL OR s.courseId = :courseId) ORDER BY s.updatedAt DESC")
    List<ChatSession> getSessions(@org.springframework.data.repository.query.Param("userId") String userId, @org.springframework.data.repository.query.Param("courseId") String courseId);
}
