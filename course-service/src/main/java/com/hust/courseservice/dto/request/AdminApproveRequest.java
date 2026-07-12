package com.hust.courseservice.dto.request;

import com.hust.courseservice.entity.enums.CourseStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminApproveRequest {
    private CourseStatus status;
    private String note;
}
