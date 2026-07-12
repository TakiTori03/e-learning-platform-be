package com.hust.learningservice.service;

import com.hust.commonlibrary.dto.ListResponse;
import com.hust.learningservice.dto.request.DiscussionRequest;
import com.hust.learningservice.dto.response.DiscussionResponse;
import org.springframework.data.domain.Pageable;

public interface DiscussionService {
    DiscussionResponse createDiscussion(DiscussionRequest request);
    DiscussionResponse getDiscussionById(String id);
    ListResponse<DiscussionResponse> getDiscussionTreeByLesson(String lessonId, Pageable pageable);
    ListResponse<DiscussionResponse> getDiscussionTreeBySection(String sectionId, Pageable pageable);
    DiscussionResponse updateDiscussion(String id, String content);
    void deleteDiscussion(String discussionId);
    DiscussionResponse likeDiscussion(String id);
    DiscussionResponse dislikeDiscussion(String id);
}
