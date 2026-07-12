package com.hust.courseservice.dto.response;

import com.hust.commonlibrary.dto.TimeResponse;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class CourseResponse extends TimeResponse {
    private String id;
    private String code;
    private String name;
    private String subTitle;
    private String thumbnail;
    private String coursePreview;
    private Integer views;
    private Double price;
    private Double finalPrice;
    private String description;
    private String level;
    private String courseSlug;
    private String status;
    private String instructorId;
    private com.hust.commonlibrary.dto.UserSharedProfile instructor;
    
    private CategoryResponse category;
    private List<SectionResponse> sections;

    private List<String> requirements;
    private List<String> willLearns;
    private List<String> tags;


   
    private Integer sectionCount;      
    private Integer lessonCount;      
    private Double totalVideosLength;  

    // Denormalized Statistics (synced via Kafka events)
    private Double avgRatingStars;
    private Integer studentCount;
    private Integer numOfReviews;
    private String note;
    private String finalExamId;
}
