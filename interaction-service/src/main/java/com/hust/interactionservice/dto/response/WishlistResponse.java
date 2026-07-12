package com.hust.interactionservice.dto.response;

import com.hust.commonlibrary.dto.TimeResponse;
import lombok.*;
import lombok.experimental.SuperBuilder;

@EqualsAndHashCode(callSuper = true)
@Getter @Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class WishlistResponse extends TimeResponse {
    private String id;
    private String courseId;
    private String userId;
}
