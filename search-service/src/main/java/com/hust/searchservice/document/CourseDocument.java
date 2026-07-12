package com.hust.searchservice.document;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.InnerField;
import org.springframework.data.elasticsearch.annotations.MultiField;
import org.springframework.data.elasticsearch.annotations.Setting;


@Document(indexName = "courses")
@Setting(settingPath = "elasticsearch/settings.json")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseDocument {

    @Id
    private String id;

    @MultiField(
            mainField = @Field(type = FieldType.Text, analyzer = "vietnamese_analyzer"),
            otherFields = {
                    @InnerField(suffix = "suggest", type = FieldType.Text, analyzer = "autocomplete_analyzer", searchAnalyzer = "autocomplete_search_analyzer")
            }
    )
    private String name;

    @Field(type = FieldType.Text, analyzer = "vietnamese_analyzer")
    private String subTitle;

    @Field(type = FieldType.Text, analyzer = "vietnamese_analyzer")
    private String description;

    @Field(type = FieldType.Double)
    private Double price;

    @Field(type = FieldType.Double)
    private Double finalPrice;

    @Field(type = FieldType.Double)
    private Double avgRatingStars;

    @Field(type = FieldType.Integer)
    private Integer studentCount;

    @Field(type = FieldType.Integer)
    private Integer numOfReviews;

    @Field(type = FieldType.Keyword)
    private String level;

    @Field(type = FieldType.Keyword)
    private com.hust.commonlibrary.enums.CourseSyncStatus status;

    @Field(type = FieldType.Keyword)
    private String instructorId;

    @Field(type = FieldType.Keyword)
    private String categoryId;

    @Field(type = FieldType.Keyword)
    private String categoryName;

    @Field(type = FieldType.Date)
    private java.time.Instant createdAt;
}
