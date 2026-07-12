package com.hust.interactionservice.dto.response;

import com.hust.commonlibrary.dto.TimeResponse;
import lombok.*;
import lombok.experimental.SuperBuilder;

@EqualsAndHashCode(callSuper = true)
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class BlogTopicResponse extends TimeResponse {
    private String id;
    private String name;
    private String slug;
    private String description;
    private String icon;
    private long postCount;
}
