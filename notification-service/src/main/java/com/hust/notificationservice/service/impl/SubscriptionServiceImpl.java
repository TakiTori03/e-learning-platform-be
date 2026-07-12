package com.hust.notificationservice.service.impl;

import com.hust.notificationservice.entity.Subscription;
import com.hust.notificationservice.repository.SubscriptionRepository;
import com.hust.notificationservice.service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Arrays;

@Service
@RequiredArgsConstructor
public class SubscriptionServiceImpl implements SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;

    @Override
    public Subscription subscribe(String email, String userId) {
        return subscriptionRepository.findByEmail(email)
                .map(sub -> {
                    sub.setIsActive(true);
                    if (userId != null) {
                        sub.setUserId(userId);
                    }
                    return subscriptionRepository.save(sub);
                })
                .orElseGet(() -> {
                    Subscription newSub = Subscription.builder()
                            .email(email)
                            .userId(userId)
                            .topics(Arrays.asList("NEWSLETTER", "COURSE_UPDATE"))
                            .isActive(true)
                            .build();
                    return subscriptionRepository.save(newSub);
                });
    }

    @Override
    public void unsubscribe(String email) {
        subscriptionRepository.findByEmail(email).ifPresent(sub -> {
            sub.setIsActive(false);
            subscriptionRepository.save(sub);
        });
    }

    @Override
    public Subscription getMySubscription(String userId) {
        return subscriptionRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Subscription not found for user: " + userId));
    }
}
