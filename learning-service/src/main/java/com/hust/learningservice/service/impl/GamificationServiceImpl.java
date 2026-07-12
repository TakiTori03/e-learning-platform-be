package com.hust.learningservice.service.impl;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hust.commonlibrary.constants.RedisPrefixConstants;
import com.hust.commonlibrary.dto.UserSharedProfile;
import com.hust.commonlibrary.service.RedisService;
import com.hust.learningservice.dto.response.LeaderboardResponse;
import com.hust.learningservice.dto.response.UserDailyActivityResponse;
import com.hust.learningservice.dto.response.UserGamificationResponse;
import com.hust.learningservice.entity.UserDailyActivity;
import com.hust.learningservice.entity.UserGamificationProfile;
import com.hust.learningservice.enums.BadgeType;
import com.hust.learningservice.enums.GamificationLevel;
import com.hust.learningservice.repository.UserDailyActivityRepository;
import com.hust.learningservice.repository.UserGamificationProfileRepository;
import com.hust.learningservice.service.GamificationService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class GamificationServiceImpl implements GamificationService {

    private final UserGamificationProfileRepository profileRepository;
    private final UserDailyActivityRepository dailyActivityRepository;
    private final RedisService redisService;

    @Override
    @Async
    @Transactional
    @com.hust.commonlibrary.annotation.DistributedLock(key = "'gamification:' + #userId")
    public void processGamificationAndActivity(String userId, String lessonType) {
        log.info("Processing gamification and daily activities async for userId={}, lessonType={}", userId, lessonType);
        try {
            LocalDate today = LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh"));
            java.time.Instant todayStartInstant = today.atStartOfDay(ZoneId.of("Asia/Ho_Chi_Minh")).toInstant();

            // 1. Process Daily Activity
            UserDailyActivity dailyActivity = dailyActivityRepository.findByUserIdAndDate(userId, todayStartInstant)
                    .orElseGet(() -> UserDailyActivity.builder()
                            .userId(userId)
                            .date(todayStartInstant)
                            .activityCount(0)
                            .build());
            dailyActivity.setActivityCount(dailyActivity.getActivityCount() + 1);
            dailyActivityRepository.save(dailyActivity);

            // 2. Process Gamification Profile
            UserGamificationProfile profile = profileRepository.findByUserId(userId)
                    .orElseGet(() -> initNewProfile(userId));

            // A. Update Streak
            updateStreakLogic(profile, today);

            // B. Add XP based on lesson type
            long xpToAdd = 10; // Default
            if (lessonType != null) {
                xpToAdd = switch (lessonType.toUpperCase()) {
                    case "VIDEO" -> 10L;
                    case "DOCUMENT" -> 15L;
                    case "QUIZ" -> 30L;
                    case "ASSIGNMENT" -> 50L;
                    default -> 10L;
                };
            }
            profile.setTotalXp(profile.getTotalXp() + xpToAdd);

            // C. Check Level Up
            GamificationLevel nextCalculatedLevel = GamificationLevel.calculateLevelByXp(profile.getTotalXp());
            if (nextCalculatedLevel.getLevelIndex() > profile.getCurrentLevel().getLevelIndex()) {
                log.info("🎉 User {} leveled up from {} to {}!", userId, profile.getCurrentLevel(), nextCalculatedLevel);
                profile.setCurrentLevel(nextCalculatedLevel);
            }

            // D. Check Badges
            checkAndAwardBadges(profile);

            profileRepository.save(profile);
            log.info("Successfully completed gamification processing for userId={}", userId);
        } catch (Exception e) {
            log.error("Error occurred while processing gamification rules: {}", e.getMessage(), e);
        }
    }

    private UserGamificationProfile initNewProfile(String userId) {
        return UserGamificationProfile.builder()
                .userId(userId)
                .currentStreak(0)
                .longestStreak(0)
                .totalXp(0)
                .currentLevel(GamificationLevel.NOVICE)
                .earnedBadges(new HashSet<>())
                .build();
    }

    private void updateStreakLogic(UserGamificationProfile profile, LocalDate today) {
        java.time.Instant lastActiveInstant = profile.getLastActiveDate();
        LocalDate lastActive = lastActiveInstant != null
                ? lastActiveInstant.atZone(ZoneId.of("Asia/Ho_Chi_Minh")).toLocalDate()
                : null;

        java.time.Instant nowInstant = java.time.Instant.now();

        if (lastActive == null) {
            profile.setCurrentStreak(1);
            profile.setLongestStreak(1);
            profile.setLastActiveDate(nowInstant);
        } else if (lastActive.equals(today)) {
            log.debug("User {} already active today. Streak unchanged.", profile.getUserId());
            profile.setLastActiveDate(nowInstant);
        } else if (lastActive.equals(today.minusDays(1))) {
            int newStreak = profile.getCurrentStreak() + 1;
            profile.setCurrentStreak(newStreak);
            profile.setLongestStreak(Math.max(newStreak, profile.getLongestStreak()));
            profile.setLastActiveDate(nowInstant);
        } else {
            profile.setCurrentStreak(1);
            profile.setLastActiveDate(nowInstant);
        }
    }

    private void checkAndAwardBadges(UserGamificationProfile profile) {
        Set<BadgeType> badges = profile.getEarnedBadges();
        if (badges == null) {
            badges = new HashSet<>();
            profile.setEarnedBadges(badges);
        }

        if (!badges.contains(BadgeType.FIRST_BLOOD)) {
            badges.add(BadgeType.FIRST_BLOOD);
            log.info("Awarded FIRST_BLOOD badge to user {}", profile.getUserId());
        }

        if (profile.getCurrentStreak() >= 3 && !badges.contains(BadgeType.STREAK_3_DAYS)) {
            badges.add(BadgeType.STREAK_3_DAYS);
            log.info("Awarded STREAK_3_DAYS badge to user {}", profile.getUserId());
        }

        if (profile.getCurrentStreak() >= 7 && !badges.contains(BadgeType.STREAK_7_DAYS)) {
            badges.add(BadgeType.STREAK_7_DAYS);
            log.info("Awarded STREAK_7_DAYS badge to user {}", profile.getUserId());
        }

        if (profile.getCurrentStreak() >= 30 && !badges.contains(BadgeType.STREAK_30_DAYS)) {
            badges.add(BadgeType.STREAK_30_DAYS);
            log.info("Awarded STREAK_30_DAYS badge to user {}", profile.getUserId());
        }

        if (profile.getTotalXp() >= 1000 && !badges.contains(BadgeType.XP_1000)) {
            badges.add(BadgeType.XP_1000);
            log.info("Awarded XP_1000 badge to user {}", profile.getUserId());
        }

        if (profile.getTotalXp() >= 5000 && !badges.contains(BadgeType.XP_5000)) {
            badges.add(BadgeType.XP_5000);
            log.info("Awarded XP_5000 badge to user {}", profile.getUserId());
        }

        if (profile.getTotalXp() >= 10000 && !badges.contains(BadgeType.XP_10000)) {
            badges.add(BadgeType.XP_10000);
            log.info("Awarded XP_10000 badge to user {}", profile.getUserId());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public UserGamificationResponse getMyGamificationProfile(String userId) {
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh"));
        UserGamificationProfile profile = profileRepository.findByUserId(userId)
                .orElseGet(() -> initNewProfile(userId));

        java.time.Instant lastActiveInstant = profile.getLastActiveDate();
        LocalDate lastActive = lastActiveInstant != null
                ? lastActiveInstant.atZone(ZoneId.of("Asia/Ho_Chi_Minh")).toLocalDate()
                : null;

        boolean isStreakActiveToday = lastActive != null &&
                (lastActive.equals(today) || lastActive.equals(today.minusDays(1)));

        int visibleStreak = profile.getCurrentStreak();
        if (lastActive != null && !lastActive.equals(today) && !lastActive.equals(today.minusDays(1))) {
            visibleStreak = 0;
        }

        UserGamificationResponse.LevelInfo levelInfo = UserGamificationResponse.LevelInfo.builder()
                .code(profile.getCurrentLevel().name())
                .levelIndex(profile.getCurrentLevel().getLevelIndex())
                .title(profile.getCurrentLevel().getTitle())
                .minXpRequired(profile.getCurrentLevel().getMinXpRequired())
                .nextLevelXpRequired(profile.getCurrentLevel().getNextLevelXpRequired())
                .build();

        List<UserGamificationResponse.BadgeInfo> badgeInfos = profile.getEarnedBadges().stream()
                .map(badge -> UserGamificationResponse.BadgeInfo.builder()
                        .code(badge.name())
                        .displayName(badge.getDisplayName())
                        .description(badge.getDescription())
                        .iconUrl(badge.getIconUrl())
                        .build())
                .collect(Collectors.toList());

        java.time.Instant todayStartInstant = today.atStartOfDay(ZoneId.of("Asia/Ho_Chi_Minh")).toInstant();
        int todayActivitiesCount = dailyActivityRepository.findByUserIdAndDate(userId, todayStartInstant)
                .map(com.hust.learningservice.entity.UserDailyActivity::getActivityCount)
                .orElse(0);

        return UserGamificationResponse.builder()
                .currentStreak(visibleStreak)
                .longestStreak(profile.getLongestStreak())
                .isStreakActiveToday(isStreakActiveToday)
                .todayActivitiesCount(todayActivitiesCount)
                .totalXp(profile.getTotalXp())
                .currentLevel(levelInfo)
                .earnedBadges(badgeInfos)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserDailyActivityResponse> getMyDailyActivities(String userId, int year) {
        LocalDate startLocalDate = LocalDate.of(year, 1, 1);
        LocalDate endLocalDate = LocalDate.of(year, 12, 31);
        java.time.Instant start = startLocalDate.atStartOfDay(ZoneId.of("Asia/Ho_Chi_Minh")).toInstant();
        java.time.Instant end = endLocalDate.atTime(23, 59, 59).atZone(ZoneId.of("Asia/Ho_Chi_Minh")).toInstant();
        List<UserDailyActivity> list = dailyActivityRepository.findByUserIdAndDateBetween(userId, start, end);

        return list.stream()
                .map(activity -> UserDailyActivityResponse.builder()
                        .date(activity.getDate())
                        .count(activity.getActivityCount())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<LeaderboardResponse> getLeaderboard(String type) {
        log.info("Fetching leaderboard for type={}", type);
        List<UserGamificationProfile> profiles;
        if ("STREAK".equalsIgnoreCase(type)) {
            profiles = profileRepository.findTop10ByOrderByCurrentStreakDesc();
        } else {
            profiles = profileRepository.findTop10ByOrderByTotalXpDesc();
        }

        if (profiles.isEmpty()) {
            return Collections.emptyList();
        }

        List<LeaderboardResponse> leaderboard = new ArrayList<>();
        for (int i = 0; i < profiles.size(); i++) {
            UserGamificationProfile profile = profiles.get(i);
            leaderboard.add(LeaderboardResponse.builder()
                    .rank(i + 1)
                    .userId(profile.getUserId())
                    .totalXp(profile.getTotalXp())
                    .currentStreak(profile.getCurrentStreak())
                    .build());
        }

        enrichUserProfiles(leaderboard);

        return leaderboard;
    }

    private void enrichUserProfiles(List<LeaderboardResponse> leaderboard) {
        if (leaderboard == null || leaderboard.isEmpty()) {
            return;
        }

        List<String> userIds = leaderboard.stream()
                .map(LeaderboardResponse::getUserId)
                .distinct()
                .collect(Collectors.toList());

        List<String> redisKeys = userIds.stream()
                .map(RedisPrefixConstants::getSharedUserProfileKey)
                .collect(Collectors.toList());

        try {
            List<Object> cachedProfiles = redisService.multiGet(redisKeys);
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            Map<String, UserSharedProfile> userDetailsMap = new HashMap<>();

            for (int i = 0; i < userIds.size(); i++) {
                String uId = userIds.get(i);
                Object val = cachedProfiles != null && i < cachedProfiles.size() ? cachedProfiles.get(i) : null;
                if (val != null) {
                    UserSharedProfile profile;
                    if (val instanceof UserSharedProfile p) {
                        profile = p;
                    } else {
                        profile = mapper.convertValue(val, UserSharedProfile.class);
                    }
                    userDetailsMap.put(uId, profile);
                }
            }

            for (LeaderboardResponse entry : leaderboard) {
                UserSharedProfile details = userDetailsMap.get(entry.getUserId());
                if (details == null) {
                    details = UserSharedProfile.builder()
                            .firstName("Học viên")
                            .lastName("")
                            .avatar("")
                            .role("STUDENT")
                            .email("")
                            .build();
                }
                entry.setUserProfile(details);
            }
        } catch (Exception e) {
            log.error("Failed to enrich leaderboard user profiles from Redis", e);
            for (LeaderboardResponse entry : leaderboard) {
                if (entry.getUserProfile() == null) {
                    entry.setUserProfile(UserSharedProfile.builder()
                            .firstName("Học viên")
                            .lastName("")
                            .avatar("")
                            .role("STUDENT")
                            .email("")
                            .build());
                }
            }
        }
    }
}
