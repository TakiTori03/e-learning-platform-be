package com.hust.interactionservice.mapper;

import com.hust.commonlibrary.mapper.BaseMapper;
import com.hust.commonlibrary.mapper.GlobalMapperConfiguration;
import com.hust.interactionservice.dto.response.ReviewResponse;
import com.hust.interactionservice.entity.ReviewReply;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = GlobalMapperConfiguration.class, componentModel = "spring")
public interface ReviewReplyMapper extends BaseMapper<ReviewReply, Object, ReviewResponse> {
    
    @Override
    @Mapping(target = "ratingStar", ignore = true)
    @Mapping(target = "title", ignore = true)
    ReviewResponse entityToResponse(ReviewReply entity);
}
