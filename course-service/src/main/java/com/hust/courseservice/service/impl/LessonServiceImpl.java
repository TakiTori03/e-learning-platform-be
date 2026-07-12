package com.hust.courseservice.service.impl;

import com.hust.commonlibrary.constants.AppConstants;
import com.hust.commonlibrary.dto.ListResponse;
import com.hust.commonlibrary.exception.payload.ResourceNotFoundException;
import com.hust.commonlibrary.utils.SecurityUtils;
import com.hust.commonlibrary.event.LessonQuizLinkedEvent;
import com.hust.courseservice.dto.request.LessonRequest;
import com.hust.courseservice.dto.response.LessonResponse;
import com.hust.courseservice.entity.Course;
import com.hust.courseservice.entity.Lesson;
import com.hust.courseservice.event.CurriculumMutatedEvent;
import com.hust.courseservice.mapper.LessonMapper;
import com.hust.courseservice.repository.CourseRepository;
import com.hust.courseservice.repository.LessonRepository;
import com.hust.courseservice.service.LessonService;
import com.hust.courseservice.entity.enums.LessonType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.TextCriteria;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings("null")
public class LessonServiceImpl implements LessonService {

    private final LessonRepository lessonRepository;
    private final CourseRepository courseRepository;
    private final LessonMapper lessonMapper;
    private final ApplicationEventPublisher eventPublisher;

    private void evictCourseDetailCache(String courseId) {
        if (courseId != null) {
            log.info("Publishing CurriculumMutatedEvent for courseId: {}", courseId);
            eventPublisher.publishEvent(new CurriculumMutatedEvent(courseId));
        }
    }

    @Override
    @Transactional
    public LessonResponse create(LessonRequest request) {
        validateCourseOwnership(request.getCourseId());

        Lesson lesson = lessonMapper.requestToEntity(request);
        lesson = lessonRepository.save(lesson);

        if (LessonType.QUIZ == lesson.getType() && lesson.getContent() != null && !lesson.getContent().isBlank()) {
            eventPublisher.publishEvent(LessonQuizLinkedEvent.builder()
                    .courseId(lesson.getCourseId())
                    .lessonId(lesson.getId())
                    .quizId(lesson.getContent())
                    .action(com.hust.commonlibrary.enums.LinkAction.LINK)
                    .build());
        }

        evictCourseDetailCache(lesson.getCourseId());
        return lessonMapper.entityToResponse(lesson);
    }

