package com.hust.aiservice.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "chat_sessions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatSession {

    @Id
    @Column(length = 36)
    private String id; // UUID

    @Column(name = "user_id", length = 50, nullable = false)
    private String userId;

    @Column(name = "course_id", length = 50)
    private String courseId;

    @Column(name = "agent_type", length = 20)
    private String agentType;

    @Column(length = 255)
    private String title;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
