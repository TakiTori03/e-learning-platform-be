package com.hust.notificationservice.repository;

import com.hust.notificationservice.entity.Subscription;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubscriptionRepository extends MongoRepository<Subscription, String> {
    Optional<Subscription> findByEmail(String email);
    Optional<Subscription> findByUserId(String userId);
    List<Subscription> findAllByIsActiveTrue();
}
