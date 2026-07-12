package com.hust.interactionservice.dto.response;

import com.hust.commonlibrary.dto.TimeResponse;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@EqualsAndHashCode(callSuper = true)
@Getter @Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewResponse extends TimeResponse {
    private String id;
    private String code;
    private String courseId;
    private String userId;
    private String title;
    private String content;
    private Double ratingStar;
    private java.util.List<ReviewResponse> replies;
    private com.hust.commonlibrary.dto.UserSharedProfile user;
    private CourseDto course;

    @lombok.Data
    @NoArgsConstructor
    @AllArgsConstructor
    @lombok.Builder
    public static class CourseDto {
        private String id;
        private String name;
        private String thumbnail;
    }
}
