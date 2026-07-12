package com.hust.learningservice.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserGamificationResponse {
    private int currentStreak;
    private int longestStreak;
    
    @JsonProperty("isStreakActiveToday")
    private boolean isStreakActiveToday;
    private long totalXp;
    
    private int todayActivitiesCount; // Added field for optimal fetching of today's activities count

    // Level info mapping
    private LevelInfo currentLevel;
    
    // Badge info list
    private List<BadgeInfo> earnedBadges;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LevelInfo {
        private String code;       // e.g. "SCHOLAR"
        private int levelIndex;    // e.g. 3
        private String title;      // e.g. "Học Giả"
        private long minXpRequired;
        private long nextLevelXpRequired;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BadgeInfo {
        private String code;        // e.g. "FIRST_BLOOD"
        private String displayName; // e.g. "Bài Học Đầu Tiên"
        private String description; // e.g. "Hoàn thành bài học..."
        private String iconUrl;     // e.g. "icon-first.png"
    }
}
