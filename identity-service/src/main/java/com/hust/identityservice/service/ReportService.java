package com.hust.identityservice.service;

import com.hust.identityservice.dto.response.SignupReportResponse;
import com.hust.identityservice.dto.response.UserResponse;

import java.util.List;

public interface ReportService {
    SignupReportResponse getNewSignupsReport(String startDate, String endDate, String groupBy);

    List<UserResponse> getTopUsers(int limit);
}
