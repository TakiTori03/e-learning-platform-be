package com.hust.learningservice.entity;

import com.hust.commonlibrary.entity.BaseDocument;
import com.hust.learningservice.enums.BadgeType;
import com.hust.learningservice.enums.GamificationLevel;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.HashSet;
import java.util.Set;

@Document(collection = "user_gamification_profiles")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class UserGamificationProfile extends BaseDocument {

    @Indexed(unique = true)
    private String userId;

    private int currentStreak;
    private int longestStreak;
    private java.time.Instant lastActiveDate;

    private long totalXp;

    @Builder.Default
    private GamificationLevel currentLevel = GamificationLevel.NOVICE;

    @Builder.Default
    private Set<BadgeType> earnedBadges = new HashSet<>();
}
