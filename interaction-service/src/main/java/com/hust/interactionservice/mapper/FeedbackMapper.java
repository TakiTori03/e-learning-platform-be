package com.hust.interactionservice.mapper;

import com.hust.commonlibrary.mapper.BaseMapper;
import com.hust.commonlibrary.mapper.GlobalMapperConfiguration;
import com.hust.interactionservice.dto.request.FeedbackRequest;
import com.hust.interactionservice.dto.response.FeedbackResponse;
import com.hust.interactionservice.entity.Feedback;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = GlobalMapperConfiguration.class, componentModel = "spring")
public interface FeedbackMapper extends BaseMapper<Feedback, FeedbackRequest, FeedbackResponse> {
    
    @Override
    @Mapping(target = "replies", ignore = true)
    FeedbackResponse entityToResponse(Feedback entity);
}

