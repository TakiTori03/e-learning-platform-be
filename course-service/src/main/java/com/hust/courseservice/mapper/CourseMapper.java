package com.hust.courseservice.mapper;

import com.hust.commonlibrary.mapper.BaseMapper;
import com.hust.commonlibrary.mapper.GlobalMapperConfiguration;
import com.hust.courseservice.dto.request.CourseRequest;
import com.hust.courseservice.dto.response.CourseResponse;
import com.hust.courseservice.entity.Course;
import org.mapstruct.Mapper;

@Mapper(config = GlobalMapperConfiguration.class, uses = {CategoryMapper.class})
public interface CourseMapper extends BaseMapper<Course, CourseRequest, CourseResponse> {

}
