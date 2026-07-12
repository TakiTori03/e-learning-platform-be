package com.hust.learningservice.resolver;

import com.hust.commonlibrary.resolver.CourseIdResolver;
import com.hust.learningservice.entity.Discussion;
import com.hust.learningservice.repository.DiscussionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component("discussionResolver")
@RequiredArgsConstructor
public class DiscussionCourseIdResolver implements CourseIdResolver {

    private final DiscussionRepository discussionRepository;

    @Override
    public String resolveCourseId(String domainId) {
        if (domainId == null || domainId.isBlank()) return null;

        return discussionRepository.findById(domainId)
                .map(Discussion::getCourseId)
                .orElse(null);
    }
}
