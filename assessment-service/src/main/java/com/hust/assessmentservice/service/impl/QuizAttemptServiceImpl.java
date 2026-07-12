package com.hust.assessmentservice.service.impl;

import com.hust.assessmentservice.dto.response.QuizAttemptResponse;
import com.hust.assessmentservice.dto.request.QuizSubmitRequest;
import com.hust.assessmentservice.dto.request.UserAnswerRequest;
import com.hust.assessmentservice.entity.AnswerOption;
import com.hust.assessmentservice.entity.Question;
import com.hust.assessmentservice.entity.Quiz;
import com.hust.assessmentservice.entity.QuizAttempt;
import com.hust.assessmentservice.entity.UserAnswer;
import com.hust.assessmentservice.repository.QuizAttemptRepository;
import com.hust.assessmentservice.repository.QuizRepository;
import com.hust.assessmentservice.service.QuizAttemptService;
import com.hust.commonlibrary.event.AssessmentSubmittedEvent;
import com.hust.commonlibrary.exception.payload.ResourceNotFoundException;
import com.hust.commonlibrary.exception.payload.InvalidParamException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.hust.assessmentservice.dto.response.QuizStartResponse;
import com.hust.assessmentservice.dto.response.StudentQuestionDTO;
import com.hust.assessmentservice.dto.response.StudentAnswerOptionDTO;
import com.hust.assessmentservice.entity.AttemptStatus;

@Service
@RequiredArgsConstructor
@Slf4j
public class QuizAttemptServiceImpl implements QuizAttemptService {

    private final QuizRepository quizRepository;
    private final QuizAttemptRepository quizAttemptRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public QuizStartResponse startQuiz(String userId, String quizId) {
        log.info("Starting quiz for userId={}, quizId={}", userId, quizId);
        
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new ResourceNotFoundException("Quiz not found with id: " + quizId));

        // 1. Look for existing IN_PROGRESS attempt & clean up if expired
        List<QuizAttempt> existingAttempts = quizAttemptRepository.findAllByUserIdAndQuizIdOrderBySubmittedAtDesc(userId, quizId);
        QuizAttempt activeAttempt = null;
        for (QuizAttempt attempt : existingAttempts) {
            if (AttemptStatus.IN_PROGRESS.equals(attempt.getStatus())) {
                Instant startedAt = attempt.getStartedAt() != null ? attempt.getStartedAt() : attempt.getCreatedAt();
                if (quiz.getTimeLimitMinutes() != null && quiz.getTimeLimitMinutes() > 0 && startedAt != null) {
                    Instant expiresAt = startedAt.plus(java.time.Duration.ofMinutes(quiz.getTimeLimitMinutes())).plus(java.time.Duration.ofMinutes(2)); // 2 mins grace
                    if (Instant.now().isAfter(expiresAt)) {
                        attempt.setStatus(AttemptStatus.ABANDONED);
                        quizAttemptRepository.save(attempt);
                        continue;
                    }
                }
                activeAttempt = attempt;
                break;
            }
        }

        if (activeAttempt != null) {
            return toQuizStartResponse(activeAttempt, quiz);
        }

        // 2. Validate max attempts
        if (quiz.getMaxAttempts() != null && quiz.getMaxAttempts() > 0) {
            long attemptCount = quizAttemptRepository.countByUserIdAndQuizId(userId, quizId);
            if (attemptCount >= quiz.getMaxAttempts()) {
                throw new InvalidParamException("Bạn đã vượt quá số lần làm bài cho phép của bài kiểm tra này (tối đa " + quiz.getMaxAttempts() + " lần).");
            }
        }

        // 3. Create new Attempt
        QuizAttempt attempt = QuizAttempt.builder()
                .userId(userId)
                .quizId(quizId)
                .courseId(quiz.getCourseId())
                .status(AttemptStatus.IN_PROGRESS)
                .startedAt(Instant.now())
                .timeLimitSnapshot(quiz.getTimeLimitMinutes())
                .quizType(quiz.getType())
                .build();
                
