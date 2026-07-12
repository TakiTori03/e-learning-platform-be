package com.hust.notificationservice.service;

import com.hust.notificationservice.entity.Subscription;

public interface SubscriptionService {
    Subscription subscribe(String email, String userId);
    void unsubscribe(String email);
    Subscription getMySubscription(String userId);
}
