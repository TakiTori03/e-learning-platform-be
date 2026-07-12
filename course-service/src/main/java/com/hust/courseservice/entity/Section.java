package com.hust.courseservice.entity;

import com.hust.commonlibrary.entity.BaseDocument;
import lombok.*;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.TextIndexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "sections")
@CompoundIndexes({
    @CompoundIndex(name = "course_position_idx", def = "{'courseId': 1, 'position': 1}")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Section extends BaseDocument {
    private String courseId;

    @TextIndexed
    private String name;
    private String description;
    private Integer position;
}
