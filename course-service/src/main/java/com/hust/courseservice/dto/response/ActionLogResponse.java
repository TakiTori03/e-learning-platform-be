package com.hust.courseservice.dto.response;

import com.hust.commonlibrary.dto.TimeResponse;
import com.hust.commonlibrary.dto.UserSharedProfile;
import com.hust.courseservice.entity.enums.ActionLogType;
import com.hust.courseservice.entity.enums.FunctionType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@EqualsAndHashCode(callSuper = true)
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class ActionLogResponse extends TimeResponse {
    private String id;
    private String userId;
    private String createdByName;
    private String courseId;
    private String sectionId;
    private String lessonId;
    private String categoryId;
    private String description;
    private ActionLogType type;
    private FunctionType functionType;

    private UserSharedProfile instructor;
}
