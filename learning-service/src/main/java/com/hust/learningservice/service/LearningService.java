package com.hust.learningservice.service;

import com.hust.learningservice.dto.request.CourseNoteRequest;
import com.hust.learningservice.dto.request.LessonProgressRequest;
import com.hust.learningservice.dto.response.CourseNoteResponse;
import com.hust.learningservice.dto.response.CourseProgressResponse;

import com.hust.learningservice.entity.StudentEnrollment;
import java.util.List;
import java.util.Map;

public interface LearningService {
    void trackProgress(String userId, LessonProgressRequest request);
    CourseProgressResponse getCourseProgress(String userId, String courseId);
    Map<String, CourseProgressResponse> getCourseProgressBulk(String userId, List<String> courseIds);
    void updateLastAccessedLesson(String userId, String courseId, String lessonId);
    void enrollStudentBulk(String userId, List<String> courseIds, String orderId);
    boolean hasAccess(String userId, String courseId);
    Map<String, Boolean> checkAccessBulk(String userId, List<String> courseIds);
    boolean checkLessonAccess(String userId, String lessonId);
    List<StudentEnrollment> getEnrolledCourses(String userId);
    List<com.hust.learningservice.dto.response.EnrolledCourseSelectResponse> getEnrolledCoursesSelect(String userId, String q);
    
    // Note Management
    List<CourseNoteResponse> getMyNotes(String userId, String courseId);
    void addNote(String userId, CourseNoteRequest request);
    void deleteNote(String noteId);

    // Assessment Sync
    void completeQuizLesson(String userId, String lessonId);
}
