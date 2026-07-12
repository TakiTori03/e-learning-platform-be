package com.hust.identityservice.entity;

import com.hust.commonlibrary.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "users")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User extends BaseEntity<UUID> {

    @Column(unique = true, nullable = false)
    private String email;

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    @Column(unique = true)
    private String phone;

    private String address;

    @Column(length = 1000)
    private String headline;

    @Column(columnDefinition = "TEXT")
    private String biography;

    private String avatar;

    private String language;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, String> socials;

    @Column(length = 50)
    private String role;

    // @Builder.Default
    // @Column(name = "show_profile")
    // boolean showProfile = true;

    // @Builder.Default
    // @Column(name = "show_courses")
    // boolean showCourses = true;

    @Column(name = "last_login")
    private Instant lastLogin;

    @Builder.Default
    @Column(name = "is_deleted")
    private boolean isDeleted = false;

    @Builder.Default
    @Enumerated(jakarta.persistence.EnumType.STRING)
    private UserStatus status = UserStatus.ACTIVE;
}
