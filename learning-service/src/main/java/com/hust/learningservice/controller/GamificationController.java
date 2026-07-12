package com.hust.learningservice.controller;

import com.hust.commonlibrary.dto.ApiResponse;
import com.hust.commonlibrary.utils.SecurityUtils;
import com.hust.learningservice.dto.response.LeaderboardResponse;
import com.hust.learningservice.dto.response.UserDailyActivityResponse;
import com.hust.learningservice.dto.response.UserGamificationResponse;
import com.hust.learningservice.service.GamificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@RestController
@RequestMapping("/gamification")
@RequiredArgsConstructor
public class GamificationController {

    private final GamificationService gamificationService;

    @GetMapping("/mine")
    public ApiResponse<UserGamificationResponse> getMyGamificationProfile() {
        String userId = SecurityUtils.getCurrentUserIdOrThrow();
        UserGamificationResponse profile = gamificationService.getMyGamificationProfile(userId);
        return ApiResponse.<UserGamificationResponse>builder()
                .success(true)
                .payload(profile)
                .build();
    }

    @GetMapping("/users/{userId}")
    public ApiResponse<UserGamificationResponse> getUserGamificationProfile(@PathVariable String userId) {
        UserGamificationResponse profile = gamificationService.getMyGamificationProfile(userId);
        return ApiResponse.<UserGamificationResponse>builder()
                .success(true)
                .payload(profile)
                .build();
    }

    @GetMapping("/activities")
    public ApiResponse<List<UserDailyActivityResponse>> getMyDailyActivities(@RequestParam(required = false) Integer year) {
        String userId = SecurityUtils.getCurrentUserIdOrThrow();
        if (year == null) {
            year = LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh")).getYear();
        }
        List<UserDailyActivityResponse> activities = gamificationService.getMyDailyActivities(userId, year);
        return ApiResponse.<List<UserDailyActivityResponse>>builder()
                .success(true)
                .payload(activities)
                .build();
    }

    @GetMapping("/users/{userId}/activities")
    public ApiResponse<List<UserDailyActivityResponse>> getUserDailyActivities(
            @PathVariable String userId,
            @RequestParam(required = false) Integer year) {
        if (year == null) {
            year = LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh")).getYear();
        }
        List<UserDailyActivityResponse> activities = gamificationService.getMyDailyActivities(userId, year);
        return ApiResponse.<List<UserDailyActivityResponse>>builder()
                .success(true)
                .payload(activities)
                .build();
    }

    @GetMapping("/leaderboard")
    public ApiResponse<List<LeaderboardResponse>> getLeaderboard(@RequestParam(defaultValue = "XP") String type) {
        List<LeaderboardResponse> leaderboard = gamificationService.getLeaderboard(type);
        return ApiResponse.<List<LeaderboardResponse>>builder()
                .success(true)
                .payload(leaderboard)
                .build();
    }
}
