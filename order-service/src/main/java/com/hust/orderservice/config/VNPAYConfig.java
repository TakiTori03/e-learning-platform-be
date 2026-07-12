package com.hust.orderservice.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@Getter
public class VNPAYConfig {

    @Value("${payment.vnpay.tmn-code:B0G7MQOH}")
    private String tmnCode;

    @Value("${payment.vnpay.hash-secret:MUQDZO18PYW9GLBHMISDS442I9SARIHT}")
    private String hashSecret;

    @Value("${payment.vnpay.url:https://sandbox.vnpayment.vn/paymentv2/vpcpay.html}")
    private String vnpPayUrl;

    @Value("${payment.vnpay.return-url:https://app.hust-elearning.online/order/vnpay-return}")
    private String vnpReturnUrl;
}
