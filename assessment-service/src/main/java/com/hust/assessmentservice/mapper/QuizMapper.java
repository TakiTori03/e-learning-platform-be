package com.hust.assessmentservice.mapper;

import com.hust.assessmentservice.dto.request.QuizRequest;
import com.hust.assessmentservice.dto.response.QuizResponse;
import com.hust.assessmentservice.dto.response.QuestionResponse;
import com.hust.assessmentservice.dto.response.AnswerOptionResponse;
import com.hust.assessmentservice.entity.Quiz;
import com.hust.commonlibrary.mapper.BaseMapper;
import com.hust.commonlibrary.mapper.GlobalMapperConfiguration;
import org.mapstruct.AfterMapping;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(config = GlobalMapperConfiguration.class, uses = {QuestionMapper.class})
public interface QuizMapper extends BaseMapper<Quiz, QuizRequest, QuizResponse> {
    
    QuizResponse entityToResponse(Quiz quiz, @Context boolean isStudent);

    @AfterMapping
    default void clearCorrectAnswers(Quiz quiz, @MappingTarget QuizResponse response, @Context boolean isStudent) {
        if (isStudent && response != null && response.getQuestions() != null) {
            for (QuestionResponse q : response.getQuestions()) {
                if (q.getOptions() != null) {
                    for (AnswerOptionResponse o : q.getOptions()) {
                        o.setIsCorrect(null);
                    }
                }
            }
        }
    }
}
