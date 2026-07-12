package com.hust.courseservice.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseRequest {
    private String name;
    private String subTitle;
    private String thumbnail;
    private String coursePreview;
    private Double price;
    private Double finalPrice;
    private String description;
    private String level;
    private String categoryId;
    private List<String> requirements;
    private List<String> willLearns;
    private List<String> tags;
}
