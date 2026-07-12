package com.hust.learningservice.dto.response;

import com.hust.commonlibrary.dto.UserSharedProfile;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeaderboardResponse {
    private int rank;
    private String userId;
    private long totalXp;
    private int currentStreak;
    private UserSharedProfile userProfile;
}
