package com.hust.interactionservice.dto.response;

import com.hust.commonlibrary.dto.TimeResponse;
import com.hust.interactionservice.constant.FeedbackStatus;
import com.hust.interactionservice.constant.FeedbackType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class FeedbackResponse extends TimeResponse {
    private String id;
    private String name;
    private String email;

    private FeedbackType type;
    private String title;
    private String content;
    private FeedbackStatus status;
    
    private List<FeedbackReplyResponse> replies;
}
