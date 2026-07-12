package com.hust.courseservice.service;

import com.hust.commonlibrary.dto.ListResponse;
import com.hust.courseservice.dto.request.LessonRequest;
import com.hust.courseservice.dto.response.LessonResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface LessonService {
    LessonResponse create(LessonRequest request);
    LessonResponse update(String id, LessonRequest request);
    void delete(List<String> ids);
    LessonResponse detail(String id);
    ListResponse<LessonResponse> search(String text, Pageable pageable);
    
    List<LessonResponse> getBySectionId(String sectionId);
    List<LessonResponse> getBySectionIdEnrolled(String sectionId, String userId);
    List<LessonResponse> getByCourseId(String courseId);
    void updateDone(String id, String userId); // updateLessonDoneByUser
    List<String> getUsersByLessonId(String id);

    void reorder(List<String> lessonIds);
}
