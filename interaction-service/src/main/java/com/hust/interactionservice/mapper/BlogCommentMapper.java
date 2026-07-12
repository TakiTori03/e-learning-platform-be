package com.hust.interactionservice.mapper;

import com.hust.commonlibrary.mapper.BaseMapper;
import com.hust.commonlibrary.mapper.GlobalMapperConfiguration;
import com.hust.interactionservice.dto.request.BlogCommentRequest;
import com.hust.interactionservice.dto.response.BlogCommentResponse;
import com.hust.interactionservice.entity.BlogComment;
import org.mapstruct.Mapper;

@Mapper(config = GlobalMapperConfiguration.class, componentModel = "spring")
public interface BlogCommentMapper extends BaseMapper<BlogComment, BlogCommentRequest, BlogCommentResponse> {
}
