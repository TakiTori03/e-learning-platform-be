package com.hust.orderservice.controller;

import com.hust.commonlibrary.dto.ApiResponse;
import com.hust.commonlibrary.utils.SecurityUtils;
import com.hust.orderservice.dto.request.OrderRequest;
import com.hust.orderservice.dto.response.OrderResponse;
import com.hust.orderservice.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
@Slf4j
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<ApiResponse<OrderResponse>> createOrder(@RequestBody @Valid OrderRequest request) {
        // Trích xuất thông tin từ Token để đảm bảo an toàn
        String userId = SecurityUtils.getCurrentUserIdOrThrow();
        
        log.info("REST request to create Order for UserID: {}", userId);
        
        request.setUserId(userId);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.<OrderResponse>builder()
                .success(true)
                .payload(orderService.createOrder(request))
                .build());
    }

    @PostMapping("/public/test-saga")
    public ResponseEntity<ApiResponse<OrderResponse>> testSagaCreate(@RequestBody OrderRequest request) {
        log.info("REST request to create Order via public test-saga API for UserID: {}", request.getUserId());
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.<OrderResponse>builder()
                .success(true)
                .payload(orderService.createOrder(request))
                .build());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrderById(@PathVariable String id) {
        log.info("REST request to get Order : {}", id);
        return ResponseEntity.ok(ApiResponse.<OrderResponse>builder()
                .success(true)
                .payload(orderService.getOrderById(id))
                .build());
    }

    @GetMapping("/mine")
    public ResponseEntity<ApiResponse<com.hust.commonlibrary.dto.ListResponse<OrderResponse>>> getMyOrders(
            @PageableDefault(sort = "createdAt", direction = org.springframework.data.domain.Sort.Direction.DESC) Pageable pageable) {
        String userId = SecurityUtils.getCurrentUserIdOrThrow();
        log.info("REST request to get all Orders for user: {}, pageable: {}", userId, pageable);
        return ResponseEntity.ok(ApiResponse.<com.hust.commonlibrary.dto.ListResponse<OrderResponse>>builder()
                .success(true)
                .payload(orderService.getOrdersByUserId(userId, pageable))
                .build());
    }
}
