package com.hust.orderservice.service;



import com.hust.commonlibrary.dto.ListResponse;
import com.hust.orderservice.dto.request.OrderRequest;
import com.hust.orderservice.dto.response.OrderResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;


public interface OrderService {
    OrderResponse createOrder(OrderRequest request);
    OrderResponse getOrderById(String id);
    ListResponse<OrderResponse> getOrdersByUserId(String userId, Pageable pageable);
    Map<String, Long> getEnrollmentCountsBulk(List<String> courseIds);
    boolean checkIfBought(String userId, String courseId);
    Map<String, Boolean> checkIfBoughtBulk(String userId, List<String> courseIds);
    ListResponse<OrderResponse> getAllOrders(Pageable pageable);
    OrderResponse updateOrderStatus(String orderId, String status);
}
