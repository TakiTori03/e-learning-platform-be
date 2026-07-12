package com.hust.identityservice.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.hust.commonlibrary.dto.TimeResponse;
import com.hust.identityservice.entity.UserStatus;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;


@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class UserResponse extends TimeResponse {
    private UUID id;

    private String email;
    private String firstName;
    private String lastName;
    private String phone;
    private String address;
    private String avatar;
    private String headline;
    private String biography;
    private String language;

    private Map<String, String> socials;

    // private boolean showProfile;
    // private boolean showCourses;

    private String role;
    private UserStatus status;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "UTC")
    private Instant lastLogin;

}