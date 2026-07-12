package com.hust.orderservice.service.impl;

import com.hust.orderservice.repository.OrderRepository;
import com.hust.orderservice.repository.projection.RevenueProjection;
import com.hust.orderservice.repository.projection.CourseSalesProjection;
import com.hust.orderservice.service.ReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import com.hust.orderservice.constant.OrderStatus;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.hust.orderservice.dto.response.CourseSalesReportResponse;
import com.hust.orderservice.dto.response.OrderResponse;
import com.hust.orderservice.dto.response.RevenueDataPoint;
import com.hust.orderservice.dto.response.RevenueReportResponse;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportServiceImpl implements ReportService {

    private final OrderRepository orderRepository;

    @Override
    public RevenueReportResponse getRevenueReport(String startDate, String endDate, String groupBy) {
        log.info("Generating revenue report from {} to {} grouped by {}", startDate, endDate, groupBy);
        
        String format = "YYYY-MM-DD"; // Default to day
        if ("month".equalsIgnoreCase(groupBy)) {
            format = "YYYY-MM";
        } else if ("year".equalsIgnoreCase(groupBy)) {
            format = "YYYY";
        }

        // Standardize date strings to include time if needed for casting
        String startParam = (startDate != null && !startDate.trim().isEmpty() && !startDate.contains(" ")) ? startDate + " 00:00:00" : startDate;
        String endParam = (endDate != null && !endDate.trim().isEmpty() && !endDate.contains(" ")) ? endDate + " 23:59:59" : endDate;

        List<RevenueProjection> rawData = orderRepository.getRevenuesDynamic(
            OrderStatus.COMPLETED.name(), startParam, endParam, format
        );
        
        List<RevenueDataPoint> dataPoints = rawData.stream().map(row -> 
            RevenueDataPoint.builder()
                .period(row.getPeriod())
                .revenue(row.getRevenue() != null ? row.getRevenue() : BigDecimal.ZERO)
                .build()
        ).toList();

        BigDecimal totalRevenue = dataPoints.stream()
                .map(RevenueDataPoint::getRevenue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return RevenueReportResponse.builder()
                .totalRevenue(totalRevenue)
                .currency("VND")
                .dataPoints(dataPoints)
                .build();
    }

    @Override
    public List<CourseSalesReportResponse> getCourseSalesReport() {
        log.info("Generating course sales report");
        
        List<CourseSalesProjection> rawData = orderRepository.getCourseSales(OrderStatus.COMPLETED.name());
        
        return rawData.stream().map(row -> 
            CourseSalesReportResponse.builder()
                .courseId(row.getCourseId())
                .courseName(row.getCourseName())
                .totalSales(row.getSales() != null ? row.getSales() : 0L)
                .totalRevenue(row.getRevenue() != null ? row.getRevenue() : BigDecimal.ZERO)
                .build()
        ).toList();
    }

    @Override
    public List<OrderResponse> getTopValueOrders(int limit) {
        log.info("Fetching top {} value orders", limit);
        Pageable pageable = PageRequest.of(0, limit);
        // Note: Assuming there is an existing mapping mechanism. 
        // If mapToResponse is available in a mapper, use it. For now, we will construct a partial representation or rely on existing mappers.
        // Assuming OrderResponse has a builder. If it needs a full mapper, you should inject OrderMapper.
        return orderRepository.getTopValueOrders(OrderStatus.COMPLETED, pageable)
                .getContent()
                .stream()
                .map(order -> OrderResponse.builder()
                        .id(order.getId())
                        .userId(order.getUserId())
                        .totalPrice(order.getTotalPrice() != null ? order.getTotalPrice().doubleValue() : 0.0)
                        .status(order.getStatus())
                        .createdAt(order.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
    }
}
