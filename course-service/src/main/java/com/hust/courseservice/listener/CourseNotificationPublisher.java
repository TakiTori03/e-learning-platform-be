package com.hust.courseservice.listener;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.hust.commonlibrary.constants.AppConstants;
import com.hust.commonlibrary.constants.FrontendRoutes;
import com.hust.commonlibrary.enums.NotificationType;
import com.hust.commonlibrary.event.NotificationEvent;
import com.hust.courseservice.entity.enums.CourseStatus;
import com.hust.courseservice.event.CourseStatusChangedLocalEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class CourseNotificationPublisher {
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCourseStatusChanged(CourseStatusChangedLocalEvent event) {
        CourseStatus action = event.getAction();
        String courseName = event.getCourse().getName();

        NotificationEvent notif = new NotificationEvent();

        if (action == CourseStatus.PENDING) {
            notif.setUserId(AppConstants.Notification_Constants.ALL_ADMIN_TOPIC);
            notif.setTitle("Khóa học chờ duyệt");
            notif.setMessage(String.format("Khóa học '%s' vừa được gửi yêu cầu phê duyệt.", courseName));
            notif.setType(NotificationType.ADMIN_ALERT);
            notif.setRedirectUrl(FrontendRoutes.ADMIN_PENDING_COURSES);
        } else if (action == CourseStatus.PUBLISHED) {
            notif.setUserId(event.getCourse().getInstructorId());
            notif.setTitle("Khóa học đã được xuất bản");
            notif.setMessage(String.format("Chúc mừng! Khóa học '%s' của bạn đã được admin duyệt và xuất bản.", courseName));
            notif.setType(NotificationType.COURSE_UPDATE);
            notif.setRedirectUrl(FrontendRoutes.INSTRUCTOR_COURSES);
        } else if (action == CourseStatus.REJECTED) {
            notif.setUserId(event.getCourse().getInstructorId());
            notif.setTitle("Khóa học bị từ chối");
            notif.setMessage(String.format("Khóa học '%s' của bạn đã bị từ chối xuất bản. Vui lòng kiểm tra lại.", courseName));
            notif.setType(NotificationType.COURSE_UPDATE);
            notif.setRedirectUrl(FrontendRoutes.INSTRUCTOR_COURSES);
        }

        kafkaTemplate.send("notification-events-topic", notif);
        log.info("Sent course notification event to Kafka for course {}", courseName);
    }
}
