package com.hust.learningservice.mapper;

import com.hust.commonlibrary.mapper.BaseMapper;
import com.hust.commonlibrary.mapper.GlobalMapperConfiguration;
import com.hust.learningservice.dto.request.DiscussionRequest;
import com.hust.learningservice.dto.response.DiscussionResponse;
import com.hust.learningservice.entity.Discussion;
import org.mapstruct.Mapper;

@Mapper(config = GlobalMapperConfiguration.class, componentModel = "spring")
public interface DiscussionMapper extends BaseMapper<Discussion, DiscussionRequest, DiscussionResponse> {
}
