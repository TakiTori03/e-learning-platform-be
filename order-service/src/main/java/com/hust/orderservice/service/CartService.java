package com.hust.orderservice.service;

import java.util.Set;

public interface CartService {
    Set<String> getCart(String userId);
    Set<String> syncCart(String userId, Set<String> localCourseIds);
    Set<String> addCourseToCart(String userId, String courseId);
    Set<String> removeCourseFromCart(String userId, String courseId);
    void clearCart(String userId);
}
