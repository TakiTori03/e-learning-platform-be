package com.hust.assessmentservice.entity;

import com.hust.commonlibrary.entity.BaseDocument;
import lombok.*;
import lombok.experimental.SuperBuilder;
import com.hust.commonlibrary.entity.QuizType;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.Instant;
import java.util.List;

@Document(collection = "quiz_attempts")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@CompoundIndex(name = "user_quiz_attempt_idx", def = "{'userId': 1, 'quizId': 1}")
public class QuizAttempt extends BaseDocument {

    @Indexed
    private String userId; // ID học viên thực hiện bài làm
    
    @Indexed
    private String quizId; // ID bài thi
    
    @Indexed
    private String courseId; // ID khóa học chứa bài thi (để lọc thống kê/phân quyền)
    
    private List<UserAnswer> submittedAnswers; // Danh sách câu trả lời của học viên
    
    private Double score; // Điểm số tính được (thang điểm 100)
    private Boolean isPassed; // Kết quả Đạt / Không đạt
    
    private QuizType quizType; // Loại đề thi của lượt làm bài này
    
    private AttemptStatus status; // Trạng thái của lượt làm bài
    private Instant startedAt; // Thời gian bắt đầu làm bài
    private Integer violationCount; // Số lần vi phạm (chuyển tab)
    private Integer timeLimitSnapshot; // Thời gian giới hạn (phút) tại thời điểm bắt đầu thi
    
    private Instant submittedAt; // Thời gian nộp bài làm
}
