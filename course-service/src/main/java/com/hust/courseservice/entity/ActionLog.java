package com.hust.courseservice.entity;

import org.springframework.data.mongodb.core.mapping.Document;

import com.hust.commonlibrary.entity.BaseDocument;
import com.hust.courseservice.entity.enums.ActionLogType;
import com.hust.courseservice.entity.enums.FunctionType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@Document(collection = "action_logs")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActionLog extends BaseDocument {
    @org.springframework.data.mongodb.core.index.Indexed
    private String userId;
    private String createdByName;
    @org.springframework.data.mongodb.core.index.Indexed
    private String courseId;
    private String sectionId;
    private String lessonId;
    private String categoryId;

    private String description;
    private ActionLogType type;
    private FunctionType functionType;
}
