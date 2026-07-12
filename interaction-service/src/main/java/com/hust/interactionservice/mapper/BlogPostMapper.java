package com.hust.interactionservice.mapper;

import com.hust.commonlibrary.mapper.BaseMapper;
import com.hust.commonlibrary.mapper.GlobalMapperConfiguration;
import com.hust.interactionservice.dto.request.BlogPostRequest;
import com.hust.interactionservice.dto.response.BlogPostResponse;
import com.hust.interactionservice.entity.BlogPost;
import org.mapstruct.Mapper;

@Mapper(config = GlobalMapperConfiguration.class, componentModel = "spring")
public interface BlogPostMapper extends BaseMapper<BlogPost, BlogPostRequest, BlogPostResponse> {
}
