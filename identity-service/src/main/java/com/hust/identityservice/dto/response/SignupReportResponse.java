package com.hust.identityservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SignupReportResponse {
    private Long totalSignups;
    private List<SignupDataPoint> dataPoints;
}
