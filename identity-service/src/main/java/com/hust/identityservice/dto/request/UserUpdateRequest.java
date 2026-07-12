package com.hust.identityservice.dto.request;

import lombok.*;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserUpdateRequest {
    private String firstName;
    private String lastName;
    private String phone;
    private String address;
    private String avatar;
    private String headline;
    private String biography;
    private String language;
    private Map<String, String> socials;
    // private Boolean showProfile;
    // private Boolean showCourses;
}
