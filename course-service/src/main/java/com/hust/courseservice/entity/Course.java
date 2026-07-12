package com.hust.courseservice.entity;

import java.util.List;

import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.index.TextIndexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.DocumentReference;

import com.hust.commonlibrary.entity.BaseDocument;
import com.hust.courseservice.entity.enums.CourseLevel;
import com.hust.courseservice.entity.enums.CourseStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Document(collection = "courses")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Course extends BaseDocument {

    @Indexed(unique = true)
    private String code;

    @TextIndexed
    private String name;

    private String subTitle;

    private String thumbnail;

    private String coursePreview;

    @Builder.Default
    private Integer views = 0;

    private Double price;

    private Double finalPrice;

    @TextIndexed
    private String description;

    private CourseLevel level;

    @Indexed
    @Builder.Default
    private CourseStatus status = CourseStatus.DRAFT;

    @Indexed(unique = true)
    private String courseSlug;

    @Indexed
    private String instructorId;

    @Indexed
    @DocumentReference(lazy = true)
    private Category category;

    private List<String> requirements;

    private List<String> willLearns;

    private List<String> tags;

    @Builder.Default
    @Indexed
    private Double avgRatingStars = 0.0;

    @Builder.Default
    private Integer studentCount = 0;

    @Builder.Default
    private Integer numOfReviews = 0;

    private String note;

    @Indexed(unique = true, partialFilter = "{'finalExamId': { $type: 'string' }}")
    private String finalExamId;
}
