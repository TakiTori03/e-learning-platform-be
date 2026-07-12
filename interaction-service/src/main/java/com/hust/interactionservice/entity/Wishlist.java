package com.hust.interactionservice.entity;

import com.hust.commonlibrary.entity.BaseDocument;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.CompoundIndex;

@Document(collection = "wishlist")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@CompoundIndex(name = "user_course_idx", def = "{'userId': 1, 'courseId': 1}", unique = true)
public class Wishlist extends BaseDocument {
    private String userId;
    private String courseId;
}
