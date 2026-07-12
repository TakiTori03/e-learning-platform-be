package com.hust.courseservice.mapper;

import com.hust.commonlibrary.mapper.BaseMapper;
import com.hust.courseservice.dto.request.SectionRequest;
import com.hust.courseservice.dto.response.SectionResponse;
import com.hust.courseservice.entity.Section;
import com.hust.commonlibrary.mapper.GlobalMapperConfiguration;
import org.mapstruct.Mapper;

@Mapper(config = GlobalMapperConfiguration.class, componentModel = "spring")
public interface SectionMapper extends BaseMapper<Section, SectionRequest, SectionResponse> {
}
