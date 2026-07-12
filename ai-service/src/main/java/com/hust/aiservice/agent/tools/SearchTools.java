package com.hust.aiservice.agent.tools;

import com.hust.aiservice.service.impl.TavilySearchService;
import dev.langchain4j.agent.tool.Tool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class SearchTools {
    private final TavilySearchService tavilySearchService;

    @Tool("Sử dụng để tìm kiếm thông tin mới nhất trên Internet khi dữ liệu nội bộ không đủ để trả lời.")
    public String searchWeb(String query) {
        log.info("🛠️ [SearchTools] searchWeb (query: {})", query);
        try {
            var results = tavilySearchService.search(query);
            return tavilySearchService.formatAsContext(results);
        } catch (Exception e) {
            log.error("Tool searchWeb error", e);
            return "Lỗi hệ thống khi tìm kiếm trên Internet.";
        }
    }
}
