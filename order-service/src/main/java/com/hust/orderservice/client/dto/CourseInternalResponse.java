package com.hust.orderservice.client.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseInternalResponse {
    private String id;
    private String name;
    private Double price;
    private Double finalPrice;
    private String thumbnail;
    private String instructorId;
}
