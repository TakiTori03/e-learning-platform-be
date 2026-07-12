package com.hust.commonlibrary.constants;

/**
 * Centralized Registry of all Apache Kafka Topic Names used across the microservices ecosystem.
 * Consolidating topics here prevents typo-driven producer/consumer mismatches.
 */
public final class KafkaTopics {

    private KafkaTopics() {
        // Prevent instantiation
    }

    /**
     * Triggered when an Order is successfully paid in full.
     * Producer: order-service
     * Consumer: learning-service
     */
    public static final String ORDER_PAID = "order-paid-topic";

    /**
     * Callback triggered to signify successful enrollment fulfillment for the Saga pattern.
     * Producer: learning-service
     * Consumer: order-service
     */
    public static final String ENROLLMENT_SUCCESS = "enrollment-success-topic";

    /**
     * Triggered when a Lesson's material/content is updated.
     * Producer: course-service
     * Consumer: learning-service
     */
    public static final String LESSON_MATERIAL_UPDATED = "lesson-material-updated-topic";

    /**
     * Triggered when a student submits an assignment or completes an assessment.
     * Producer: assessment-service
     * Consumer: learning-service
     */
    public static final String ASSESSMENT_SUBMITTED = "assessment-submitted-topic";

    /**
     * Triggered when raw text has been successfully extracted from media/documents.
     * Producer: worker-service
     * Consumer: ai-service
     */
    public static final String RAW_TEXT_INGESTED = "raw-text-ingested-topic";

    /**
     * Triggered to request processing (transcoding/OCR) of a media asset.
     * Producer: media-service
     * Consumer: worker-service
     */
    public static final String MEDIA_PROCESSING = "media-processing-topic";

    /**
     * Triggered when a media asset is successfully processed and ready for streaming/reading.
     * Producer: worker-service
     * Consumer: media-service (cập nhật trạng thái Media entity)
     * Consumer: course-service (cập nhật content URL + videoLength vào Lesson entity)
     */
    public static final String LESSON_MEDIA_READY = "lesson-media-ready-topic";

    /**
     * Triggered when review statistics (avg rating, count) change for a course.
     * Producer: interaction-service
     * Consumer: course-service
     */
    public static final String COURSE_REVIEW_UPDATED = "course-review-updated-topic";

    /**
     * Triggered when enrollment count changes for a course.
     * Producer: learning-service
     * Consumer: course-service
     */
    public static final String COURSE_ENROLLMENT_UPDATED = "course-enrollment-updated-topic";

    /**
     * Triggered when a Lesson is linked or unlinked to a Quiz.
     * Producer: course-service
     * Consumer: assessment-service
     */
    public static final String LESSON_QUIZ_LINKED = "lesson-quiz-linked-topic";

    /**
     * Triggered when a Course is linked or unlinked to a Final Exam Quiz.
     * Producer: course-service
     * Consumer: assessment-service
     */
    public static final String COURSE_FINAL_EXAM_LINKED = "course-final-exam-linked-topic";

    /**
     * Triggered to request STT (Speech-To-Text) processing.
     * Producer: worker-service
     * Consumer: stt-service (Python)
     */
    public static final String STT_REQUESTS = "stt-requests-topic";

    /**
     * Triggered when STT transcription is completed.
     * Producer: stt-service (Python)
     * Consumer: worker-service
     */
    public static final String STT_RESULTS = "stt-results-topic";

    /**
     * Triggered to request PDF parsing / OCR.
     * Producer: worker-service
     * Consumer: pdf-parser-service (Python)
     */
    public static final String PDF_PARSER_REQUESTS = "pdf-parser-requests-topic";

    /**
     * Triggered when PDF parsing is completed.
     * Producer: pdf-parser-service (Python)
     * Consumer: worker-service
     */
    public static final String PDF_PARSER_RESULTS = "pdf-parser-results-topic";

    /**
     * Triggered to synchronize course details from course-service to search-service (Elasticsearch).
     * Producer: course-service
     * Consumer: search-service
     */
    public static final String COURSE_SEARCH_SYNC = "course-search-sync-topic";

    /**
     * Triggered to issue a certificate generation request.
     */
    public static final String CERTIFICATE_ISSUED = "certificate-issued-topic";

    /**
     * Triggered when a certificate has been successfully generated.
     */
    public static final String CERTIFICATE_GENERATED = "certificate-generated-topic";

    /**
     * Triggered when an admin replies to a user's feedback.
     * Producer: interaction-service
     * Consumer: notification-service
     */
    public static final String FEEDBACK_REPLIED = "feedback-replied-topic";

    /**
     * Standard notification events.
     */
    public static final String NOTIFICATION_EVENTS = "notification-events-topic";
}

