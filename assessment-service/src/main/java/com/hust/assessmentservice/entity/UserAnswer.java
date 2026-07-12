package com.hust.assessmentservice.entity;

import lombok.*;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserAnswer {
    private String questionId;
    private List<String> selectedOptionIds; // Các ID phương án học viên đã chọn
    private Boolean isCorrect; // Đánh dấu câu trả lời này có đúng hay không
}
