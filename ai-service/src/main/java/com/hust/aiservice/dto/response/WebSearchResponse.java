package com.hust.aiservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Kết quả tìm kiếm web từ Tavily Search API.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebSearchResponse {

    /** Tiêu đề trang web */
    private String title;

    /** Nội dung tóm tắt */
    private String content;

    /** URL nguồn */
    private String url;

    /** Điểm liên quan (do Tavily chấm) */
    private double score;
}
