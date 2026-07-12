package com.hust.aiservice.service;

import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;
import java.util.List;

@Service
@Slf4j
public class PromptGuardService {

    // Danh sách các từ khóa mang tính chất "Hack", "Jailbreak" hoặc "Prompt Injection"
    private static final List<String> DENY_LIST = List.of(
            "ignore previous",
            "bỏ qua",
            "quên đi",
            "hướng dẫn trước",
            "system prompt",
            "mật khẩu",
            "database",
            "in ra toàn bộ",
            "drop table",
            "delete from",
            "you are a developer",
            "admin",
            "lệnh hệ thống",
            "hack"
    );

    /**
     * Quét và chặn các câu hỏi có dấu hiệu tấn công AI (Prompt Injection)
     * @param message Tin nhắn của người dùng
     * @return true nếu an toàn, false nếu có dấu hiệu tấn công
     */
    public boolean isSafePrompt(String message) {
        if (message == null || message.trim().isEmpty()) {
            return true;
        }
        
        String lowerCaseMsg = message.toLowerCase();
        for (String keyword : DENY_LIST) {
            if (keyword.equals("bỏ qua") || keyword.equals("quên đi")) {
                if (lowerCaseMsg.contains(keyword + " các") || 
                    lowerCaseMsg.contains(keyword + " hướng dẫn") || 
                    lowerCaseMsg.contains(keyword + " quy tắc")) {
                    log.warn("🚨 [Prompt Guard] Phát hiện dấu hiệu Prompt Injection với cụm từ nguy hiểm: {}", keyword);
                    return false;
                }
            } else {
                if (lowerCaseMsg.contains(keyword)) {
                    log.warn("🚨 [Prompt Guard] Phát hiện dấu hiệu Prompt Injection với cụm từ nguy hiểm: {}", keyword);
                    return false;
                }
            }
        }
        return true;
    }
}
