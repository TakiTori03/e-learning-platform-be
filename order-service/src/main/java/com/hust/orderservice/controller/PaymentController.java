package com.hust.orderservice.controller;

import com.hust.commonlibrary.dto.ApiResponse;
import com.hust.orderservice.entity.Order;
import com.hust.orderservice.repository.OrderRepository;
import com.hust.orderservice.service.PaymentService;
import com.hust.orderservice.service.VNPAYService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    private final VNPAYService vnpayService;
    private final PaymentService paymentService;
    private final OrderRepository orderRepository;

    @GetMapping("/vnpay/create/{orderId}")
    public ResponseEntity<ApiResponse<String>> createVNPAYPayment(@PathVariable String orderId, HttpServletRequest request) {
        log.info("Requesting VNPAY payment URL for order: {}", orderId);
        
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        
        long amount = order.getTotalPrice().longValue();
        String orderInfo = "Payment for order " + orderId;
        
        String ipAddress = request.getHeader("X-Forwarded-For");
        if (ipAddress == null || ipAddress.isEmpty()) {
            ipAddress = request.getRemoteAddr();
        }

        String paymentUrl = vnpayService.createPaymentUrl(orderId, amount, orderInfo, ipAddress);
        return ResponseEntity.ok(ApiResponse.<String>builder()
                .success(true)
                .payload(paymentUrl)
                .build());
    }

    @GetMapping("/vnpay/vnpay_return")
    public ResponseEntity<ApiResponse<String>> vnpayReturn(@RequestParam Map<String, String> params) {
        log.info("VNPAY Return Callback parameters: {}", params);
        String result = paymentService.processVNPAYReturn(params);
        return ResponseEntity.ok(ApiResponse.<String>builder()
                .success("success".equals(result))
                .payload(result)
                .build());
    }

    @GetMapping("/vnpay/vnpay_ipn")
    public ResponseEntity<Map<String, String>> vnpayIPN(@RequestParam Map<String, String> params) {
        log.info("VNPay IPN Received: {}", params);
        Map<String, String> response = paymentService.processVNPAYIPN(params);
        return ResponseEntity.ok(response);
    }
}
