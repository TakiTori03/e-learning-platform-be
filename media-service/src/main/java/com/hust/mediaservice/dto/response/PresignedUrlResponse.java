package com.hust.mediaservice.dto.response;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PresignedUrlResponse {
    private String uploadUrl;
    private String mediaId;
    private String objectKey;
}
