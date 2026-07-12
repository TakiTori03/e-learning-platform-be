package com.hust.courseservice.event;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CurriculumMutatedEvent {
    private final String courseId;
}
