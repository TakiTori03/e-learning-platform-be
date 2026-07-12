package com.hust.learningservice.entity;

import com.hust.commonlibrary.entity.BaseDocument;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "user_daily_activities")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@CompoundIndex(name = "user_date_idx", def = "{'userId': 1, 'date': 1}", unique = true)
public class UserDailyActivity extends BaseDocument {

    private String userId;
    private Instant date;
    private int activityCount;
}
