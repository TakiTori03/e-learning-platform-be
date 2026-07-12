package com.hust.orderservice.dto.response;


import com.hust.commonlibrary.dto.TimeResponse;
import com.hust.orderservice.constant.OrderStatus;

import com.hust.orderservice.entity.OrderItem;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Getter @Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponse extends TimeResponse {
    private String id;
    private String userId;
    private Double totalPrice;
    private Double vatFee;
    private String note;
    private OrderStatus status;
    private List<OrderItem> items;
    private com.hust.commonlibrary.dto.UserSharedProfile user;
    private String transactionMethod;
    private String transactionNo;
}
