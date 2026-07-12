package com.hust.orderservice.service;

import com.hust.orderservice.dto.response.CourseSalesReportResponse;
import com.hust.orderservice.dto.response.OrderResponse;
import com.hust.orderservice.dto.response.RevenueReportResponse;

import java.util.List;

public interface ReportService {
    
    RevenueReportResponse getRevenueReport(String startDate, String endDate, String groupBy);

    List<CourseSalesReportResponse> getCourseSalesReport();

    List<OrderResponse> getTopValueOrders(int limit);
}
