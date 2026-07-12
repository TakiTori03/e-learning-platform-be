package com.hust.commonlibrary.dto;

import lombok.*;
import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSharedProfile implements Serializable {
    private static final long serialVersionUID = 1L;
    private String firstName;
    private String lastName;
    private String avatar;
    private String role;
    private String email;
    private String headline;
    private String biography;
}
