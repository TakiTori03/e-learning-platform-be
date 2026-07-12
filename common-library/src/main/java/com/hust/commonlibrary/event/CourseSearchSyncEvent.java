package com.hust.commonlibrary.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseSearchSyncEvent implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private String name;
    private String subTitle;
    private String description;
    private Double price;
    private Double finalPrice;
    private Double avgRatingStars;
    private Integer studentCount;
    private Integer numOfReviews;
    private String level;
    private com.hust.commonlibrary.enums.CourseSyncStatus status;
    private String instructorId;
    private String categoryId;
    private String categoryName;
    private com.hust.commonlibrary.enums.SyncAction action; // "CREATE", "UPDATE", "DELETE"
    private java.time.Instant createdAt;
    
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();
}
