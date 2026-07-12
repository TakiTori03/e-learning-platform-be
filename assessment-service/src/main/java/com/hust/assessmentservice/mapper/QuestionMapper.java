package com.hust.assessmentservice.mapper;

import com.hust.assessmentservice.dto.request.QuestionRequest;
import com.hust.assessmentservice.dto.response.QuestionResponse;
import com.hust.assessmentservice.entity.Question;
import com.hust.commonlibrary.mapper.BaseMapper;
import com.hust.commonlibrary.mapper.GlobalMapperConfiguration;
import org.mapstruct.Mapper;

@Mapper(config = GlobalMapperConfiguration.class, uses = {AnswerOptionMapper.class})
public interface QuestionMapper extends BaseMapper<Question, QuestionRequest, QuestionResponse> {
}
