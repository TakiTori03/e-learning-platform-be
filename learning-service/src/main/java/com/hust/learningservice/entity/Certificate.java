package com.hust.learningservice.entity;

import com.hust.commonlibrary.entity.BaseDocument;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.Instant;

@Document(collection = "certificates")
@CompoundIndexes({
    @CompoundIndex(name = "user_course_cert_unique_idx", def = "{'userId': 1, 'courseId': 1}", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Certificate extends BaseDocument {

    @Indexed
    private String userId;

    @Indexed
    private String courseId;

    private String courseName;
    private String studentFullName;
    
    private Double finalScore;
    private String classification;

    private String certificateUrl;
    private CertificateStatus status;
    private Instant issuedAt;
}
