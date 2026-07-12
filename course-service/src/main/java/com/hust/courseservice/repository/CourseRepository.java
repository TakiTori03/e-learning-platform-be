package com.hust.courseservice.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.TextCriteria;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.hust.courseservice.dto.response.AuthorCourseReportResponse;
import com.hust.courseservice.dto.response.CourseInsightReportResponse;
import com.hust.courseservice.dto.response.AdminCourseStatsResponse;
import com.hust.courseservice.entity.Course;
import com.hust.courseservice.entity.enums.CourseStatus;

@Repository
public interface CourseRepository extends MongoRepository<Course, String> {

    boolean existsByCode(String code);

    @org.springframework.data.mongodb.repository.Query("{ 'status': 'PUBLISHED', $expr: { $lt: ['$finalPrice', '$price'] }, 'createdAt': { $gte: ?0, $lte: ?1 } }")
    List<Course> findDiscountedCoursesWithinTime(java.time.Instant startDate, java.time.Instant endDate);

    java.util.Optional<Course> findByCode(String code);

    Page<Course> findAllBy(TextCriteria criteria, Pageable pageable);

    List<Course> findAllByCategoryId(String categoryId);

    List<Course> findAllByCategoryIdAndStatus(String categoryId, CourseStatus status);

    Page<Course> findAllByCategoryIdAndStatus(String categoryId, CourseStatus status, Pageable pageable);

    long countByCategoryId(String categoryId);

    List<Course> findAllByInstructorId(String instructorId);

    List<Course> findAllByStatus(CourseStatus status);

    Page<Course> findAllByStatus(CourseStatus status, Pageable pageable);

    @org.springframework.data.mongodb.repository.Aggregation(pipeline = {
        "{ '$group': { " +
        "    '_id': null, " +
        "    'totalCourses': { $sum: 1 }, " +
        "    'totalActiveCourses': { $sum: { $cond: [ { $eq: [ '$status', 'PUBLISHED' ] }, 1, 0 ] } }, " +
        "    'totalDraftCourses': { $sum: { $cond: [ { $eq: [ '$status', 'DRAFT' ] }, 1, 0 ] } }, " +
        "    'totalViews': { $sum: '$views' }, " +
        "    'averageRating': { $avg: '$avgRatingStars' }, " +
        "    'totalReviews': { $sum: '$numOfReviews' } " +
        "} }"
    })
    List<CourseInsightReportResponse> getCourseInsightsAggregation();

    @org.springframework.data.mongodb.repository.Aggregation(pipeline = {
        "{ '$match': { 'instructorId': ?0 } }",
        "{ '$group': { " +
        "    '_id': '$instructorId', " +
        "    'totalCourses': { $sum: 1 }, " +
        "    'totalViews': { $sum: '$views' }, " +
        "    'averageRating': { $avg: '$avgRatingStars' }, " +
        "    'totalReviews': { $sum: '$numOfReviews' }, " +
        "    'studentCount': { $sum: '$studentCount' } " +
        "} }",
        "{ '$project': { 'instructorId': '$_id', 'totalCourses': 1, 'totalViews': 1, 'averageRating': 1, 'totalReviews': 1, 'studentCount': 1, '_id': 0 } }"
    })
    List<AuthorCourseReportResponse> getAuthorCourseReportAggregation(String instructorId);

    @org.springframework.data.mongodb.repository.Aggregation(pipeline = {
        "{ '$group': { " +
        "    '_id': null, " +
        "    'totalCourses': { $sum: 1 }, " +
        "    'publishedCourses': { $sum: { $cond: [ { $eq: [ '$status', 'PUBLISHED' ] }, 1, 0 ] } }, " +
        "    'pendingCourses': { $sum: { $cond: [ { $eq: [ '$status', 'PENDING' ] }, 1, 0 ] } }, " +
        "    'draftCourses': { $sum: { $cond: [ { $eq: [ '$status', 'DRAFT' ] }, 1, 0 ] } }, " +
        "    'rejectedCourses': { $sum: { $cond: [ { $eq: [ '$status', 'REJECTED' ] }, 1, 0 ] } }, " +
        "    'archivedCourses': { $sum: { $cond: [ { $eq: [ '$status', 'ARCHIVED' ] }, 1, 0 ] } } " +
        "} }"
    })
    List<AdminCourseStatsResponse> getAdminCourseStatsAggregation();

    @org.springframework.data.mongodb.repository.Aggregation(pipeline = {
        "{ '$match': { 'instructorId': ?0 } }",
        "{ '$group': { " +
        "    '_id': null, " +
        "    'totalCourses': { $sum: 1 }, " +
        "    'publishedCourses': { $sum: { $cond: [ { $eq: [ '$status', 'PUBLISHED' ] }, 1, 0 ] } }, " +
        "    'pendingCourses': { $sum: { $cond: [ { $eq: [ '$status', 'PENDING' ] }, 1, 0 ] } }, " +
        "    'draftCourses': { $sum: { $cond: [ { $eq: [ '$status', 'DRAFT' ] }, 1, 0 ] } }, " +
        "    'rejectedCourses': { $sum: { $cond: [ { $eq: [ '$status', 'REJECTED' ] }, 1, 0 ] } }, " +
        "    'archivedCourses': { $sum: { $cond: [ { $eq: [ '$status', 'ARCHIVED' ] }, 1, 0 ] } } " +
        "} }"
    })
    List<AdminCourseStatsResponse> getInstructorCourseStatsAggregation(String instructorId);
}
