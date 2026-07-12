package com.hust.assessmentservice.entity;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnswerOption {
    private String id; // UUID sinh tự động cho phương án
    private String optionText;
    private Boolean isCorrect; // Đánh dấu đây là phương án đúng
}
