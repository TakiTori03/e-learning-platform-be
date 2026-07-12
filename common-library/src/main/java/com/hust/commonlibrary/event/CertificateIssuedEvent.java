package com.hust.commonlibrary.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.Instant;

/**
 * Sự kiện phát ra từ learning-service khi học viên đủ điều kiện cấp chứng chỉ.
 * worker-service lắng nghe để sinh tệp PDF chứng chỉ tương ứng.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CertificateIssuedEvent {
    private String certificateId;
    private String userId;
    private String courseId;
    private String studentName;
    private String courseName;
    private Double finalScore;
    private String classification;
    private Instant issuedAt;
}
