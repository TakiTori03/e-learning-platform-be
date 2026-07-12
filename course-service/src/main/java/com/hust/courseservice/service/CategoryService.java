package com.hust.courseservice.service;

import com.hust.commonlibrary.dto.ListResponse;
import com.hust.courseservice.dto.request.CategoryRequest;
import com.hust.courseservice.dto.response.CategoryResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface CategoryService {
    CategoryResponse create(CategoryRequest request);
    CategoryResponse update(String id, CategoryRequest request);
    void delete(String id);
    CategoryResponse detail(String id);
    ListResponse<CategoryResponse> search(String text, Pageable pageable);
    List<CategoryResponse> getSelect(String q);
    void updateStatus(String id);
}
