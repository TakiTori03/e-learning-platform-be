package com.hust.learningservice.entity;
 
import com.hust.commonlibrary.entity.BaseDocument;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.List;
 
@Document(collection = "discussions")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class Discussion extends BaseDocument {
    private String code;
    private String courseId;
    private String sectionId;
    private String lessonId;
    private String userId;
    private String content;
    private String parentId;
    private String rootId;

    @Builder.Default
    private List<String> likedUserIds = new ArrayList<>();

    @Builder.Default
    private List<String> dislikedUserIds = new ArrayList<>();
}
