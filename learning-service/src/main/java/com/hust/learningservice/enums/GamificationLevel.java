package com.hust.learningservice.enums;

import lombok.Getter;

@Getter
public enum GamificationLevel {
    NOVICE(1, 0, "Tân Binh"),
    APPRENTICE(2, 100, "Tập Sự"),
    SCHOLAR(3, 500, "Học Giả"),
    EXPERT(4, 1500, "Chuyên Gia"),
    MASTER(5, 4000, "Cao Thủ");

    private final int levelIndex;
    private final long minXpRequired;
    private final String title;

    GamificationLevel(int levelIndex, long minXpRequired, String title) {
        this.levelIndex = levelIndex;
        this.minXpRequired = minXpRequired;
        this.title = title;
    }

    public long getNextLevelXpRequired() {
        int nextIndex = this.levelIndex + 1;
        for (GamificationLevel level : values()) {
            if (level.getLevelIndex() == nextIndex) {
                return level.getMinXpRequired();
            }
        }
        return 10000L; // Ceiling limit for MASTER level
    }

    public static GamificationLevel calculateLevelByXp(long totalXp) {
        GamificationLevel currentLevel = NOVICE;
        for (GamificationLevel level : values()) {
            if (totalXp >= level.getMinXpRequired()) {
                currentLevel = level;
            } else {
                break;
            }
        }
        return currentLevel;
    }
}
