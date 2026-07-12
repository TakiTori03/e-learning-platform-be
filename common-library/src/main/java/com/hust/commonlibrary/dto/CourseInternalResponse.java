package com.hust.commonlibrary.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.EqualsAndHashCode;

import java.util.List;

import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class CourseInternalResponse extends TimeResponse {
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
    
    // Statistics
    private Double avgRatingStars;
    private Integer studentCount;
    private Integer numOfReviews;
    private Integer sectionCount;      
    private Integer lessonCount;      
    private Double totalVideosLength;  
    
    private List<String> requirements;
    private List<String> willLearns;
    private List<String> tags;
}
