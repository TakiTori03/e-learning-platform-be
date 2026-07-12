package com.hust.aiservice.agent.tools;

import com.hust.aiservice.dto.Citation;
import com.hust.aiservice.dto.response.ChatResponse;
import com.hust.aiservice.service.RagService;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.P;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class TutorTools {
    private final RagService ragService;

    private static final ThreadLocal<List<Citation>> CITATIONS_THREAD_LOCAL = new ThreadLocal<>();

    public static List<Citation> getCitations() {
        return CITATIONS_THREAD_LOCAL.get();
    }

    public static void clear() {
        CITATIONS_THREAD_LOCAL.remove();
    }

    @Tool("Sử dụng khi học viên hỏi về kiến thức chuyên môn, bài giảng, bài tập lập trình, khái niệm trong khóa học. Cần truyền đúng courseId.")
    public String searchCourseMaterials(
            @P("ID của khóa học hiện tại") String courseId,
            @P("Câu hỏi chuyên môn hoặc từ khóa học viên cần tra cứu trong tài liệu bài giảng") String question
    ) {
        log.info("🛠️ [TutorTools] searchCourseMaterials (courseId: {}, question: {})", courseId, question);
        try {
            ChatResponse response = ragService.retrieveContext(courseId, question);
            if (response != null && response.getCitations() != null) {
                CITATIONS_THREAD_LOCAL.set(response.getCitations());
            }
            return response != null ? response.getAnswer() : "Không tìm thấy tài liệu phù hợp.";
        } catch (Exception e) {
            log.error("Tool searchCourseMaterials error", e);
            return "Lỗi hệ thống khi tìm tài liệu.";
        }
    }
}
