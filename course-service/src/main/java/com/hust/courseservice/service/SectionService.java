package com.hust.courseservice.service;

import com.hust.commonlibrary.dto.ListResponse;
import com.hust.courseservice.dto.request.SectionRequest;
import com.hust.courseservice.dto.response.SectionResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface SectionService {
    SectionResponse create(SectionRequest request);
    SectionResponse update(String id, SectionRequest request);
    void delete(List<String> ids);
    SectionResponse detail(String id);
    ListResponse<SectionResponse> search(String text, Pageable pageable);
    List<SectionResponse> getByCourseId(String courseId);
    void reorder(List<String> sectionIds);
}
