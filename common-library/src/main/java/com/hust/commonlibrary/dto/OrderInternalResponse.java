package com.hust.commonlibrary.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

import java.util.List;

import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class OrderInternalResponse extends TimeResponse {
    private String id;
    private String userId;
    private Double totalPrice;
    private Double vatFee;
    private String note;
    private String status;
    private List<OrderItemInternalResponse> items;
}
