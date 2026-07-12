package com.hust.learningservice.enums;

import lombok.Getter;

@Getter
public enum BadgeType {
    FIRST_BLOOD("Bài Học Đầu Tiên", "Hoàn thành bài học đầu tiên", "https://lwfiles.mycourse.app/65ac73296e5c564383a8e28b-public/badges/newbie.png"),
    STREAK_3_DAYS("Khởi Động", "Duy trì học liên tục 3 ngày", "https://thumb.ac-illust.com/d9/d9577885428afb171e9d09dad899ee1e_t.jpeg"),
    STREAK_7_DAYS("Chiến Binh Chăm Chỉ", "Duy trì học liên tục 7 ngày", "https://thumbs.dreamstime.com/b/print-235466646.jpg"),
    STREAK_30_DAYS("Thói Quen Sắt Đá", "Duy trì học liên tục 30 ngày", "https://cdn-icons-png.flaticon.com/512/1477/1477224.png"),
    XP_1000("Học Giả Tập Sự", "Tích lũy đạt mốc 1,000 XP đầu tiên", "https://cdn-icons-png.flaticon.com/512/3676/3676059.png"),
    XP_5000("Học Giả Uyên Bác", "Tích lũy đạt mốc 5,000 XP học tập", "https://cdn-icons-png.flaticon.com/512/1477/1477174.png"),
    XP_10000("Bậc Thầy Học Thuật", "Tích lũy đạt cột mốc 10,000 XP", "https://cdn-icons-png.flaticon.com/512/2583/2583264.png");

    private final String displayName;
    private final String description;
    private final String iconUrl;

    BadgeType(String displayName, String description, String iconUrl) {
        this.displayName = displayName;
        this.description = description;
        this.iconUrl = iconUrl;
    }
}