        attempt = quizAttemptRepository.save(attempt);
        return toQuizStartResponse(attempt, quiz);
    }

    private QuizStartResponse toQuizStartResponse(QuizAttempt attempt, Quiz quiz) {
        List<StudentQuestionDTO> questionDTOs = new ArrayList<>();
        if (quiz.getQuestions() != null) {
            for (Question q : quiz.getQuestions()) {
                List<StudentAnswerOptionDTO> options = new ArrayList<>();
                if (q.getOptions() != null) {
                    for (AnswerOption opt : q.getOptions()) {
                        options.add(StudentAnswerOptionDTO.builder()
                                .id(opt.getId())
                                .optionText(opt.getOptionText())
                                .build());
                    }
                }
                questionDTOs.add(StudentQuestionDTO.builder()
                        .id(q.getId())
                        .questionText(q.getQuestionText())
                        .type(q.getType())
                        .options(options)
                        .scoreWeight(q.getScoreWeight())
                        .build());
            }
        }

        return QuizStartResponse.builder()
                .attemptId(attempt.getId())
                .startedAt(attempt.getStartedAt())
                .timeLimitMinutes(attempt.getTimeLimitSnapshot())
                .questions(questionDTOs)
                .quizTitle(quiz.getTitle())
                .passingScorePercentage(quiz.getPassingScorePercentage())
                .build();
    }

    @Override
    @Transactional
    public QuizAttemptResponse submitQuiz(String userId, String attemptId, QuizSubmitRequest request) {
        log.info("Processing quiz submission for userId={}, attemptId={}", userId, attemptId);
        
        QuizAttempt attempt = quizAttemptRepository.findById(attemptId)
                .orElseThrow(() -> new ResourceNotFoundException("Lượt thi không tồn tại: " + attemptId));

        if (!attempt.getUserId().equals(userId)) {
            throw new InvalidParamException("Bạn không có quyền nộp bài cho lượt thi này.");
        }

        if (AttemptStatus.SUBMITTED.equals(attempt.getStatus())) {
            throw new InvalidParamException("Lượt thi này đã được nộp.");
        }

        if (AttemptStatus.ABANDONED.equals(attempt.getStatus())) {
            throw new InvalidParamException("Lượt thi này đã bị huỷ hoặc quá thời gian làm bài.");
        }

        Quiz quiz = quizRepository.findById(attempt.getQuizId())
                .orElseThrow(() -> new ResourceNotFoundException("Quiz not found"));

        List<UserAnswerRequest> submittedAnswers = request.getSubmittedAnswers() != null ? 
                request.getSubmittedAnswers() : List.of();

        // Map submitted answers by questionId for fast lookup
        Map<String, List<String>> userAnsMap = submittedAnswers.stream()
                .collect(Collectors.toMap(
                        UserAnswerRequest::getQuestionId,
                        ans -> ans.getSelectedOptionIds() != null ? ans.getSelectedOptionIds() : List.of(),
                        (existing, replacement) -> replacement // Keep latest if duplicate
                ));

        double totalWeight = 0.0;
        double earnedWeight = 0.0;
        int correctCount = 0;

        List<UserAnswer> finalSubmittedAnswers = new ArrayList<>();

        if (quiz.getQuestions() != null) {
            for (Question question : quiz.getQuestions()) {
                double weight = question.getScoreWeight() != null ? question.getScoreWeight() : 1.0;
                totalWeight += weight;

                List<String> selectedOptionIds = userAnsMap.getOrDefault(question.getId(), List.of());

                // Extract correct option IDs
                Set<String> correctOptionIds = question.getOptions() != null ? question.getOptions().stream()
                        .filter(opt -> opt.getIsCorrect() != null && opt.getIsCorrect())
                        .map(AnswerOption::getId)
                        .collect(Collectors.toSet()) : Set.of();

                // Validate answer correctness
                Set<String> userSelectedSet = Set.copyOf(selectedOptionIds);
                boolean isQuestionCorrect = false;
                if (!correctOptionIds.isEmpty() && userSelectedSet.equals(correctOptionIds)) {
                    earnedWeight += weight;
                    correctCount++;
                    isQuestionCorrect = true;
                }

                // Map to persistent entity structure
                finalSubmittedAnswers.add(UserAnswer.builder()
                        .questionId(question.getId())
                        .selectedOptionIds(selectedOptionIds)
                        .isCorrect(isQuestionCorrect)
                        .build());
            }
        }

        // Calculate final score in scale of 100
        double score = totalWeight > 0.0 ? (earnedWeight / totalWeight) * 100.0 : 0.0;
        score = Math.round(score * 100.0) / 100.0;
        boolean isPassed = score >= (quiz.getPassingScorePercentage() != null ? quiz.getPassingScorePercentage() : 50.0);

        attempt.setSubmittedAnswers(finalSubmittedAnswers);
        attempt.setScore(score);
        attempt.setIsPassed(isPassed);
        attempt.setStatus(AttemptStatus.SUBMITTED);
        attempt.setSubmittedAt(Instant.now());
        if (request.getViolationCount() != null) {
            attempt.setViolationCount(request.getViolationCount());
        }

        QuizAttempt savedAttempt = quizAttemptRepository.save(attempt);
        log.info("QuizAttempt saved successfully. id={}, score={}, isPassed={}", savedAttempt.getId(), score, isPassed);

        // Publish event to Kafka for other services (e.g. learning-service) to process
        publishAssessmentSubmittedEvent(savedAttempt, quiz.getLessonId());

        return toAttemptResponse(savedAttempt, quiz);
    }

    @Override
    public List<QuizAttemptResponse> getAttemptsByUserIdAndQuizId(String userId, String quizId) {
        log.info("Fetching attempts for userId={}, quizId={}", userId, quizId);
        Quiz quiz = quizRepository.findById(quizId).orElse(null);
        return quizAttemptRepository.findAllByUserIdAndQuizIdOrderBySubmittedAtDesc(userId, quizId).stream()
                .map(attempt -> toAttemptResponse(attempt, quiz))
                .collect(Collectors.toList());
    }

    @Override
    public List<QuizAttemptResponse> getAttemptsByUserIdAndCourseId(String userId, String courseId) {
        log.info("Fetching attempts for userId={}, courseId={}", userId, courseId);
        java.util.Map<String, Quiz> quizMap = new java.util.HashMap<>();
        return quizAttemptRepository.findAllByUserIdAndCourseIdOrderBySubmittedAtDesc(userId, courseId).stream()
                .map(attempt -> {
                    Quiz quiz = quizMap.computeIfAbsent(attempt.getQuizId(), id -> quizRepository.findById(id).orElse(null));
                    return toAttemptResponse(attempt, quiz);
                })
                .collect(Collectors.toList());
    }

    private void publishAssessmentSubmittedEvent(QuizAttempt attempt, String lessonId) {
        try {
            AssessmentSubmittedEvent event = AssessmentSubmittedEvent.builder()
                    .attemptId(attempt.getId())
                    .userId(attempt.getUserId())
                    .quizId(attempt.getQuizId())
                    .courseId(attempt.getCourseId())
                    .lessonId(lessonId)
                    .score(attempt.getScore())
                    .isPassed(attempt.getIsPassed())
                    .quizType(attempt.getQuizType())
                    .submittedAt(attempt.getSubmittedAt())
                    .build();

            log.info("Publishing local AssessmentSubmittedEvent: attemptId={}, isPassed={}", event.getAttemptId(), event.getIsPassed());
            eventPublisher.publishEvent(event);
        } catch (Exception e) {
            log.error("Failed to publish local AssessmentSubmittedEvent for attemptId: " + attempt.getId(), e);
        }
    }

    private QuizAttemptResponse toAttemptResponse(QuizAttempt attempt, Quiz quiz) {
        List<UserAnswer> answers = attempt.getSubmittedAnswers();
        if (answers != null && quiz != null && quiz.getQuestions() != null) {
            Map<String, Question> questionMap = quiz.getQuestions().stream()
                    .collect(Collectors.toMap(Question::getId, q -> q, (existing, replacing) -> replacing));

            answers = answers.stream().map(ans -> {
                Boolean isCorrect = ans.getIsCorrect();
                if (isCorrect == null) {
                    Question question = questionMap.get(ans.getQuestionId());
                    if (question != null) {
                        Set<String> correctOptionIds = question.getOptions().stream()
                                .filter(opt -> opt.getIsCorrect() != null && opt.getIsCorrect())
                                .map(AnswerOption::getId)
                                .collect(Collectors.toSet());
                        Set<String> userSelectedSet = ans.getSelectedOptionIds() != null 
                                ? Set.copyOf(ans.getSelectedOptionIds()) 
                                : Set.of();
                        isCorrect = !correctOptionIds.isEmpty() && userSelectedSet.equals(correctOptionIds);
                    } else {
                        isCorrect = false;
                    }
                }
                return UserAnswer.builder()
                        .questionId(ans.getQuestionId())
                        .selectedOptionIds(ans.getSelectedOptionIds())
                        .isCorrect(isCorrect)
                        .build();
            }).collect(Collectors.toList());
        }

        long attemptsUsed = quizAttemptRepository.countByUserIdAndQuizId(attempt.getUserId(), attempt.getQuizId());

        return QuizAttemptResponse.builder()
                .id(attempt.getId())
                .userId(attempt.getUserId())
                .quizId(attempt.getQuizId())
                .courseId(attempt.getCourseId())
                .score(attempt.getScore())
                .isPassed(attempt.getIsPassed())
                .quizType(quiz != null ? quiz.getType() : null)
                .submittedAt(attempt.getSubmittedAt())
                .submittedAnswers(answers)
                .maxAttempts(quiz != null ? quiz.getMaxAttempts() : null)
                .attemptsUsed(attemptsUsed)
                .build();
    }

    @Override
    @Transactional
    public com.hust.assessmentservice.dto.response.QuizEligibilityResponse getQuizEligibility(String userId, String quizId) {
        log.info("Fetching quiz eligibility for userId={}, quizId={}", userId, quizId);
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new ResourceNotFoundException("Quiz not found with id: " + quizId));

        List<QuizAttempt> attempts = quizAttemptRepository.findAllByUserIdAndQuizIdOrderBySubmittedAtDesc(userId, quizId);
        boolean changed = false;
        for (QuizAttempt attempt : attempts) {
            if (AttemptStatus.IN_PROGRESS.equals(attempt.getStatus())) {
                Instant startedAt = attempt.getStartedAt() != null ? attempt.getStartedAt() : attempt.getCreatedAt();
                if (quiz.getTimeLimitMinutes() != null && quiz.getTimeLimitMinutes() > 0 && startedAt != null) {
                    Instant expiresAt = startedAt.plus(java.time.Duration.ofMinutes(quiz.getTimeLimitMinutes())).plus(java.time.Duration.ofMinutes(2)); // 2 mins grace
                    if (Instant.now().isAfter(expiresAt)) {
                        attempt.setStatus(AttemptStatus.ABANDONED);
                        quizAttemptRepository.save(attempt);
                        changed = true;
                    }
                }
            }
        }
        if (changed) {
            attempts = quizAttemptRepository.findAllByUserIdAndQuizIdOrderBySubmittedAtDesc(userId, quizId);
        }
        
        int attemptsUsed = attempts.size();
        Integer maxAttempts = quiz.getMaxAttempts();
        
        boolean hasPassed = false;
        double highestScore = 0.0;
        
        for (QuizAttempt attempt : attempts) {
            if (attempt.getIsPassed() != null && attempt.getIsPassed()) {
                hasPassed = true;
            }
            if (attempt.getScore() != null && attempt.getScore() > highestScore) {
                highestScore = attempt.getScore();
            }
        }
        
        boolean canAttemptAgain = true;
        if (maxAttempts != null && maxAttempts > 0) {
            canAttemptAgain = attemptsUsed < maxAttempts;
        }

        return com.hust.assessmentservice.dto.response.QuizEligibilityResponse.builder()
                .quizId(quizId)
                .maxAttempts(maxAttempts)
                .attemptsUsed(attemptsUsed)
                .hasPassed(hasPassed)
                .highestScore(highestScore)
                .canAttemptAgain(canAttemptAgain)
                .build();
    }
}
