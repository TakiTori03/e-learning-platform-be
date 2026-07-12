package com.hust.courseservice.service.impl;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.TextCriteria;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hust.commonlibrary.constants.AppConstants;
import com.hust.commonlibrary.dto.ListResponse;
import com.hust.commonlibrary.exception.payload.ResourceNotFoundException;
import com.hust.commonlibrary.utils.SecurityUtils;
import com.hust.courseservice.dto.request.SectionRequest;
import com.hust.courseservice.dto.response.SectionResponse;
import com.hust.courseservice.entity.Course;
import com.hust.courseservice.entity.Lesson;
import com.hust.courseservice.entity.Section;
import com.hust.courseservice.event.CurriculumMutatedEvent;
import com.hust.courseservice.mapper.SectionMapper;
import com.hust.courseservice.repository.CourseRepository;
import com.hust.courseservice.repository.LessonRepository;
import com.hust.courseservice.repository.SectionRepository;
import com.hust.courseservice.service.LessonService;
import com.hust.courseservice.service.SectionService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;

@Service
@RequiredArgsConstructor
@Slf4j
public class SectionServiceImpl implements SectionService {

    private final SectionRepository sectionRepository;
    private final CourseRepository courseRepository;
    private final SectionMapper sectionMapper;
    private final LessonService lessonService;
    private final LessonRepository lessonRepository;
    private final ApplicationEventPublisher eventPublisher;

    private void evictCourseDetailCache(String courseId) {
        if (courseId != null) {
            log.info("Publishing CurriculumMutatedEvent for courseId: {}", courseId);
            eventPublisher.publishEvent(new CurriculumMutatedEvent(courseId));
        }
    }

    @Override
    public SectionResponse create(SectionRequest request) {
        validateCourseOwnership(request.getCourseId());
        Section section = sectionMapper.requestToEntity(request);
        section = sectionRepository.save(section);
        evictCourseDetailCache(section.getCourseId());
        return sectionMapper.entityToResponse(section);
    }

    @Override
    public SectionResponse update(String id, SectionRequest request) {
        Section section = sectionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        AppConstants.Resource_Constants.SESSION,
                        AppConstants.Field_Constants.ID,
                        id));

        validateCourseOwnership(section.getCourseId());

        sectionMapper.partialUpdate(section, request);
        section = sectionRepository.save(section);
        evictCourseDetailCache(section.getCourseId());
        return sectionMapper.entityToResponse(section);
    }

    @Override
    @Transactional
    public void delete(List<String> ids) {
        if (ids == null || ids.isEmpty()) return;
        List<Section> sections = sectionRepository.findAllById(ids);

        sections.stream()
                .map(Section::getCourseId)
                .distinct()
                .forEach(this::validateCourseOwnership);

        List<String> courseIdsToEvict = sections.stream()
                .map(Section::getCourseId)
                .distinct()
                .toList();

        // Cascade delete lessons belonging to these sections
        for (Section section : sections) {
            List<Lesson> lessons = lessonRepository.findAllBySectionIdOrderByPositionAsc(section.getId());
            if (lessons != null && !lessons.isEmpty()) {
                List<String> lessonIds = lessons.stream().map(Lesson::getId).toList();
                lessonService.delete(lessonIds);
            }
        }

        sectionRepository.deleteAll(sections);

        // Evict course cache
        courseIdsToEvict.forEach(this::evictCourseDetailCache);
    }

    @Override
    public SectionResponse detail(String id) {
        return sectionRepository.findById(id)
                .map(sectionMapper::entityToResponse)
                .orElseThrow(() -> new ResourceNotFoundException(
                        AppConstants.Resource_Constants.SESSION,
                        AppConstants.Field_Constants.ID,
                        id));
    }

    @Override
    public ListResponse<SectionResponse> search(String text, Pageable pageable) {
        Page<Section> sectionPage;
        if (text == null || text.trim().isEmpty()) {
            sectionPage = sectionRepository.findAll(pageable);
        } else {
            TextCriteria criteria = TextCriteria.forDefaultLanguage().matchingAny(text);
            sectionPage = sectionRepository.findAllBy(criteria, pageable);
        }
        return ListResponse.of(sectionMapper.entityToResponse(sectionPage.getContent()), sectionPage);
    }

    @Override
    public List<SectionResponse> getByCourseId(String courseId) {
        return sectionMapper.entityToResponse(sectionRepository.findAllByCourseIdOrderByPositionAsc(courseId));
    }

    @Override
    @Transactional
    public void reorder(List<String> sectionIds) {
        if (sectionIds == null || sectionIds.isEmpty()) return;

        List<Section> sections = sectionRepository.findAllById(sectionIds);

        sections.stream()
                .map(Section::getCourseId)
                .distinct()
                .forEach(this::validateCourseOwnership);

        List<String> courseIdsToEvict = sections.stream()
                .map(Section::getCourseId)
                .distinct()
                .toList();

        Map<String, Section> sectionMap = sections.stream()
                .collect(Collectors.toMap(Section::getId, s -> s));

        List<Section> toUpdate = new java.util.ArrayList<>();
        for (int i = 0; i < sectionIds.size(); i++) {
            Section section = sectionMap.get(sectionIds.get(i));
            if (section != null) {
                section.setPosition(i + 1);
                toUpdate.add(section);
            }
        }

        if (!toUpdate.isEmpty()) {
            sectionRepository.saveAll(toUpdate);
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
