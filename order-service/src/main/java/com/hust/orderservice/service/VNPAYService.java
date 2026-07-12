package com.hust.orderservice.service;

import java.util.Map;

public interface VNPAYService {
    String createPaymentUrl(String orderId, long amount, String orderInfo, String ipAddress);
    Boolean verifyCallback(Map<String, String> queryParams);
}
