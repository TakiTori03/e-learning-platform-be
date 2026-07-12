package com.hust.mediaservice.dto.response;

import com.hust.commonlibrary.dto.TimeResponse;
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
public class MediaResponse extends TimeResponse {
    private String id;
    private String fileName;
    private String fileType;
    private String contentType;
    private Long fileSize;
    private String url;
    private String provider;
    private String status;
    private Double duration;
}
