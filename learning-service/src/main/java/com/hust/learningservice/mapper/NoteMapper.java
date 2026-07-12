package com.hust.learningservice.mapper;

import com.hust.commonlibrary.mapper.BaseMapper;
import com.hust.commonlibrary.mapper.GlobalMapperConfiguration;
import com.hust.learningservice.dto.request.CourseNoteRequest;
import com.hust.learningservice.dto.response.CourseNoteResponse;
import com.hust.learningservice.entity.CourseNote;
import org.mapstruct.Mapper;

@Mapper(config = GlobalMapperConfiguration.class, componentModel = "spring")
public interface NoteMapper extends BaseMapper<CourseNote, CourseNoteRequest, CourseNoteResponse> {

}
