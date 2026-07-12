package com.hust.learningservice.service;

import java.util.List;

import com.hust.learningservice.dto.response.LeaderboardResponse;
import com.hust.learningservice.dto.response.UserDailyActivityResponse;
import com.hust.learningservice.dto.response.UserGamificationResponse;

public interface GamificationService {

    void processGamificationAndActivity(String userId, String lessonType);
    UserGamificationResponse getMyGamificationProfile(String userId);
    List<UserDailyActivityResponse> getMyDailyActivities(String userId, int year);
    List<LeaderboardResponse> getLeaderboard(String type);

}
