package com.hust.learningservice.repository;

import com.hust.learningservice.entity.UserDailyActivity;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserDailyActivityRepository extends MongoRepository<UserDailyActivity, String> {
    Optional<UserDailyActivity> findByUserIdAndDate(String userId, Instant date);
    List<UserDailyActivity> findByUserIdAndDateBetween(String userId, Instant start, Instant end);
}
