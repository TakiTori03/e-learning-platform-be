package com.hust.courseservice.consumer;

import com.hust.commonlibrary.constants.KafkaTopics;
import com.hust.commonlibrary.event.LessonMediaReadyEvent;
import com.hust.commonlibrary.event.NotificationEvent;
import com.hust.courseservice.entity.Course;
import com.hust.courseservice.entity.Lesson;
import com.hust.courseservice.entity.enums.LessonType;
import com.hust.courseservice.event.CurriculumMutatedEvent;
import com.hust.courseservice.repository.CourseRepository;
import com.hust.courseservice.repository.LessonRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class LessonMediaReadyConsumer {

    private final LessonRepository lessonRepository;
    private final CourseRepository courseRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @KafkaListener(topics = KafkaTopics.LESSON_MEDIA_READY, groupId = "course-service-lesson-group")
    public void consume(LessonMediaReadyEvent event) {
        log.info("🎯 [course-service] Nhận sự kiện LESSON_MEDIA_READY cho lesson: {}", event.getLessonId());

        try {
            Optional<Lesson> lessonOpt = lessonRepository.findById(event.getLessonId());
            if (lessonOpt.isPresent()) {
                Lesson lesson = lessonOpt.get();

                boolean isSuccess = (event.getFileSize() != null && event.getFileSize() != -1L);
                boolean updated = false;

                if (isSuccess) {
                    if (event.getUrl() != null && !event.getUrl().isEmpty()) {
                        lesson.setContent(event.getUrl());
                        updated = true;
                    }

                    if (event.getDuration() != null) {
                        lesson.setVideoLength(event.getDuration());
                        updated = true;
                    }

                    if (event.getTranscriptUrl() != null && !event.getTranscriptUrl().isEmpty()) {
                        lesson.setTranscriptUrl(event.getTranscriptUrl());
                        updated = true;
                    }
                }

                if (updated) {
                    lessonRepository.save(lesson);
                    log.info("✅ Đã cập nhật content và videoLength cho lesson: {}", event.getLessonId());

                    // 🧹 EVICT CACHE: Phát sự kiện để xóa cache Course Detail cũ, đồng bộ hiển thị video mới cho học viên
                    if (lesson.getCourseId() != null) {
                        eventPublisher.publishEvent(new CurriculumMutatedEvent(lesson.getCourseId()));
                        log.info("🧹 Đã phát CurriculumMutatedEvent để evict cache cho courseId: {}", lesson.getCourseId());
                    }
                }

                // Gửi Notification cho giảng viên
                sendNotification(lesson, isSuccess);

            } else {
                log.warn("⚠️ Không tìm thấy lesson với id: {}", event.getLessonId());
            }
        } catch (Exception e) {
            log.error("❌ Lỗi khi cập nhật lesson từ sự kiện LESSON_MEDIA_READY: {}", e.getMessage(), e);
        }
    }

    private void sendNotification(Lesson lesson, boolean isSuccess) {
        if (lesson.getCourseId() == null) return;
        
        Optional<Course> courseOpt = courseRepository.findById(lesson.getCourseId());
        if (courseOpt.isEmpty()) return;
        
        Course course = courseOpt.get();
        String instructorId = course.getInstructorId();
        if (instructorId == null) return;

        boolean isVideo = lesson.getType() == LessonType.VIDEO;
        String typeName = isVideo ? "Video" : "Tài liệu PDF";

        String title = isSuccess ? "Xử lý " + typeName + " thành công" : "Xử lý " + typeName + " thất bại";
        String message = isSuccess 
                ? String.format("%s bài giảng trong khóa học '%s' đã được xử lý và sẵn sàng hiển thị.", typeName, course.getName())
                : String.format("%s bài giảng trong khóa học '%s' xử lý thất bại.", typeName, course.getName());

        NotificationEvent notificationEvent = NotificationEvent.builder()
                .userId(instructorId)
                .title(title)
                .message(message)
                .type(com.hust.commonlibrary.enums.NotificationType.ASYNC_TASK)
                .build();

        kafkaTemplate.send(KafkaTopics.NOTIFICATION_EVENTS, notificationEvent);
        log.info("📢 Đã gửi NotificationEvent qua Kafka cho notification-service (Media Ready, user: {})", instructorId);
    }
}
