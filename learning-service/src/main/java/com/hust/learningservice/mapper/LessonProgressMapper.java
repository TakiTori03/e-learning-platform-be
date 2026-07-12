package com.hust.learningservice.mapper;

import com.hust.commonlibrary.mapper.BaseMapper;
import com.hust.commonlibrary.mapper.GlobalMapperConfiguration;
import com.hust.learningservice.dto.request.LessonProgressRequest;
import com.hust.learningservice.entity.LessonProgress;
import org.mapstruct.Mapper;


@Mapper(config = GlobalMapperConfiguration.class, componentModel = "spring")
public interface LessonProgressMapper extends BaseMapper<LessonProgress, LessonProgressRequest, Object> {

}
