package com.hust.interactionservice.mapper;

import com.hust.commonlibrary.mapper.BaseMapper;
import com.hust.commonlibrary.mapper.GlobalMapperConfiguration;
import com.hust.interactionservice.dto.request.BlogTopicRequest;
import com.hust.interactionservice.dto.response.BlogTopicResponse;
import com.hust.interactionservice.entity.BlogTopic;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = GlobalMapperConfiguration.class, componentModel = "spring")
public interface BlogTopicMapper extends BaseMapper<BlogTopic, BlogTopicRequest, BlogTopicResponse> {

    @Override
    @Mapping(target = "postCount", ignore = true)
    BlogTopicResponse entityToResponse(BlogTopic entity);
}
