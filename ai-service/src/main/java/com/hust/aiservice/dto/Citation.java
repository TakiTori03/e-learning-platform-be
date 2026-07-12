package com.hust.aiservice.dto;

import com.hust.commonlibrary.entity.ContentType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Citation {
    private String lessonId;
    private ContentType contentType;
    private String sourceCitation;
}
