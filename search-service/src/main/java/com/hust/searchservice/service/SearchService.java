package com.hust.searchservice.service;

import com.hust.commonlibrary.dto.ListResponse;
import com.hust.searchservice.dto.response.CourseSearchResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface SearchService {
    ListResponse<CourseSearchResponse> searchCourses(
            String q,
            List<String> authors,
            List<String> topics,
            List<String> levels,
            List<String> prices,
            Double rating,
            String status,
            Pageable pageable
    );

    List<String> suggestCourses(String q);
}
