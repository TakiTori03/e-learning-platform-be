package com.hust.interactionservice.entity;

import org.springframework.data.mongodb.core.mapping.Document;

import com.hust.commonlibrary.entity.BaseDocument;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Document(collection = "reviews")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class Review extends BaseDocument {
    private String code;
    @org.springframework.data.mongodb.core.index.Indexed
    private String courseId;
    @org.springframework.data.mongodb.core.index.Indexed
    private String userId;
    private String title;
    private String content;
    private Double ratingStar;
}
