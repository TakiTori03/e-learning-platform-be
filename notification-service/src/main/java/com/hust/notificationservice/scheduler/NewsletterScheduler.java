package com.hust.notificationservice.scheduler;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.hust.commonlibrary.dto.ApiResponse;
import com.hust.notificationservice.client.CourseClient;
import com.hust.notificationservice.entity.Subscription;
import com.hust.notificationservice.repository.SubscriptionRepository;
import com.hust.notificationservice.service.EmailService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class NewsletterScheduler {

    private final SubscriptionRepository subscriptionRepository;
    private final EmailService emailService;
    private final CourseClient courseClient;

    @Value("${app.frontend-url:https://e-learning-platform-web.vercel.app}")
    private String frontendUrl;

    // Cron job running at 8 AM on the 1st day of every month
    @Scheduled(cron = "0 0 8 1 * *")
    public void sendMonthlyNewsletter() {
        processAndSendNewsletter(Instant.now());
    }

    public void processAndSendNewsletter(Instant currentTime) {
        log.info("Starting monthly newsletter distribution job for time context: {}", currentTime);
        List<Subscription> activeSubscriptions = subscriptionRepository.findAllByIsActiveTrue();

        if (activeSubscriptions.isEmpty()) {
            log.info("No active subscriptions found. Skipping.");
            return;
        }

        List<CourseClient.CourseResponse> discountedCourses = null;
        try {
            ApiResponse<List<CourseClient.CourseResponse>> response = courseClient.getDiscountedCoursesLastMonth(currentTime);
            if (response != null && response.isSuccess()) {
                discountedCourses = response.getPayload();
            }
        } catch (Exception e) {
            log.error("Failed to fetch discounted courses", e);
        }

        if (discountedCourses == null || discountedCourses.isEmpty()) {
            log.info("No discounted courses found this month. Skipping newsletter.");
            return;
        }

        // Build structured course data for the Thymeleaf template
        java.text.NumberFormat priceFormatter = java.text.NumberFormat.getNumberInstance(new java.util.Locale("vi", "VN"));

        List<Map<String, Object>> courseDataList = new java.util.ArrayList<>();
        for (CourseClient.CourseResponse course : discountedCourses) {
            Map<String, Object> courseData = new HashMap<>();
            courseData.put("name", course.getName());
            courseData.put("thumbnail", course.getThumbnail());
            courseData.put("priceFormatted", priceFormatter.format(course.getPrice() != null ? course.getPrice() : 0) + " đ");
            courseData.put("finalPriceFormatted", priceFormatter.format(course.getFinalPrice() != null ? course.getFinalPrice() : 0) + " đ");
            courseData.put("url", frontendUrl + "/courses/" + course.getId());
            courseDataList.add(courseData);
        }

        List<String> toList = activeSubscriptions.stream()
                .map(Subscription::getEmail)
                .toList();

        Map<String, Object> templateModel = new HashMap<>();
        templateModel.put("courses", courseDataList);

        emailService.sendBulkHtmlEmails(
                toList,
                "🔥 Bản tin hàng tháng: Khóa học mới đang giảm giá!",
                "newsletter-template",
                templateModel
        );
        log.info("Newsletter bulk email dispatched for {} recipients with {} discounted courses.", toList.size(), courseDataList.size());
    }
}
