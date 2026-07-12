package com.hust.assessmentservice.consumer;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.hust.assessmentservice.repository.QuizRepository;
import com.hust.commonlibrary.constants.KafkaTopics;
import com.hust.commonlibrary.event.CourseFinalExamLinkedEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class CourseFinalExamLinkedConsumer {

    private final QuizRepository quizRepository;

    @KafkaListener(topics = KafkaTopics.COURSE_FINAL_EXAM_LINKED, groupId = "assessment-service-group")
    public void consumeCourseFinalExamLinkedEvent(CourseFinalExamLinkedEvent event) {
        log.info("Received CourseFinalExamLinkedEvent from Kafka: {}", event);
        try {
            if (com.hust.commonlibrary.enums.LinkAction.LINK.equals(event.getAction())) {
                quizRepository.findById(event.getFinalExamId()).ifPresentOrElse(quiz -> {
                    quiz.setCourseId(event.getCourseId());
                    quizRepository.save(quiz);
                    log.info("Successfully linked final exam Quiz {} to courseId={}", event.getFinalExamId(), event.getCourseId());
                }, () -> log.warn("Final exam Quiz {} not found for linking", event.getFinalExamId()));
            }
        } catch (Exception e) {
            log.error("Error processing CourseFinalExamLinkedEvent for finalExamId={}: {}", event.getFinalExamId(), e.getMessage(), e);
        }
    }
}
