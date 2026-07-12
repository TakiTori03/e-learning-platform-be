package com.hust.mediaservice.mapper;

import com.hust.commonlibrary.mapper.BaseMapper;
import com.hust.commonlibrary.mapper.GlobalMapperConfiguration;
import com.hust.mediaservice.dto.response.MediaResponse;
import com.hust.mediaservice.entity.Media;
import org.mapstruct.Mapper;

@Mapper(config = GlobalMapperConfiguration.class, componentModel = "spring")
public interface MediaMapper extends BaseMapper<Media, Object, MediaResponse> {
}
