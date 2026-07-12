package com.hust.identityservice.dto.request;

import lombok.*;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class InstructorRegistrationRequest extends UserRegistrationRequest {

    private String headline;

    private String biography;

}