    @Override
    @Transactional
    public LessonResponse update(String id, LessonRequest request) {
        Lesson lesson = lessonRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        AppConstants.Resource_Constants.LESSON,
                        AppConstants.Field_Constants.ID,
                        id));

        validateCourseOwnership(lesson.getCourseId());

        String oldQuizId = LessonType.QUIZ == lesson.getType() ? lesson.getContent() : null;

        lessonMapper.partialUpdate(lesson, request);
        lesson = lessonRepository.save(lesson);

        String newQuizId = LessonType.QUIZ == lesson.getType() ? lesson.getContent() : null;

        // Handle unlinking the old quiz if it changed
        if (oldQuizId != null && !oldQuizId.isBlank() && !oldQuizId.equals(newQuizId)) {
            eventPublisher.publishEvent(LessonQuizLinkedEvent.builder()
                    .courseId(lesson.getCourseId())
                    .lessonId(lesson.getId())
                    .quizId(oldQuizId)
                    .action(com.hust.commonlibrary.enums.LinkAction.UNLINK)
                    .build());
        }

        // Handle linking the new quiz if it changed
        if (newQuizId != null && !newQuizId.isBlank() && !newQuizId.equals(oldQuizId)) {
            eventPublisher.publishEvent(LessonQuizLinkedEvent.builder()
                    .courseId(lesson.getCourseId())
                    .lessonId(lesson.getId())
                    .quizId(newQuizId)
                    .action(com.hust.commonlibrary.enums.LinkAction.LINK)
                    .build());
        }

        evictCourseDetailCache(lesson.getCourseId());
        return lessonMapper.entityToResponse(lesson);
    }

    @Override
    @Transactional
    public void delete(List<String> ids) {
        if (ids == null || ids.isEmpty()) return;
        List<Lesson> lessons = lessonRepository.findAllById(ids);

        lessons.stream()
                .map(Lesson::getCourseId)
                .distinct()
                .forEach(this::validateCourseOwnership);

        List<String> courseIdsToEvict = lessons.stream()
                .map(Lesson::getCourseId)
                .distinct()
                .toList();

        for (Lesson lesson : lessons) {
            if (LessonType.QUIZ == lesson.getType() && lesson.getContent() != null && !lesson.getContent().isBlank()) {
                eventPublisher.publishEvent(LessonQuizLinkedEvent.builder()
                        .courseId(lesson.getCourseId())
                        .lessonId(lesson.getId())
                        .quizId(lesson.getContent())
                        .action(com.hust.commonlibrary.enums.LinkAction.UNLINK)
                        .build());
            }
        }

        lessonRepository.deleteAll(lessons);
        courseIdsToEvict.forEach(this::evictCourseDetailCache);
    }

    @Override
    public LessonResponse detail(String id) {
        return lessonRepository.findById(id)
                .map(lessonMapper::entityToResponse)
                .orElseThrow(() -> new ResourceNotFoundException(
                        AppConstants.Resource_Constants.LESSON,
                        AppConstants.Field_Constants.ID,
                        id));
    }

    @Override
    public ListResponse<LessonResponse> search(String text, Pageable pageable) {
        Page<Lesson> lessonPage;
        if (text == null || text.trim().isEmpty()) {
            lessonPage = lessonRepository.findAll(pageable);
        } else {
            TextCriteria criteria = TextCriteria.forDefaultLanguage().matchingAny(text);
            lessonPage = lessonRepository.findAllBy(criteria, pageable);
        }
        return ListResponse.of(lessonMapper.entityToResponse(lessonPage.getContent()), lessonPage);
    }

    @Override
    public List<LessonResponse> getBySectionId(String sectionId) {
        return lessonMapper.entityToResponse(lessonRepository.findAllBySectionIdOrderByPositionAsc(sectionId));
    }

    @Override
    public List<LessonResponse> getBySectionIdEnrolled(String sectionId, String userId) {
       return List.of();
    }

    @Override
    public List<LessonResponse> getByCourseId(String courseId) {
        return lessonMapper.entityToResponse(lessonRepository.findAllByCourseIdOrderByPositionAsc(courseId));
    }

    @Override
    public void updateDone(String id, String userId) {

    }

    @Override
    public List<String> getUsersByLessonId(String id) {
        return List.of(); // Placeholder
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public void reorder(List<String> lessonIds) {
        if (lessonIds == null || lessonIds.isEmpty()) return;

        List<Lesson> lessons = lessonRepository.findAllById(lessonIds);

        lessons.stream()
                .map(Lesson::getCourseId)
                .distinct()
                .forEach(this::validateCourseOwnership);

        List<String> courseIdsToEvict = lessons.stream()
                .map(Lesson::getCourseId)
                .distinct()
                .toList();

        java.util.Map<String, Lesson> lessonMap = lessons.stream()
                .collect(java.util.stream.Collectors.toMap(Lesson::getId, l -> l));

        List<Lesson> toUpdate = new java.util.ArrayList<>();
        for (int i = 0; i < lessonIds.size(); i++) {
            Lesson lesson = lessonMap.get(lessonIds.get(i));
            if (lesson != null) {
                lesson.setPosition(i + 1);
                toUpdate.add(lesson);
            }
        }

        if (!toUpdate.isEmpty()) {
            lessonRepository.saveAll(toUpdate);
            courseIdsToEvict.forEach(this::evictCourseDetailCache);
        }
    }

    private void validateCourseOwnership(String courseId) {
        if (courseId == null) {
            throw new IllegalArgumentException("Course ID cannot be null");
        }
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = authentication != null && authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (isAdmin) return;

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course", "id", courseId));

        String currentUserId = SecurityUtils.getCurrentUserIdOrThrow();
        if (!course.getInstructorId().equals(currentUserId)) {
            throw new AccessDeniedException("You do not have permission to perform this action on this course content!");
        }
    }
}
