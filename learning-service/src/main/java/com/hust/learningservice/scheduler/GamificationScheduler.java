package com.hust.learningservice.scheduler;

import com.hust.learningservice.entity.UserGamificationProfile;
import com.hust.learningservice.repository.UserGamificationProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class GamificationScheduler {

    private final UserGamificationProfileRepository profileRepository;

    /**
     * Run daily at 00:05 AM (Asia/Ho_Chi_Minh) to validate and reset expired streaks.
     */
    @Scheduled(cron = "0 5 0 * * *", zone = "Asia/Ho_Chi_Minh")
    public void resetExpiredStreaks() {
        log.info("Starting daily gamification streak validation job...");
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh"));
        LocalDate yesterday = today.minusDays(1);

        List<UserGamificationProfile> activeProfiles = profileRepository.findAllByCurrentStreakGreaterThan(0);
        int resetCount = 0;

        for (UserGamificationProfile profile : activeProfiles) {
            Instant lastActiveInstant = profile.getLastActiveDate();
            if (lastActiveInstant == null) {
                profile.setCurrentStreak(0);
                profileRepository.save(profile);
                resetCount++;
                continue;
            }

            LocalDate lastActive = lastActiveInstant.atZone(ZoneId.of("Asia/Ho_Chi_Minh")).toLocalDate();
            if (!lastActive.equals(today) && !lastActive.equals(yesterday)) {
                log.info("Resetting streak for user: {} (last active was: {})", profile.getUserId(), lastActive);
                profile.setCurrentStreak(0);
                profileRepository.save(profile);
                resetCount++;
            }
        }

        log.info("Daily gamification streak validation finished. Reset {} profiles.", resetCount);
    }
}
