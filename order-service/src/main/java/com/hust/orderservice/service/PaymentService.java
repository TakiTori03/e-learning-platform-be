package com.hust.orderservice.service;

import com.hust.commonlibrary.dto.ListResponse;
import com.hust.orderservice.dto.response.PaymentResponse;
import org.springframework.data.domain.Pageable;
import java.util.Map;

public interface PaymentService {
    Map<String, String> processVNPAYIPN(Map<String, String> params);
    String processVNPAYReturn(Map<String, String> params);
    
    ListResponse<PaymentResponse> getAllPaymentsForAdmin(Pageable pageable);
    PaymentResponse getPaymentById(String id);
}
