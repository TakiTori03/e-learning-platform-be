package com.hust.assessmentservice.mapper;

import com.hust.assessmentservice.dto.request.AnswerOptionRequest;
import com.hust.assessmentservice.dto.response.AnswerOptionResponse;
import com.hust.assessmentservice.entity.AnswerOption;
import com.hust.commonlibrary.mapper.BaseMapper;
import com.hust.commonlibrary.mapper.GlobalMapperConfiguration;
import org.mapstruct.Mapper;

@Mapper(config = GlobalMapperConfiguration.class, componentModel = "spring")
public interface AnswerOptionMapper extends BaseMapper<AnswerOption, AnswerOptionRequest, AnswerOptionResponse> {
}
