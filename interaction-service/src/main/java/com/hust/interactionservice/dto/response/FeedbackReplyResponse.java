package com.hust.interactionservice.dto.response;

import com.hust.commonlibrary.dto.TimeResponse;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@EqualsAndHashCode(callSuper = true)
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class FeedbackReplyResponse extends TimeResponse {
    private String id;
    private String feedbackId;
    private String content;
}

