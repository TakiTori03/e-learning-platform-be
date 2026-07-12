package com.hust.courseservice.service.impl;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import com.hust.commonlibrary.constants.AppConstants;
import com.hust.commonlibrary.dto.ListResponse;
import com.hust.commonlibrary.exception.payload.ResourceNotFoundException;
import com.hust.courseservice.dto.request.CategoryRequest;
import com.hust.courseservice.dto.response.CategoryResponse;
import com.hust.courseservice.entity.Category;
import com.hust.courseservice.mapper.CategoryMapper;
import com.hust.courseservice.repository.CategoryRepository;
import com.hust.courseservice.service.CategoryService;
import com.hust.courseservice.service.CourseService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;
    private final MongoTemplate mongoTemplate;
    private final CourseService courseService;

    private CategoryResponse toResponseWithCount(Category entity) {
        CategoryResponse response = categoryMapper.entityToResponse(entity);
        if (response != null) {
            Map<String, Long> countMap = courseService.countByCategories(List.of(entity.getId()));
            response.setCourses(countMap.getOrDefault(entity.getId(), 0L));
        }
        return response;
    }

    private List<CategoryResponse> toResponseWithCount(List<Category> entities) {
        List<CategoryResponse> responses = categoryMapper.entityToResponse(entities);
        if (responses != null && !responses.isEmpty()) {
            List<String> ids = responses.stream().map(CategoryResponse::getId).toList();
            Map<String, Long> countMap = courseService.countByCategories(ids);
            for (CategoryResponse res : responses) {
                res.setCourses(countMap.getOrDefault(res.getId(), 0L));
            }
        }
        return responses;
    }

    @Override
    public CategoryResponse create(CategoryRequest request) {
        Category category = categoryMapper.requestToEntity(request);
        category.setCategorySlug(toSlug(request.getName()));
        category = categoryRepository.save(category);
        return toResponseWithCount(category);
    }

    @Override
    public CategoryResponse update(String id, CategoryRequest request) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        AppConstants.Resource_Constants.CATEGORY,
                        AppConstants.Field_Constants.ID,
                        id));

        categoryMapper.partialUpdate(category, request);
        category.setCategorySlug(toSlug(request.getName()));
        category = categoryRepository.save(category);
        return toResponseWithCount(category);
    }

    @Override
    public void delete(String id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        AppConstants.Resource_Constants.CATEGORY,
                        AppConstants.Field_Constants.ID,
                        id));
        categoryRepository.delete(category);
    }

    @Override
    public CategoryResponse detail(String id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        AppConstants.Resource_Constants.CATEGORY,
                        AppConstants.Field_Constants.ID,
                        id));
        return toResponseWithCount(category);
    }

    @Override
    public ListResponse<CategoryResponse> search(String text, Pageable pageable) {
        Query query = new Query();

        if (text != null && !text.trim().isEmpty()) {
            String sanitizedText = Pattern.quote(text.trim());
            query.addCriteria(new Criteria().orOperator(
                    Criteria.where("name").regex(sanitizedText, "i"),
                    Criteria.where("description").regex(sanitizedText, "i")
            ));
        }

        long total = mongoTemplate.count(query, Category.class);
        query.with(pageable);
        List<Category> categories = mongoTemplate.find(query, Category.class);

        List<CategoryResponse> responseList = toResponseWithCount(categories);

        Page<Category> categoryPage = new PageImpl<>(categories, pageable, total);
        return ListResponse.of(responseList, categoryPage);
    }

    @Override
    public List<CategoryResponse> getSelect(String q) {
        if (q != null && !q.trim().isEmpty()) {
            Query query = new Query();
            String sanitizedText = Pattern.quote(q.trim());
            query.addCriteria(new Criteria().orOperator(
                    Criteria.where("name").regex(sanitizedText, "i"),
                    Criteria.where("description").regex(sanitizedText, "i")
            ));
            List<Category> categories = mongoTemplate.find(query, Category.class);
            return toResponseWithCount(categories);
        }
        return toResponseWithCount(categoryRepository.findAll());
    }

    @Override
    public void updateStatus(String id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        AppConstants.Resource_Constants.CATEGORY,
                        AppConstants.Field_Constants.ID,
                        id));
        // Toggle or set status logic
        categoryRepository.save(category);
    }

    private String toSlug(String input) {
        if (input == null) return "";
        String nowhitespace = Pattern.compile("\\s+").matcher(input).replaceAll("-");
        String normalized = Normalizer.normalize(nowhitespace, Normalizer.Form.NFD);
        String slug = Pattern.compile("[^\\w-]").matcher(normalized).replaceAll("");
        return slug.toLowerCase(Locale.ENGLISH);
    }
}
