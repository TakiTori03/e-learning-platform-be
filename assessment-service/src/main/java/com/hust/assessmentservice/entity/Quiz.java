package com.hust.assessmentservice.entity;

import com.hust.commonlibrary.entity.BaseDocument;
import lombok.*;
import lombok.experimental.SuperBuilder;
import com.hust.commonlibrary.entity.QuizType;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.List;

@Document(collection = "quizzes")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class Quiz extends BaseDocument {

    @Indexed(unique = true, partialFilter = "{'type': 'FINAL', 'courseId': { $type: 'string' }}")
    private String courseId; // Liên kết tới khóa học của đề thi

    @Indexed
    private String lessonId; // Liên kết tới bài học chứa Quiz (null đối với bài thi cuối khóa)
    
    @Indexed
    private String authorId; // ID của giảng viên tạo đề thi
    
    private String title;
    private String description;
    
    private Integer timeLimitMinutes; // Thời gian làm bài (phút), null nếu không giới hạn
    private Double passingScorePercentage; // Điểm đạt yêu cầu (ví dụ: 65.0%)
    private Integer maxAttempts; // Giới hạn số lần nộp bài, null hoặc <= 0 nghĩa là không giới hạn
    
    private QuizType type; // Loại đề thi (PRACTICE, MIDTERM, FINAL)
    
    private List<Question> questions; // Danh sách câu hỏi của bài thi
}
