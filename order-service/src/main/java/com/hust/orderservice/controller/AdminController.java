package com.hust.orderservice.controller;

import com.hust.commonlibrary.dto.ApiResponse;
import com.hust.commonlibrary.dto.ListResponse;
import com.hust.orderservice.dto.response.CourseSalesReportResponse;
import com.hust.orderservice.dto.response.OrderResponse;
import com.hust.orderservice.dto.response.PaymentResponse;
import com.hust.orderservice.dto.response.RevenueReportResponse;
import com.hust.orderservice.service.OrderService;
import com.hust.orderservice.service.PaymentService;
import com.hust.orderservice.service.ReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Slf4j
public class AdminController {

    private final OrderService orderService;
    private final ReportService reportService;
    private final PaymentService paymentService;

    // ======================== ORDER MANAGEMENT ========================

    @GetMapping("/orders")
    public ResponseEntity<ApiResponse<ListResponse<OrderResponse>>> getAllOrders(
            @PageableDefault Pageable pageable) {
        log.info("Admin fetching all orders with pagination: {}", pageable);
        return ResponseEntity.ok(
                ApiResponse.<ListResponse<OrderResponse>>builder()
                        .success(true)
                        .payload(orderService.getAllOrders(pageable))
                        .build()
        );
    }

    @GetMapping("/orders/{id}")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrderById(@PathVariable String id) {
        log.info("Admin fetching details for Order ID: {}", id);
        return ResponseEntity.ok(
                ApiResponse.<OrderResponse>builder()
                        .success(true)
                        .payload(orderService.getOrderById(id))
                        .build()
        );
    }

    @PatchMapping("/orders/{id}/status")
    public ResponseEntity<ApiResponse<OrderResponse>> updateOrderStatus(
            @PathVariable String id,
            @RequestParam String status) {
        log.info("Admin updating status of Order {} to {}", id, status);
        return ResponseEntity.ok(
                ApiResponse.<OrderResponse>builder()
                        .success(true)
                        .payload(orderService.updateOrderStatus(id, status))
                        .build()
        );
    }

    // ======================== REPORT ========================

    @GetMapping("/reports/revenues")
    public ResponseEntity<ApiResponse<RevenueReportResponse>> getRevenues(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(defaultValue = "month") String groupBy) {
        return ResponseEntity.ok(
                ApiResponse.<RevenueReportResponse>builder()
                        .success(true)
                        .payload(reportService.getRevenueReport(startDate, endDate, groupBy))
                        .build()
        );
    }

    @GetMapping("/reports/course-sales")
    public ResponseEntity<ApiResponse<List<CourseSalesReportResponse>>> getCourseSales() {
        return ResponseEntity.ok(
                ApiResponse.<List<CourseSalesReportResponse>>builder()
                        .success(true)
                        .payload(reportService.getCourseSalesReport())
                        .build()
        );
    }

    @GetMapping("/reports/top-orders")
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getTopOrders(
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(
                ApiResponse.<List<OrderResponse>>builder()
                        .success(true)
                        .payload(reportService.getTopValueOrders(limit))
                        .build()
        );
    }

    // ======================== TRANSACTION ========================

    @GetMapping("/transactions")
    public ResponseEntity<ApiResponse<ListResponse<PaymentResponse>>> getAllTransactions(
            @PageableDefault(size = 15) Pageable pageable) {
        log.info("Admin query: Fetching paginated list of all bank transactions: {}", pageable);
        return ResponseEntity.ok(
                ApiResponse.<ListResponse<PaymentResponse>>builder()
                        .success(true)
                        .payload(paymentService.getAllPaymentsForAdmin(pageable))
                        .build()
        );
    }

    @GetMapping("/transactions/{id}")
    public ResponseEntity<ApiResponse<PaymentResponse>> getTransactionById(@PathVariable String id) {
        log.info("Admin query: Fetching specific transaction technical audit metadata for ID: {}", id);
        return ResponseEntity.ok(
                ApiResponse.<PaymentResponse>builder()
                        .success(true)
                        .payload(paymentService.getPaymentById(id))
                        .build()
        );
    }
}
