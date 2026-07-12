package com.hust.courseservice.entity;

import com.hust.commonlibrary.entity.BaseDocument;
import lombok.*;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.index.TextIndexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "categories")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Category extends BaseDocument {

    @TextIndexed
    @Indexed(unique = true)
    private String name;

    private String description;

    @Indexed(unique = true)
    private String categorySlug;

    private String icon;
}
