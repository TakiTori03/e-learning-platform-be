package com.hust.courseservice.mapper;

import com.hust.commonlibrary.mapper.BaseMapper;
import com.hust.courseservice.dto.request.LessonRequest;
import com.hust.courseservice.dto.response.LessonResponse;
import com.hust.courseservice.entity.Lesson;
import com.hust.commonlibrary.mapper.GlobalMapperConfiguration;
import org.mapstruct.Mapper;

@Mapper(config = GlobalMapperConfiguration.class, componentModel = "spring")
public interface LessonMapper extends BaseMapper<Lesson, LessonRequest, LessonResponse> {
}
