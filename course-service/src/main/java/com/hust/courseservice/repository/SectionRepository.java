package com.hust.courseservice.repository;

import com.hust.courseservice.entity.Section;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.TextCriteria;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SectionRepository extends MongoRepository<Section, String> {
    List<Section> findAllByCourseIdOrderByPositionAsc(String courseId);

    Page<Section> findAllBy(TextCriteria criteria, Pageable pageable);
}
