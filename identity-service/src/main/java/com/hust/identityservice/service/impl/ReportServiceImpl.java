package com.hust.identityservice.service.impl;

import com.hust.identityservice.repository.UserRepository;
import com.hust.identityservice.repository.projection.SignupProjection;
import com.hust.identityservice.service.ReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import com.hust.identityservice.mapper.UserMapper;
import com.hust.identityservice.dto.response.SignupDataPoint;
import com.hust.identityservice.dto.response.SignupReportResponse;
import com.hust.identityservice.dto.response.UserResponse;
import com.hust.identityservice.entity.UserStatus;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportServiceImpl implements ReportService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Override
    public SignupReportResponse getNewSignupsReport(String startDate, String endDate, String groupBy) {
        log.info("Generating new signups report from {} to {} grouped by {}", startDate, endDate, groupBy);
        
        String format = "YYYY-MM-DD"; // Default to day
        if ("month".equalsIgnoreCase(groupBy)) {
            format = "YYYY-MM";
        } else if ("year".equalsIgnoreCase(groupBy)) {
            format = "YYYY";
        }

        // Standardize date strings to include time if needed for casting, e.g., 2026-05-28 -> 2026-05-28 00:00:00
        String startParam = (startDate != null && !startDate.trim().isEmpty() && !startDate.contains(" ")) ? startDate + " 00:00:00" : startDate;
        String endParam = (endDate != null && !endDate.trim().isEmpty() && !endDate.contains(" ")) ? endDate + " 23:59:59" : endDate;

        List<SignupProjection> rawData = userRepository.getNewSignupsDynamic(startParam, endParam, format);
        
        List<SignupDataPoint> dataPoints = rawData.stream().map(row -> 
            SignupDataPoint.builder()
                .period(row.getPeriod())
                .count(row.getCount() != null ? row.getCount() : 0L)
                .build()
        ).toList();

        Long totalSignups = dataPoints.stream()
                .mapToLong(SignupDataPoint::getCount)
                .sum();

        return SignupReportResponse.builder()
                .totalSignups(totalSignups)
                .dataPoints(dataPoints)
                .build();
    }

    @Override
    public List<UserResponse> getTopUsers(int limit) {
        log.info("Fetching top {} users", limit);
        Pageable pageable = PageRequest.of(0, limit);
        
        return userRepository.getTopUsersByLastLogin(UserStatus.ACTIVE, pageable)
                .getContent()
                .stream()
                .map(userMapper::toUserResponse)
                .collect(Collectors.toList());
    }
}
