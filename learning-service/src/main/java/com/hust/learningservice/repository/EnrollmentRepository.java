package com.hust.learningservice.repository;

import com.hust.learningservice.entity.StudentEnrollment;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;
import com.hust.learningservice.dto.response.CourseProgressReportResponse;

@Repository
public interface EnrollmentRepository extends MongoRepository<StudentEnrollment, String> {
    Optional<StudentEnrollment> findByUserIdAndCourseId(String userId, String courseId);
    List<StudentEnrollment> findByUserId(String userId);
    List<StudentEnrollment> findAllByUserIdAndCourseIdIn(String userId, List<String> courseIds);
    boolean existsByUserIdAndCourseId(String userId, String courseId);
    boolean existsByUserIdAndOrderId(String userId, String orderId);

    @org.springframework.data.mongodb.repository.Aggregation(pipeline = {
        "{ '$group': { " +
        "    '_id': '$courseId', " +
        "    'averageProgress': { $avg: '$progress' }, " +
        "    'totalEnrollments': { $sum: 1 }, " +
        "    'completedEnrollments': { $sum: { $cond: [ { $eq: [ '$isCompleted', true ] }, 1, 0 ] } } " +
        "} }",
        "{ '$project': { 'courseId': '$_id', 'averageProgress': 1, 'totalEnrollments': 1, 'completedEnrollments': 1, '_id': 0 } }"
    })
    List<CourseProgressReportResponse> getUsersProgressAggregation();

    long countByCourseId(String courseId);
}
