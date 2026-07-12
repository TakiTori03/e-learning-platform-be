package com.hust.searchservice.repository;

import com.hust.searchservice.document.CourseDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CourseSearchRepository extends ElasticsearchRepository<CourseDocument, String> {
}
