package com.hust.assessmentservice.mapper;

import com.hust.assessmentservice.dto.response.QuizAttemptResponse;
import com.hust.assessmentservice.dto.request.QuizSubmitRequest;
import com.hust.assessmentservice.entity.QuizAttempt;
import com.hust.commonlibrary.mapper.BaseMapper;
import com.hust.commonlibrary.mapper.GlobalMapperConfiguration;
import org.mapstruct.Mapper;

@Mapper(config = GlobalMapperConfiguration.class, uses = {UserAnswerMapper.class})
public interface QuizAttemptMapper extends BaseMapper<QuizAttempt, QuizSubmitRequest, QuizAttemptResponse> {
}
