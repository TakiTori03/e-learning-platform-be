package com.hust.learningservice.repository;

import com.hust.learningservice.entity.UserGamificationProfile;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserGamificationProfileRepository extends MongoRepository<UserGamificationProfile, String> {
    Optional<UserGamificationProfile> findByUserId(String userId);
    List<UserGamificationProfile> findAllByCurrentStreakGreaterThan(int streak);
    List<UserGamificationProfile> findTop10ByOrderByTotalXpDesc();
    List<UserGamificationProfile> findTop10ByOrderByCurrentStreakDesc();
}
