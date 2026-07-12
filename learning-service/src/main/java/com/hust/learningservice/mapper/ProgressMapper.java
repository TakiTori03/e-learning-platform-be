package com.hust.learningservice.mapper;

import com.hust.commonlibrary.mapper.BaseMapper;
import com.hust.commonlibrary.mapper.GlobalMapperConfiguration;
import com.hust.learningservice.dto.response.CourseProgressResponse;
import com.hust.learningservice.entity.StudentEnrollment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = GlobalMapperConfiguration.class, componentModel = "spring")
public interface ProgressMapper extends BaseMapper<StudentEnrollment, Object, CourseProgressResponse> {

    @Override
    @Mapping(target = "isEnrolled", constant = "true")
    CourseProgressResponse entityToResponse(StudentEnrollment enrollment);
}
