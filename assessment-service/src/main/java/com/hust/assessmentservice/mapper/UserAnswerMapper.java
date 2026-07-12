package com.hust.assessmentservice.mapper;

import com.hust.assessmentservice.dto.request.UserAnswerRequest;
import com.hust.assessmentservice.entity.UserAnswer;
import com.hust.commonlibrary.mapper.BaseMapper;
import com.hust.commonlibrary.mapper.GlobalMapperConfiguration;
import org.mapstruct.Mapper;

@Mapper(config = GlobalMapperConfiguration.class, componentModel = "spring")
public interface UserAnswerMapper extends BaseMapper<UserAnswer, UserAnswerRequest, UserAnswerRequest> {
}
