package com.hust.orderservice.dto.response;

import com.hust.commonlibrary.dto.TimeResponse;
import com.hust.orderservice.constant.PaymentStatus;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.Instant;

@EqualsAndHashCode(callSuper = true)
@Getter @Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponse extends TimeResponse {
    private String id;
    private String orderId;
    private String transactionId;
    private String method;
    private BigDecimal amount;
    private String bankCode;
    private String bankTranNo;
    private String cardType;
    private String orderInfo;
    private String vnpTxnRef;
    private PaymentStatus status;
    private Instant payDate;
    private com.hust.commonlibrary.dto.UserSharedProfile user;
}
