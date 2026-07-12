package com.hust.interactionservice.mapper;

import com.hust.commonlibrary.mapper.BaseMapper;
import com.hust.commonlibrary.mapper.GlobalMapperConfiguration;
import com.hust.interactionservice.dto.request.ReviewRequest;
import com.hust.interactionservice.dto.response.ReviewResponse;
import com.hust.interactionservice.entity.Review;
import org.mapstruct.Mapper;

@Mapper(config = GlobalMapperConfiguration.class, componentModel = "spring")
public interface ReviewMapper extends BaseMapper<Review, ReviewRequest, ReviewResponse> {
}
