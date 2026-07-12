package com.hust.aiservice.entity;

import com.hust.commonlibrary.entity.ContentType;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "document_chunks")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentChunk {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "course_id", length = 50, nullable = false)
    private String courseId;

    @Column(name = "lesson_id", length = 50, nullable = false)
    private String lessonId;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "content_type", length = 20, nullable = false)
    private ContentType contentType;

    @Column(name = "source_citation", length = 50)
    private String sourceCitation;

    @Column(name = "media_id", length = 50)
    private String mediaId;

    @Column(name = "chunk_index")
    private Integer chunkIndex;

    @Column(name = "created_at", insertable = false, updatable = false)
    private java.time.LocalDateTime createdAt;
}

