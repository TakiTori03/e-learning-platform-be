package com.hust.courseservice.event;

import com.hust.courseservice.entity.Course;
import com.hust.courseservice.entity.enums.CourseStatus;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CourseStatusChangedLocalEvent {
    private Course course;
    private CourseStatus action;
}
