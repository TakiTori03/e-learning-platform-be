package com.hust.commonlibrary.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PdfParserResultEvent {
    private String mediaId;
    private String courseId;
    private String lessonId;
    private String fileUrl;
    private int totalPages;
    private List<ParsedPage> pages;
    private boolean success;
    private String errorMessage;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ParsedPage {
        private int page;
        private String rawContent;
        private List<String> chunks;
    }
}
