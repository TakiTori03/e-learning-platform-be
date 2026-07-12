package com.hust.assessmentservice.consumer;

import com.hust.assessmentservice.repository.QuizRepository;
import com.hust.commonlibrary.constants.KafkaTopics;
import com.hust.commonlibrary.event.LessonQuizLinkedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings("null")
public class LessonQuizLinkedConsumer {

    private final QuizRepository quizRepository;

    @KafkaListener(topics = KafkaTopics.LESSON_QUIZ_LINKED, groupId = "assessment-service-group")
    public void consumeLessonQuizLinkedEvent(LessonQuizLinkedEvent event) {
        log.info("Received LessonQuizLinkedEvent from Kafka: {}", event);
        try {
            if (com.hust.commonlibrary.enums.LinkAction.LINK.equals(event.getAction())) {
                quizRepository.findById(event.getQuizId()).ifPresentOrElse(quiz -> {
                    quiz.setCourseId(event.getCourseId());
                    quiz.setLessonId(event.getLessonId());
                    quizRepository.save(quiz);
                    log.info("Successfully linked Quiz {} to courseId={}, lessonId={}", event.getQuizId(), event.getCourseId(), event.getLessonId());
                }, () -> log.warn("Quiz {} not found for linking", event.getQuizId()));
            } else if (com.hust.commonlibrary.enums.LinkAction.UNLINK.equals(event.getAction())) {
                quizRepository.findByLessonId(event.getLessonId()).ifPresentOrElse(quiz -> {
                    quiz.setCourseId(null);
                    quiz.setLessonId(null);
                    quizRepository.save(quiz);
                    log.info("Successfully unlinked Quiz {} from lessonId={}", quiz.getId(), event.getLessonId());
                }, () -> log.info("No linked Quiz found for lessonId={}", event.getLessonId()));
            }
        } catch (Exception e) {
            log.error("Error processing LessonQuizLinkedEvent for quizId={}: {}", event.getQuizId(), e.getMessage(), e);
        }
    }
}
