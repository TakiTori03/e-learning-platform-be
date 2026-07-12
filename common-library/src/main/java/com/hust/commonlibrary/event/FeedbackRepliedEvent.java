package com.hust.commonlibrary.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeedbackRepliedEvent {
    private String feedbackId;
    private String userId; // The user who submitted the feedback
    private String userEmail; // Email of the user
    private String userFullName;
    private String title; // Feedback title
    private String content; // Original feedback content
    private String replyContent; // Admin's reply
    private String repliedBy; // Admin name
}
