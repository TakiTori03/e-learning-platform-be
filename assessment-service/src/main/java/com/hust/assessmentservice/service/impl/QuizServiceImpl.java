package com.hust.assessmentservice.service.impl;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import com.hust.assessmentservice.dto.request.QuizRequest;
import com.hust.assessmentservice.dto.response.QuizResponse;
import com.hust.assessmentservice.entity.AnswerOption;
import com.hust.assessmentservice.entity.Question;
import com.hust.assessmentservice.entity.Quiz;
import com.hust.assessmentservice.entity.QuizAttempt;
import com.hust.assessmentservice.mapper.QuizMapper;
import com.hust.assessmentservice.repository.QuizRepository;
import com.hust.assessmentservice.service.QuizService;
import com.hust.commonlibrary.dto.ListResponse;
import com.hust.commonlibrary.entity.QuizType;
import com.hust.commonlibrary.exception.payload.ResourceNotFoundException;
import org.springframework.data.domain.Pageable;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class QuizServiceImpl implements QuizService {

    private final QuizRepository quizRepository;
    private final QuizMapper quizMapper;
    private final MongoTemplate mongoTemplate;

    @Override
    public QuizResponse createQuiz(QuizRequest request) {
        log.info("Creating quiz for courseId={}, lessonId={}", request.getCourseId(), request.getLessonId());

        Quiz quiz = quizMapper.requestToEntity(request);
        generateMissingIds(quiz);

        Quiz saved = quizRepository.save(quiz);
        return toQuizResponse(saved, false);
    }

    @Override
    public QuizResponse updateQuiz(String id, QuizRequest request) {
        log.info("Updating quiz id={}", id);
        Quiz quiz = quizRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Quiz not found with id: " + id));

        // Map updated properties onto the existing entity
        quizMapper.partialUpdate(quiz, request);
        generateMissingIds(quiz);

        Quiz saved = quizRepository.save(quiz);
        return toQuizResponse(saved, false);
    }

    @Override
    public void deleteQuiz(String id) {
        log.info("Deleting quiz id={}", id);
        Quiz quiz = quizRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Quiz not found with id: " + id));

        if ((quiz.getCourseId() != null && !quiz.getCourseId().isEmpty()) || 
            (quiz.getLessonId() != null && !quiz.getLessonId().isEmpty())) {
            throw new IllegalStateException("Cannot delete a quiz that is already linked to a course or lesson.");
        }

        quizRepository.deleteById(id);
    }

    @Override
    public QuizResponse getQuizById(String id, boolean isStudent) {
        log.info("Fetching quiz id={}, isStudent={}", id, isStudent);
        Quiz quiz = quizRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Quiz not found with id: " + id));
        return toQuizResponse(quiz, isStudent);
    }

    @Override
    public QuizResponse getQuizByLessonId(String lessonId, boolean isStudent) {
        log.info("Fetching quiz for lessonId={}, isStudent={}", lessonId, isStudent);
        Quiz quiz = quizRepository.findByLessonId(lessonId)
                .orElseThrow(() -> new ResourceNotFoundException("Quiz not found for lessonId: " + lessonId));
        return toQuizResponse(quiz, isStudent);
    }

    @Override
    public ListResponse<QuizResponse> getAllQuizzes(String authorId, String q, String courseId) {
        log.info("Fetching all quizzes for authorId={}, q={}, courseId={}", authorId, q, courseId);
        Query query = new Query();
        query.addCriteria(Criteria.where("authorId").is(authorId));

        if (courseId != null && !courseId.trim().isEmpty()) {
            query.addCriteria(Criteria.where("courseId").is(courseId));
        }

        if (q != null && !q.trim().isEmpty()) {
            Criteria searchCriteria = new Criteria().orOperator(
                    Criteria.where("title").regex(q, "i"),
                    Criteria.where("description").regex(q, "i")
            );
            query.addCriteria(searchCriteria);
        }

        query.with(org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "createdAt"));

        List<Quiz> quizzes = mongoTemplate.find(query, Quiz.class);
        List<QuizResponse> responses = quizzes.stream()
                .map(quiz -> toQuizResponse(quiz, false))
                .collect(Collectors.toList());

        return ListResponse.<QuizResponse>builder()
                .content(responses)
                .build();
    }

    @Override
    public com.hust.assessmentservice.dto.response.QuizStatsResponse getQuizStatistics(String authorId) {
        log.info("Calculating quiz statistics for authorId={}", authorId);
        Query query = new Query();
        query.addCriteria(Criteria.where("authorId").is(authorId));

        List<Quiz> quizzes = mongoTemplate.find(query, Quiz.class);
        long totalCount = quizzes.size();
        long linkedCount = quizzes.stream().filter(q -> (q.getLessonId() != null && !q.getLessonId().isEmpty()) || (q.getCourseId() != null && !q.getCourseId().isEmpty())).count();
        long unlinkedCount = totalCount - linkedCount;
        double avgTimeLimit = quizzes.stream().mapToInt(q -> q.getTimeLimitMinutes() != null ? q.getTimeLimitMinutes() : 0).average().orElse(0.0);

        return com.hust.assessmentservice.dto.response.QuizStatsResponse.builder()
                .totalQuizzes(totalCount)
                .linkedQuizzes(linkedCount)
                .unlinkedQuizzes(unlinkedCount)
                .avgTimeLimit(Math.round(avgTimeLimit))
                .build();
    }

    @Override
    public ListResponse<QuizResponse> getQuizzesForSelect(String authorId, String q, QuizType type) {
        log.info("Fetching quizzes for select: authorId={}, q={}, type={}", authorId, q, type);
        Query query = new Query();
        query.addCriteria(Criteria.where("authorId").is(authorId));

        QuizType quizType = type != null ? type : QuizType.PRACTICE;
        query.addCriteria(Criteria.where("type").is(quizType));

        query.addCriteria(Criteria.where("courseId").in(null, ""));
        query.addCriteria(Criteria.where("lessonId").in(null, ""));

        if (q != null && !q.trim().isEmpty()) {
            query.addCriteria(Criteria.where("title").regex(q, "i"));
        }

        List<Quiz> quizzes = mongoTemplate.find(query, Quiz.class);
        List<QuizResponse> responses = quizzes.stream()
                .map(quiz -> toQuizResponse(quiz, false))
                .collect(Collectors.toList());

        return ListResponse.<QuizResponse>builder()
                .content(responses)
                .build();
    }

    private void generateMissingIds(Quiz quiz) {
        if (quiz.getQuestions() != null) {
            for (Question q : quiz.getQuestions()) {
                if (q.getId() == null) {
                    q.setId(UUID.randomUUID().toString());
                }
                if (q.getOptions() != null) {
                    for (AnswerOption o : q.getOptions()) {
                        if (o.getId() == null) {
                            o.setId(UUID.randomUUID().toString());
                        }
                    }
                }
            }
        }
    }

    private QuizResponse toQuizResponse(Quiz quiz, boolean isStudent) {
        return quizMapper.entityToResponse(quiz, isStudent);
    }

    @Override
    public ListResponse<com.hust.assessmentservice.dto.response.QuizAssignmentResponse> getStudentAssignmentsPaged(
            String userId, List<String> courseIds, String search, com.hust.commonlibrary.entity.QuizType type, com.hust.assessmentservice.entity.QuizStatus status, Pageable pageable) {
        int page = pageable.getPageNumber();
        int limit = pageable.getPageSize();
        log.info("Fetching paged assignments: userId={}, courseIds={}, page={}, limit={}, search={}, type={}, status={}",
                userId, courseIds, page, limit, search, type, status);

        // 1. Fetch all quizzes matching search text and type (filter by courseIds if provided)
        Query query = new Query();
        if (courseIds != null && !courseIds.isEmpty()) {
            query.addCriteria(Criteria.where("courseId").in(courseIds));
        }
        if (type != null) {
            query.addCriteria(Criteria.where("type").is(type));
        }
        if (search != null && !search.trim().isEmpty()) {
            query.addCriteria(new Criteria().orOperator(
                    Criteria.where("title").regex(search, "i"),
                    Criteria.where("description").regex(search, "i")
            ));
        }

        List<Quiz> quizzes = mongoTemplate.find(query, Quiz.class);

        // 2. Fetch all attempts by this user (filter by courseIds if provided)
        Query attemptQuery = new Query();
        attemptQuery.addCriteria(Criteria.where("userId").is(userId));
        if (courseIds != null && !courseIds.isEmpty()) {
            attemptQuery.addCriteria(Criteria.where("courseId").in(courseIds));
        }
        attemptQuery.with(org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "submittedAt"));
        List<QuizAttempt> attempts = mongoTemplate.find(attemptQuery, QuizAttempt.class);

        // Group attempts by quiz ID for fast lookup
        java.util.Map<String, List<QuizAttempt>> attemptsByQuiz = attempts.stream()
                .collect(Collectors.groupingBy(QuizAttempt::getQuizId));

        // 3. Map to QuizAssignmentResponse and compute statuses/scores
        List<com.hust.assessmentservice.dto.response.QuizAssignmentResponse> allResponses = quizzes.stream()
                .map(quiz -> {
                    List<QuizAttempt> quizAttempts = attemptsByQuiz.getOrDefault(quiz.getId(), java.util.Collections.emptyList());

                    Double bestScore = quizAttempts.isEmpty() ? null : quizAttempts.stream()
                            .mapToDouble(a -> a.getScore() != null ? a.getScore() : 0.0)
                            .max()
                            .orElse(0.0);

                    boolean isPassed = quizAttempts.stream().anyMatch(a -> Boolean.TRUE.equals(a.getIsPassed()));

                    com.hust.assessmentservice.entity.QuizStatus calculatedStatus = com.hust.assessmentservice.entity.QuizStatus.NOTDONE;
                    if (isPassed) {
                        calculatedStatus = com.hust.assessmentservice.entity.QuizStatus.DONE;
                    } else if (!quizAttempts.isEmpty()) {
                        calculatedStatus = com.hust.assessmentservice.entity.QuizStatus.PROGRESS;
                    }

                    int usedAttempts = quizAttempts.size();
                    int questionsCount = quiz.getQuestions() != null ? quiz.getQuestions().size() : 0;

                    return com.hust.assessmentservice.dto.response.QuizAssignmentResponse.builder()
                            .id(quiz.getId())
                            .courseId(quiz.getCourseId())
                            .lessonId(quiz.getLessonId())
                            .title(quiz.getTitle())
                            .description(quiz.getDescription())
                            .timeLimitMinutes(quiz.getTimeLimitMinutes())
                            .passingScorePercentage(quiz.getPassingScorePercentage())
                            .maxAttempts(quiz.getMaxAttempts())
                            .type(quiz.getType())
                            .bestScore(bestScore)
                            .isPassed(isPassed)
                            .status(calculatedStatus)
                            .usedAttempts(usedAttempts)
                            .questionsCount(questionsCount)
                            .createdAt(quiz.getCreatedAt())
                            .build();
                })
                // 4. Filter by status if filter is not null
                .filter(res -> status == null || res.getStatus() == status)
                .sorted((a, b) -> {
                    if (a.getCreatedAt() != null && b.getCreatedAt() != null) {
                        return b.getCreatedAt().compareTo(a.getCreatedAt());
                    }
                    return 0;
                })
                .collect(Collectors.toList());

        // 5. Paginate in memory
        int totalElements = allResponses.size();
        int totalPages = (int) Math.ceil((double) totalElements / limit);
        int fromIndex = page * limit;
        int toIndex = Math.min(fromIndex + limit, totalElements);

        List<com.hust.assessmentservice.dto.response.QuizAssignmentResponse> pagedContent = java.util.Collections.emptyList();
        if (fromIndex < totalElements) {
            pagedContent = allResponses.subList(fromIndex, toIndex);
        }

        return ListResponse.<com.hust.assessmentservice.dto.response.QuizAssignmentResponse>builder()
                .content(pagedContent)
                .pageNumber(page + 1)
                .pageSize(limit)
                .totalElements(totalElements)
                .totalPages(totalPages)
                .isLast((page + 1) >= totalPages)
                .build();
    }

    @Override
    public com.hust.assessmentservice.dto.response.QuizAssignmentStatsResponse getStudentAssignmentsStats(
            String userId, List<String> courseIds) {
        log.info("Calculating assignments stats for userId={}, courseIds={}", userId, courseIds);

        // 1. Fetch all quizzes (filter by courseIds if provided)
        Query query = new Query();
        if (courseIds != null && !courseIds.isEmpty()) {
            query.addCriteria(Criteria.where("courseId").in(courseIds));
        }
        List<Quiz> quizzes = mongoTemplate.find(query, Quiz.class);

        // 2. Fetch all attempts by this user (filter by courseIds if provided)
        Query attemptQuery = new Query();
        attemptQuery.addCriteria(Criteria.where("userId").is(userId));
        if (courseIds != null && !courseIds.isEmpty()) {
            attemptQuery.addCriteria(Criteria.where("courseId").in(courseIds));
        }
        List<QuizAttempt> attempts = mongoTemplate.find(attemptQuery, QuizAttempt.class);

        // Group attempts by quiz ID for fast lookup
        java.util.Map<String, List<QuizAttempt>> attemptsByQuiz = attempts.stream()
                .collect(Collectors.groupingBy(QuizAttempt::getQuizId));

        long totalCount = quizzes.size();
        long completedCount = 0;
        long inProgressCount = 0;

        for (Quiz quiz : quizzes) {
            List<QuizAttempt> quizAttempts = attemptsByQuiz.getOrDefault(quiz.getId(), java.util.Collections.emptyList());
            if (!quizAttempts.isEmpty()) {
                boolean isPassed = quizAttempts.stream().anyMatch(a -> Boolean.TRUE.equals(a.getIsPassed()));
                if (isPassed) {
                    completedCount++;
                } else {
                    inProgressCount++;
                }
            }
        }

        double completionRate = totalCount > 0 ? Math.round(((double) completedCount / totalCount * 100) * 10) / 10.0 : 0.0;

        return com.hust.assessmentservice.dto.response.QuizAssignmentStatsResponse.builder()
                .totalCount(totalCount)
                .completedCount(completedCount)
                .inProgressCount(inProgressCount)
                .completionRate(completionRate)
                .build();
    }

    @Override
    public QuizResponse importQuiz(String title, String description, Integer timeLimitMinutes, Double passingScorePercentage, QuizType type, Integer maxAttempts, org.springframework.web.multipart.MultipartFile file, String authorId) {
        log.info("Importing quiz from file. AuthorId={}", authorId);

        Quiz quiz = Quiz.builder()
                .title(title)
                .description(description)
                .timeLimitMinutes(timeLimitMinutes)
                .passingScorePercentage(passingScorePercentage)
                .type(type != null ? type : QuizType.PRACTICE)
                .maxAttempts(maxAttempts)
                .authorId(authorId)
                .questions(new java.util.ArrayList<>())
                .build();

        String filename = file.getOriginalFilename();
        if (filename != null && filename.toLowerCase().endsWith(".csv")) {
            parseCsvFile(file, quiz);
        } else {
            parseExcelFile(file, quiz);
        }

        Quiz saved = quizRepository.save(quiz);
        return toQuizResponse(saved, false);
    }

    private void parseCsvFile(org.springframework.web.multipart.MultipartFile file, Quiz quiz) {
        try (java.io.Reader reader = new java.io.InputStreamReader(file.getInputStream(), java.nio.charset.StandardCharsets.UTF_8);
             org.apache.commons.csv.CSVParser csvParser = new org.apache.commons.csv.CSVParser(reader, org.apache.commons.csv.CSVFormat.DEFAULT.withFirstRecordAsHeader().withIgnoreHeaderCase().withTrim())) {
            
            for (org.apache.commons.csv.CSVRecord record : csvParser) {
                if (record.size() < 3) continue;
                
                String questionText = record.get(0);
                if (questionText == null || questionText.isEmpty()) continue;
                
                Double scoreWeight = 1.0;
                try {
                    String scoreStr = record.get(1);
                    if (scoreStr != null && !scoreStr.isEmpty()) {
                        scoreWeight = Double.parseDouble(scoreStr);
                    }
                } catch (Exception e) {}
                
                String correctAnswersStr = record.get(2);
                List<String> correctLetters = new java.util.ArrayList<>();
                if (correctAnswersStr != null && !correctAnswersStr.isEmpty()) {
                    String[] parts = correctAnswersStr.split("[,\\s]+");
                    for (String part : parts) {
                        if (!part.trim().isEmpty()) {
                            correctLetters.add(part.trim().toUpperCase());
                        }
                    }
                }

                List<AnswerOption> options = new java.util.ArrayList<>();
                int correctCount = 0;
                // Các cột từ 3 trở đi chứa các phương án
                for (int j = 3; j < record.size(); j++) {
                    String optText = record.get(j);
                    if (optText != null && !optText.trim().isEmpty()) {
                        String currentLetter = String.valueOf((char) ('A' + (j - 3)));
                        boolean isCorrect = correctLetters.contains(currentLetter);
                        if (isCorrect) correctCount++;

                        options.add(AnswerOption.builder()
                                .id(UUID.randomUUID().toString())
                                .optionText(optText.trim())
                                .isCorrect(isCorrect)
                                .build());
                    }
                }

                com.hust.assessmentservice.entity.enums.QuestionType qType =
                        (correctCount > 1) ? com.hust.assessmentservice.entity.enums.QuestionType.MULTI_CHOICE : com.hust.assessmentservice.entity.enums.QuestionType.SINGLE_CHOICE;

                Question question = Question.builder()
                        .id(UUID.randomUUID().toString())
                        .questionText(questionText.trim())
                        .scoreWeight(scoreWeight)
                        .options(options)
                        .type(qType)
                        .build();

                quiz.getQuestions().add(question);
            }
        } catch (Exception e) {
            log.error("Error parsing CSV file", e);
            throw new com.hust.commonlibrary.exception.payload.InvalidParamException("Failed to parse CSV file: " + e.getMessage());
        }
    }

    private void parseExcelFile(org.springframework.web.multipart.MultipartFile file, Quiz quiz) {
        try (org.apache.poi.ss.usermodel.Workbook workbook = org.apache.poi.ss.usermodel.WorkbookFactory.create(file.getInputStream())) {
            org.apache.poi.ss.usermodel.Sheet sheet = workbook.getSheetAt(0);

            // Skip header row
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                org.apache.poi.ss.usermodel.Row row = sheet.getRow(i);
                if (row == null) continue;

                String questionText = getCellValueAsString(row.getCell(0));
                if (questionText == null || questionText.trim().isEmpty()) continue;

                String scoreWeightStr = getCellValueAsString(row.getCell(1));
                Double scoreWeight = 1.0;
                try {
                    if (scoreWeightStr != null && !scoreWeightStr.isEmpty()) {
                        scoreWeight = Double.parseDouble(scoreWeightStr);
                    }
                } catch (Exception e) {}

                // Cột 2 (Cột C) chứa đáp án đúng (VD: A hoặc A, C)
                String correctAnswersStr = getCellValueAsString(row.getCell(2));
                List<String> correctLetters = new java.util.ArrayList<>();
                if (correctAnswersStr != null && !correctAnswersStr.isEmpty()) {
                    String[] parts = correctAnswersStr.split("[,\\s]+");
                    for (String part : parts) {
                        if (!part.trim().isEmpty()) {
                            correctLetters.add(part.trim().toUpperCase());
                        }
                    }
                }

                List<AnswerOption> options = new java.util.ArrayList<>();
                int correctCount = 0;
                // Các cột từ 3 (Cột D) trở đi chứa các phương án (A, B, C, D, E...)
                int lastCellIndex = row.getLastCellNum();
                for (int j = 3; j < lastCellIndex; j++) {
                    String optText = getCellValueAsString(row.getCell(j));
                    if (optText != null && !optText.trim().isEmpty()) {
                        String currentLetter = String.valueOf((char) ('A' + (j - 3)));
                        boolean isCorrect = correctLetters.contains(currentLetter);
                        if (isCorrect) correctCount++;

                        options.add(AnswerOption.builder()
                                .id(UUID.randomUUID().toString())
                                .optionText(optText.trim())
                                .isCorrect(isCorrect)
                                .build());
                    }
                }

                com.hust.assessmentservice.entity.enums.QuestionType qType =
                        (correctCount > 1) ? com.hust.assessmentservice.entity.enums.QuestionType.MULTI_CHOICE : com.hust.assessmentservice.entity.enums.QuestionType.SINGLE_CHOICE;

                Question question = Question.builder()
                        .id(UUID.randomUUID().toString())
                        .questionText(questionText.trim())
                        .scoreWeight(scoreWeight)
                        .options(options)
                        .type(qType)
                        .build();

                quiz.getQuestions().add(question);
            }

        } catch (Exception e) {
            log.error("Error parsing Excel file", e);
            throw new com.hust.commonlibrary.exception.payload.InvalidParamException("Failed to parse Excel file: " + e.getMessage());
        }
    }

    private String getCellValueAsString(org.apache.poi.ss.usermodel.Cell cell) {
        if (cell == null) return "";
        switch (cell.getCellType()) {
            case STRING: return cell.getStringCellValue();
            case NUMERIC:
                if (org.apache.poi.ss.usermodel.DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                }
                return String.valueOf(cell.getNumericCellValue());
            case BOOLEAN: return String.valueOf(cell.getBooleanCellValue());
            case FORMULA: return cell.getCellFormula();
            default: return "";
        }
    }
}
