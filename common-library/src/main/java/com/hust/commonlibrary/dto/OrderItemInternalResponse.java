package com.hust.commonlibrary.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class OrderItemInternalResponse {
    private String courseId;
    private String name;
    private BigDecimal finalPrice;
    private String thumbnail;
}
