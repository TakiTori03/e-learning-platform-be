package com.hust.learningservice.service.impl;

import com.hust.commonlibrary.constants.RedisPrefixConstants;
import com.hust.commonlibrary.dto.ListResponse;
import com.hust.commonlibrary.dto.UserSharedProfile;
import com.hust.commonlibrary.service.RedisService;
import com.hust.commonlibrary.utils.SecurityUtils;
import com.hust.learningservice.dto.request.DiscussionRequest;
import com.hust.learningservice.dto.response.DiscussionResponse;
import com.hust.learningservice.entity.Discussion;
import com.hust.learningservice.mapper.DiscussionMapper;
import com.hust.learningservice.repository.DiscussionRepository;
import com.hust.learningservice.service.DiscussionService;
import com.hust.learningservice.utils.AppUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DiscussionServiceImpl implements DiscussionService {

    private final DiscussionRepository discussionRepository;
    private final DiscussionMapper discussionMapper;
    private final RedisService redisService;
    private final SimpMessagingTemplate messagingTemplate;

    @Override
    public DiscussionResponse createDiscussion(DiscussionRequest request) {
        String userId = SecurityUtils.getCurrentUserIdOrThrow();
        Discussion discussion = discussionMapper.requestToEntity(request);
        discussion.setUserId(userId);
        discussion.setCode(AppUtils.generateCode("DISCUSS"));

        if (discussion.getParentId() != null) {
            Discussion parent = discussionRepository.findById(discussion.getParentId())
                    .orElseThrow(() -> new RuntimeException("Parent not found"));
            discussion.setRootId(parent.getRootId() != null ? parent.getRootId() : parent.getId());
            discussion.setCourseId(parent.getCourseId());
            discussion.setSectionId(parent.getSectionId());
            discussion.setLessonId(parent.getLessonId());
        }

        DiscussionResponse response = discussionMapper.entityToResponse(discussionRepository.save(discussion));
        enrichProfiles(java.util.Collections.singletonList(response));

        try {
            String destination = "/topic/lesson." + response.getLessonId();
            messagingTemplate.convertAndSend(destination, response);
            log.info("Successfully broadcasted new discussion over WebSocket to destination: {}", destination);
        } catch (Exception e) {
            log.error("Failed to broadcast discussion creation over WebSocket: {}", e.getMessage(), e);
        }

        return response;
    }

    @Override
    public DiscussionResponse getDiscussionById(String id) {
        DiscussionResponse response = discussionRepository.findById(id)
                .map(discussionMapper::entityToResponse)
                .orElseThrow(() -> new RuntimeException("Discussion not found"));
        response.setReplyCount((int) discussionRepository.countByParentId(id));
        enrichProfiles(java.util.Collections.singletonList(response));
        return response;
    }

    @Override
    public ListResponse<DiscussionResponse> getDiscussionTreeByLesson(String lessonId, Pageable pageable) {
        Page<Discussion> rootPage = discussionRepository.findByLessonIdAndParentIdIsNull(lessonId, pageable);
        return buildPagedTree(rootPage);
    }

    @Override
    public ListResponse<DiscussionResponse> getDiscussionTreeBySection(String sectionId, Pageable pageable) {
        Page<Discussion> rootPage = discussionRepository.findBySectionIdAndParentIdIsNull(sectionId, pageable);
        return buildPagedTree(rootPage);
    }

    @Override
    public DiscussionResponse updateDiscussion(String id, String content) {
        Discussion discussion = discussionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Discussion not found"));

        String currentUserId = SecurityUtils.getCurrentUserIdOrThrow();
        if (!discussion.getUserId().equals(currentUserId)) {
            throw new RuntimeException("Access denied: You are not authorized to update this discussion!");
        }

        discussion.setContent(content);
        DiscussionResponse response = discussionMapper.entityToResponse(discussionRepository.save(discussion));
        enrichProfiles(java.util.Collections.singletonList(response));

        try {
            String destination = "/topic/lesson." + response.getLessonId();
            messagingTemplate.convertAndSend(destination, response);
            log.info("Successfully broadcasted updated discussion over WebSocket to destination: {}", destination);
        } catch (Exception e) {
            log.error("Failed to broadcast discussion update over WebSocket: {}", e.getMessage(), e);
        }

        return response;
    }

    private ListResponse<DiscussionResponse> buildPagedTree(Page<Discussion> rootPage) {
        List<String> rootIds = rootPage.getContent().stream()
                .map(Discussion::getId)
                .toList();

        List<Discussion> allRelated = discussionRepository.findByRootIdIn(rootIds);

        List<Discussion> combined = new ArrayList<>(rootPage.getContent());
        combined.addAll(allRelated);

        List<DiscussionResponse> allResponses = combined.stream()
                .map(discussionMapper::entityToResponse)
                .toList();

        Map<String, List<DiscussionResponse>> groupedByParent = allResponses.stream()
                .filter(res -> res.getParentId() != null)
                .collect(Collectors.groupingBy(DiscussionResponse::getParentId));

        List<DiscussionResponse> rootResponses = allResponses.stream()
                .filter(res -> rootIds.contains(res.getId()))
                .toList();

        allResponses.forEach(res -> {
            List<DiscussionResponse> replies = groupedByParent.get(res.getId());
            res.setReplies(replies);
            res.setReplyCount(replies != null ? replies.size() : 0);
        });

        ListResponse<DiscussionResponse> listResponse = ListResponse.of(rootResponses, rootPage);
        enrichProfiles(listResponse.getContent());
        return listResponse;
    }

    @Override
    @Transactional
    public void deleteDiscussion(String discussionId) {
        Discussion discussion = discussionRepository.findById(discussionId)
                .orElseThrow(() -> new RuntimeException("Discussion not found"));

        String currentUserId = SecurityUtils.getCurrentUserIdOrThrow();
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        String courseOwnerKey = RedisPrefixConstants.getSharedCourseOwnerKey(discussion.getCourseId());
        String courseOwnerId = (String) redisService.get(courseOwnerKey);
        boolean isCourseOwner = courseOwnerId != null && courseOwnerId.equals(currentUserId);

        if (!discussion.getUserId().equals(currentUserId) && !isAdmin && !isCourseOwner) {
            throw new RuntimeException("Access denied: You are not authorized to delete this discussion!");
        }

        // Clean up replies to avoid orphan records in MongoDB
        if (discussion.getParentId() == null) {
            discussionRepository.deleteByRootId(discussionId);
            log.info("Cascaded delete: cleaned up all replies with rootId={}", discussionId);
        } else {
            discussionRepository.deleteByParentId(discussionId);
            log.info("Cascaded delete: cleaned up child replies with parentId={}", discussionId);
        }

        discussionRepository.deleteById(discussionId);
    }

    @Override
    public DiscussionResponse likeDiscussion(String id) {
        Discussion discussion = discussionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Discussion not found"));

        String userId = SecurityUtils.getCurrentUserIdOrThrow();

        if (discussion.getLikedUserIds() == null) {
            discussion.setLikedUserIds(new ArrayList<>());
        }
        if (discussion.getDislikedUserIds() == null) {
            discussion.setDislikedUserIds(new ArrayList<>());
        }

        if (discussion.getLikedUserIds().contains(userId)) {
            discussion.getLikedUserIds().remove(userId);
        } else {
            discussion.getLikedUserIds().add(userId);
            discussion.getDislikedUserIds().remove(userId);
        }

        DiscussionResponse response = discussionMapper.entityToResponse(discussionRepository.save(discussion));
        response.setReplyCount((int) discussionRepository.countByParentId(discussion.getId()));
        enrichProfiles(java.util.Collections.singletonList(response));

        try {
            String destination = "/topic/lesson." + response.getLessonId();
            messagingTemplate.convertAndSend(destination, response);
            log.info("Successfully broadcasted discussion like/unlike over WebSocket to destination: {}", destination);
        } catch (Exception e) {
            log.error("Failed to broadcast discussion like update over WebSocket: {}", e.getMessage(), e);
        }

        return response;
    }

    @Override
    public DiscussionResponse dislikeDiscussion(String id) {
        Discussion discussion = discussionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Discussion not found"));

        String userId = SecurityUtils.getCurrentUserIdOrThrow();

        if (discussion.getLikedUserIds() == null) {
            discussion.setLikedUserIds(new ArrayList<>());
        }
        if (discussion.getDislikedUserIds() == null) {
            discussion.setDislikedUserIds(new ArrayList<>());
        }

        if (discussion.getDislikedUserIds().contains(userId)) {
            discussion.getDislikedUserIds().remove(userId);
        } else {
            discussion.getDislikedUserIds().add(userId);
            discussion.getLikedUserIds().remove(userId);
        }

        DiscussionResponse response = discussionMapper.entityToResponse(discussionRepository.save(discussion));
        response.setReplyCount((int) discussionRepository.countByParentId(discussion.getId()));
        enrichProfiles(java.util.Collections.singletonList(response));

        try {
            String destination = "/topic/lesson." + response.getLessonId();
            messagingTemplate.convertAndSend(destination, response);
            log.info("Successfully broadcasted discussion dislike/undislike over WebSocket to destination: {}", destination);
        } catch (Exception e) {
            log.error("Failed to broadcast discussion dislike update over WebSocket: {}", e.getMessage(), e);
        }

        return response;
    }

    private void enrichProfiles(List<DiscussionResponse> responses) {
        if (responses == null || responses.isEmpty()) return;

        List<String> userIds = new java.util.ArrayList<>();
        collectUserIds(responses, userIds);

        List<String> distinctUserIds = userIds.stream().distinct().toList();
        List<String> redisKeys = distinctUserIds.stream()
                .map(RedisPrefixConstants::getSharedUserProfileKey)
                .toList();

        try {
            List<Object> cachedProfiles = redisService.multiGet(redisKeys);
            Map<String, UserSharedProfile> profileMap = new java.util.HashMap<>();

            for (int i = 0; i < distinctUserIds.size(); i++) {
                String uId = distinctUserIds.get(i);
                Object val = cachedProfiles != null && i < cachedProfiles.size() ? cachedProfiles.get(i) : null;
                if (val != null) {
                    if (val instanceof UserSharedProfile profile) {
                        profileMap.put(uId, profile);
                    } else {
                        UserSharedProfile profile = new com.fasterxml.jackson.databind.ObjectMapper()
                                .convertValue(val, UserSharedProfile.class);
                        profileMap.put(uId, profile);
                    }
                }
            }

            populateProfiles(responses, profileMap);
        } catch (Exception e) {
            log.error("Failed to enrich discussion user profiles from Redis: {}", e.getMessage(), e);
        }
    }

    private void collectUserIds(List<DiscussionResponse> responses, List<String> userIds) {
        for (DiscussionResponse res : responses) {
            if (res.getUserId() != null) {
                userIds.add(res.getUserId());
            }
            if (res.getReplies() != null && !res.getReplies().isEmpty()) {
                collectUserIds(res.getReplies(), userIds);
            }
        }
    }

    private void populateProfiles(List<DiscussionResponse> responses, Map<String, UserSharedProfile> profileMap) {
        for (DiscussionResponse res : responses) {
            UserSharedProfile profile = profileMap.get(res.getUserId());
            if (profile != null) {
                res.setUser(profile);
            }
            if (res.getReplies() != null && !res.getReplies().isEmpty()) {
                populateProfiles(res.getReplies(), profileMap);
            }
        }
    }
}
