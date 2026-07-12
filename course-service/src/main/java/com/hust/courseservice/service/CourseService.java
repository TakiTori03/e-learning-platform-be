package com.hust.courseservice.service;

import java.util.List;

import org.springframework.data.domain.Pageable;

import com.hust.commonlibrary.dto.ListResponse;
import com.hust.courseservice.dto.request.CourseRequest;
import com.hust.courseservice.dto.response.ActionLogResponse;
import com.hust.courseservice.dto.response.AdminCourseStatsResponse;
import com.hust.courseservice.dto.response.CourseResponse;
import com.hust.courseservice.entity.enums.CourseStatus;

public interface CourseService {
    AdminCourseStatsResponse getAdminCourseStats();
    // Standard CRUD (matching monolith admin/client)
    java.util.Map<String, Long> countByCategories(List<String> categoryIds);

    CourseResponse create(CourseRequest request);
    CourseResponse update(String id, CourseRequest request);
    void delete(List<String> ids);
    CourseResponse detail(String id);
    ListResponse<CourseResponse> search(
            String q,
            List<String> authors,
            List<String> topics,
            List<String> levels,
            List<String> prices,
            Double rating,
            CourseStatus status,
            Pageable pageable
    );
    ListResponse<CourseResponse> searchElasticsearch(
            String q,
            List<String> authors,
            List<String> topics,
            List<String> levels,
            List<String> prices,
            Double rating,
            Pageable pageable
    );
    List<String> suggestCourses(String q, int limit);

    // Specific Monolith Functionalities
    List<CourseResponse> getPopularCourses(int limit);
    List<CourseResponse> getTrendingCourses(int limit);
    List<CourseResponse> getRecommendedCourses(List<String> enrolledIds, int limit);
    List<CourseResponse> getRelatedCourses(String courseId, int limit);
//    CourseResponse getFullDetail(String id); // getCourseDetail
    void updateStatus(String id, CourseStatus status); // updateActiveStatusCourse
    List<CourseResponse> getAllActiveCourses();
    ListResponse<ActionLogResponse> getHistories(String id, int page, int limit);

    List<CourseResponse> getAllCoursesForInstructor();

    // Dedicated Admin Methods (Full privileges)
    void adminUpdateStatus(String id, CourseStatus status, String note);

    java.util.Map<String, Integer> getBulkViews(List<String> courseIds);

    List<CourseResponse> getBulkCourses(List<String> courseIds);

    CourseResponse linkFinalExam(String courseId, String finalExamId, String instructorId);

    List<CourseResponse> getDiscountedCourses(java.time.Instant currentTime);
}
