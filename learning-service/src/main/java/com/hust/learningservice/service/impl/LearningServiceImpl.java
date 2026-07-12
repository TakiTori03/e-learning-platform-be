package com.hust.learningservice.service.impl;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hust.commonlibrary.constants.RedisPrefixConstants;
import com.hust.commonlibrary.dto.ApiResponse;
import com.hust.commonlibrary.dto.LessonInternalResponse;
import com.hust.commonlibrary.event.CourseEnrollmentUpdatedEvent;
import com.hust.commonlibrary.service.RedisService;
import com.hust.learningservice.client.CourseClient;
import com.hust.learningservice.dto.request.LessonProgressRequest;
import com.hust.learningservice.dto.response.CourseNoteResponse;
import com.hust.learningservice.dto.response.CourseProgressResponse;
import com.hust.learningservice.entity.CourseNote;
import com.hust.learningservice.entity.LessonProgress;
import com.hust.learningservice.entity.StudentEnrollment;
import com.hust.learningservice.mapper.LessonProgressMapper;
import com.hust.learningservice.mapper.NoteMapper;
import com.hust.learningservice.mapper.ProgressMapper;
import com.hust.learningservice.repository.CourseNoteRepository;
import com.hust.learningservice.repository.EnrollmentRepository;
import com.hust.learningservice.repository.ProgressRepository;
import com.hust.learningservice.service.LearningService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class LearningServiceImpl implements LearningService {

    private final EnrollmentRepository enrollmentRepository;
    private final ProgressRepository progressRepository;
    private final CourseNoteRepository noteRepository;
    private final CourseClient courseClient;
    private final NoteMapper noteMapper;
    private final ProgressMapper progressMapper;
    private final LessonProgressMapper lessonProgressMapper;
    private final ApplicationEventPublisher eventPublisher;
    private final com.hust.learningservice.service.GamificationService gamificationService;
    private final RedisService redisService;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    @Override
    @Transactional
    public void trackProgress(String userId, LessonProgressRequest request) {
        // TỐI ƯU HOÁ: Write-Behind Caching
        // Nếu chỉ là cập nhật thời gian xem (không đánh dấu hoàn thành), lưu vào Redis rồi trả về ngay lập tức (không chạm vào MongoDB).
        if (Boolean.FALSE.equals(request.getIsDone()) || request.getIsDone() == null) {
            String syncKey = RedisPrefixConstants.getLessonProgressSyncKey(userId, request.getCourseId(), request.getLessonId());
            if (request.getLastWatchedTime() != null) {
                redisService.set(syncKey, request, 60, TimeUnit.MINUTES);
                log.debug("⚡ Cached lastWatchedTime in Redis: {}", syncKey);
                return;
            }
        }

        // Bỏ key cache nếu người dùng đánh dấu hoàn thành bài học để MongoDB chiếm quyền đồng bộ
        redisService.delete(RedisPrefixConstants.getLessonProgressSyncKey(userId, request.getCourseId(), request.getLessonId()));

        LessonProgress progress = progressRepository.findByUserIdAndCourseIdAndLessonId(
                userId, request.getCourseId(), request.getLessonId())
                .orElseGet(() -> {
                    LessonProgress newProgress = lessonProgressMapper.requestToEntity(request);
                    newProgress.setUserId(userId);
                    newProgress.setIsDone(false);
                    return newProgress;
                });

        boolean wasDone = progress.getIsDone() != null && progress.getIsDone();

        // Sử dụng partialUpdate từ BaseMapper để cập nhật các trường từ request
        lessonProgressMapper.partialUpdate(progress, request);

        // [UX Tối ưu]: Nếu học viên xem xong video (isDone = true), reset thời gian xem về 0
        // Để lần sau nếu họ có mở lại bài học này để ôn tập, video sẽ tự động phát lại từ đầu (0:00)
        // thay vì nhảy ngay đến giây cuối cùng của video.
        if (Boolean.TRUE.equals(request.getIsDone())) {
            progress.setLastWatchedTime(0.0);
        }

        progressRepository.save(progress);

        recalculatePercentage(userId, request.getCourseId());

        boolean isDoneNow = progress.getIsDone() != null && progress.getIsDone();
        if (isDoneNow && !wasDone) {
            String lessonType = "VIDEO"; // Fallback default
            try {
                ApiResponse<LessonInternalResponse> lessonRes = courseClient.getLessonDetail(request.getLessonId());
                if (lessonRes != null && lessonRes.isSuccess() && lessonRes.getPayload() != null && lessonRes.getPayload().getType() != null) {
                    lessonType = lessonRes.getPayload().getType();
                }
            } catch (Exception e) {
                log.error("Failed to fetch lesson details to determine type: {}", e.getMessage());
            }
            gamificationService.processGamificationAndActivity(userId, lessonType);
        }
    }

    private void recalculatePercentage(String userId, String courseId) {
        StudentEnrollment enrollment = enrollmentRepository.findByUserIdAndCourseId(userId, courseId)
                .orElseThrow(() -> new RuntimeException("Student not enrolled in course"));

        long totalLessons = 0;
        try {
            ApiResponse<Long> countRes = courseClient.getLessonCount(courseId);
            if (countRes != null && countRes.isSuccess() && countRes.getPayload() != null) {
                totalLessons = countRes.getPayload();
            }
        } catch (Exception e) {
            log.error("Failed to fetch lesson count: {}", e.getMessage());
            return;
        }

        if (totalLessons == 0) return;

        List<LessonProgress> list = progressRepository.findByUserIdAndCourseId(userId, courseId);
        long finishedCount = list.stream()
                .filter(p -> p.getIsDone() != null && p.getIsDone())
                .count();

        double percentage = (double) finishedCount / totalLessons * 100;
        enrollment.setProgress(Math.min(100.0, percentage));

        boolean isNowCompleted = percentage >= 100.0;
        enrollment.setIsCompleted(isNowCompleted);
        if (isNowCompleted) {
            if (enrollment.getCompletedAt() == null) {
                enrollment.setCompletedAt(java.time.Instant.now());
            }
        } else {
            enrollment.setCompletedAt(null);
        }

        enrollmentRepository.save(enrollment);
    }

    @Override
    @Transactional(readOnly = true)
    public CourseProgressResponse getCourseProgress(String userId, String courseId) {
        Optional<StudentEnrollment> enrollmentOpt = enrollmentRepository.findByUserIdAndCourseId(userId, courseId);

        if (enrollmentOpt.isEmpty()) {
            return CourseProgressResponse.builder()
                    .userId(userId)
                    .courseId(courseId)
                    .isEnrolled(false)
                    .build();
        }

        List<LessonProgress> progressList = progressRepository.findByUserIdAndCourseId(userId, courseId);
        List<String> finishedLessonIds = progressList.stream()
                .filter(p -> p.getIsDone() != null && p.getIsDone())
                .map(LessonProgress::getLessonId)
                .toList();

        java.util.Map<String, Double> watchTimes = progressList.stream()
                .filter(p -> p.getLastWatchedTime() != null && p.getLastWatchedTime() > 0)
                .collect(java.util.stream.Collectors.toMap(LessonProgress::getLessonId, LessonProgress::getLastWatchedTime));

        // [Tối ưu Write-Behind]: Quét thêm Redis để lấy lastWatchedTime mới nhất chưa kịp Bulk Upsert
        try {
            String pattern = RedisPrefixConstants.getLessonProgressSyncKey(userId, courseId, "*");
            Set<String> cachedKeys = redisService.keys(pattern);
            if (cachedKeys != null && !cachedKeys.isEmpty()) {
                for (String fullKey : cachedKeys) {
                    int prefixIndex = fullKey.indexOf(RedisPrefixConstants.LOCAL_LESSON_PROGRESS_SYNC);
                    if (prefixIndex != -1) {
                        String internalKey = fullKey.substring(prefixIndex);
                        String[] parts = internalKey.split(":");
                        if (parts.length >= 5) {
                            String lessonId = parts[4];
                            Object cachedObj = redisService.get(internalKey);
                            if (cachedObj != null) {
                                LessonProgressRequest req;
                                if (cachedObj instanceof String) {
                                    req = objectMapper.readValue((String) cachedObj, LessonProgressRequest.class);
                                } else {
                                    req = objectMapper.convertValue(cachedObj, LessonProgressRequest.class);
                                }
                                if (req.getLastWatchedTime() != null && req.getLastWatchedTime() > 0) {
                                    watchTimes.put(lessonId, req.getLastWatchedTime());
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("⚠️ Lỗi khi đọc cache Redis trong getCourseProgress: {}", e.getMessage());
        }

        CourseProgressResponse response = progressMapper.entityToResponse(enrollmentOpt.get());
        response.setFinishedLessonIds(finishedLessonIds);
        response.setLessonWatchTimes(watchTimes);

        return response;
    }

    @Override
    @Transactional
    public void updateLastAccessedLesson(String userId, String courseId, String lessonId) {
        enrollmentRepository.findByUserIdAndCourseId(userId, courseId)
                .ifPresent(enrollment -> {
                    enrollment.setLastAccessedLessonId(lessonId);
                    enrollmentRepository.save(enrollment);
                    log.debug("Updated last accessed lesson to {} for user {} in course {}", lessonId, userId, courseId);
                });
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.Map<String, CourseProgressResponse> getCourseProgressBulk(String userId, List<String> courseIds) {
        List<StudentEnrollment> enrollments = enrollmentRepository.findAllByUserIdAndCourseIdIn(userId, courseIds);
        List<LessonProgress> allProgress = progressRepository.findAllByUserIdAndCourseIdIn(userId, courseIds);

        java.util.Map<String, List<LessonProgress>> progressByCourse = allProgress.stream()
                .collect(Collectors.groupingBy(LessonProgress::getCourseId));

        java.util.Map<String, CourseProgressResponse> resultMap = new java.util.HashMap<>();

        for (StudentEnrollment enrollment : enrollments) {
            CourseProgressResponse response = progressMapper.entityToResponse(enrollment);
            List<LessonProgress> courseProgress = progressByCourse.getOrDefault(enrollment.getCourseId(), List.of());

            List<String> finishedLessonIds = courseProgress.stream()
                    .filter(p -> Boolean.TRUE.equals(p.getIsDone()))
                    .map(LessonProgress::getLessonId)
                    .toList();

            response.setFinishedLessonIds(finishedLessonIds);
            resultMap.put(enrollment.getCourseId(), response);
        }

        // Đảm bảo những khóa học không có enrollment vẫn có response cơ bản (isEnrolled = false)
        courseIds.forEach(id -> resultMap.putIfAbsent(id, CourseProgressResponse.builder()
                .userId(userId)
                .courseId(id)
                .isEnrolled(false)
                .build()));

        return resultMap;
    }

    @Override
    @Transactional
    public void enrollStudentBulk(String userId, List<String> courseIds, String orderId) {
        if (courseIds == null || courseIds.isEmpty()) return;

        // Check idempotency: Nếu đã có enrollment cho Order này rồi thì bỏ qua việc save nhưng vẫn tiếp tục để Consumer bắn lại event
        if (orderId != null && enrollmentRepository.existsByUserIdAndOrderId(userId, orderId)) {
            log.info("Order {} already processed for user {}, enrollment records already exist", orderId, userId);
            return;
        }

        List<StudentEnrollment> existing = enrollmentRepository.findAllByUserIdAndCourseIdIn(userId, courseIds);
        Set<String> existingCourseIds = existing.stream()
                .map(StudentEnrollment::getCourseId)
                .collect(Collectors.toSet());

        List<StudentEnrollment> newEnrollments = courseIds.stream()
                .filter(id -> !existingCourseIds.contains(id))
                .distinct()
                .map(courseId -> StudentEnrollment.builder()
                        .userId(userId)
                        .courseId(courseId)
                        .orderId(orderId)
                        .progress(0.0)
                        .isCompleted(false)
                        .build())
                .collect(Collectors.toList());

        if (!newEnrollments.isEmpty()) {
            enrollmentRepository.saveAll(newEnrollments);
            log.info("Bulk enrolled user {} in {} new courses for Order {}", userId, newEnrollments.size(), orderId);

            // Phát sự kiện cập nhật số lượng học viên lên Kafka cho từng khóa học mới đăng ký
            for (StudentEnrollment enrollment : newEnrollments) {
                publishEnrollmentStats(enrollment.getCourseId());
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasAccess(String userId, String courseId) {
        return enrollmentRepository.existsByUserIdAndCourseId(userId, courseId);
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.Map<String, Boolean> checkAccessBulk(String userId, List<String> courseIds) {
        if (courseIds == null || courseIds.isEmpty()) {
            return java.util.Collections.emptyMap();
        }
        List<StudentEnrollment> enrollments = enrollmentRepository.findAllByUserIdAndCourseIdIn(userId, courseIds);
        Set<String> enrolledCourseIds = enrollments.stream()
                .map(StudentEnrollment::getCourseId)
                .collect(Collectors.toSet());

        java.util.Map<String, Boolean> result = new java.util.HashMap<>();
        for (String courseId : courseIds) {
            result.put(courseId, enrolledCourseIds.contains(courseId));
        }
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public List<StudentEnrollment> getEnrolledCourses(String userId) {
        return enrollmentRepository.findByUserId(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean checkLessonAccess(String userId, String lessonId) {
        ApiResponse<LessonInternalResponse> lessonRes = courseClient.getLessonDetail(lessonId);
        if (lessonRes == null || !lessonRes.isSuccess() || lessonRes.getPayload() == null) {
            return false;
        }

        LessonInternalResponse lesson = lessonRes.getPayload();

        return enrollmentRepository.existsByUserIdAndCourseId(userId, lesson.getCourseId());
    }

    @Override
    @Transactional(readOnly = true)
    public List<CourseNoteResponse> getMyNotes(String userId, String courseId) {
        List<CourseNote> notes = noteRepository.findAllByUserIdAndCourseId(userId, courseId);
        return noteMapper.entityToResponse(notes);
    }

    @Override
    @Transactional
    public void addNote(String userId, com.hust.learningservice.dto.request.CourseNoteRequest request) {
        CourseNote note = noteMapper.requestToEntity(request);
        note.setUserId(userId);
        noteRepository.save(note);
    }

    @Override
    @Transactional
    public void deleteNote(String noteId) {
        noteRepository.deleteById(noteId);
    }

    @Override
    @Transactional
    public void completeQuizLesson(String userId, String lessonId) {
        log.info("Initializing automated completion sequence for Lesson Quiz: {}", lessonId);

        // 1. Call Feign client to determine which Course owns this Quiz Lesson
        ApiResponse<LessonInternalResponse> lessonRes = courseClient.getLessonDetail(lessonId);
        if (lessonRes == null || !lessonRes.isSuccess() || lessonRes.getPayload() == null) {
            log.error("CRITICAL SYNC ERROR: Could not resolve Course ID for Lesson {} via Feign. Aborting progress sync.", lessonId);
            return;
        }

        String courseId = lessonRes.getPayload().getCourseId();
        log.info("Resolved Quiz Lesson {} to Parent Course {}. Mapping completion event...", lessonId, courseId);

        // 2. Map to core LessonProgressRequest to reuse calculation engine
        LessonProgressRequest syncRequest = LessonProgressRequest.builder()
                .courseId(courseId)
                .lessonId(lessonId)
                .isDone(true)
                .build();

        // 3. Invoke core track progress to save record and trigger overall course percent recalculation!
        trackProgress(userId, syncRequest);
    }

    /**
     * Đếm tổng số học viên đã đăng ký khóa học rồi phát sự kiện Spring local.
     * EnrollmentEventListener sẽ lắng nghe sự kiện này và gửi lên Kafka sau khi commit transaction.
     */
    private void publishEnrollmentStats(String courseId) {
        try {
            long count = enrollmentRepository.countByCourseId(courseId);

            CourseEnrollmentUpdatedEvent event = CourseEnrollmentUpdatedEvent.builder()
                    .courseId(courseId)
                    .studentCount(count)
                    .build();

            eventPublisher.publishEvent(event);
            log.info("📤 Published local CourseEnrollmentUpdatedEvent: courseId={}, studentCount={}", courseId, count);
        } catch (Exception e) {
            log.error("⚠️ Failed to publish local enrollment stats for course {}: {}", courseId, e.getMessage());
        }
    }

    @Override
    public List<com.hust.learningservice.dto.response.EnrolledCourseSelectResponse> getEnrolledCoursesSelect(String userId, String q) {
        log.info("Fetching enrolled courses select for userId={}, query={}", userId, q);
        List<StudentEnrollment> enrollments = enrollmentRepository.findByUserId(userId);
        if (enrollments == null || enrollments.isEmpty()) {
            return java.util.Collections.emptyList();
        }

        List<String> courseIds = enrollments.stream()
                .map(StudentEnrollment::getCourseId)
                .distinct()
                .collect(Collectors.toList());

        try {
            ApiResponse<List<com.hust.commonlibrary.dto.CourseInternalResponse>> coursesRes = courseClient.getBulkCourses(courseIds);
            if (coursesRes != null && coursesRes.isSuccess() && coursesRes.getPayload() != null) {
                return coursesRes.getPayload().stream()
                        .filter(course -> q == null || q.trim().isEmpty() || course.getName().toLowerCase().contains(q.trim().toLowerCase()))
                        .map(course -> com.hust.learningservice.dto.response.EnrolledCourseSelectResponse.builder()
                                .id(course.getId())
                                .name(course.getName())
                                .build())
                        .collect(Collectors.toList());
            }
        } catch (Exception e) {
            log.error("Failed to fetch bulk course details for courseIds={}: {}", courseIds, e.getMessage());
        }

        return java.util.Collections.emptyList();
    }
}
