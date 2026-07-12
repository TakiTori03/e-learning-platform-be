package com.hust.commonlibrary.service;

import java.util.concurrent.TimeUnit;

public interface RedisService {
    // String Operations
    void set(String key, Object value);
    void set(String key, Object value, long timeout, TimeUnit unit);
    boolean setIfAbsent(String key, Object value, long timeout, TimeUnit unit);
    
    Object get(String key);
    <T> T get(String key, Class<T> clazz);
    java.util.List<Object> multiGet(java.util.List<String> keys);
    
    // Key Management
    void delete(String key);
    boolean hasKey(String key);
    boolean expire(String key, long timeout, TimeUnit unit);
    Long getExpire(String key);
    
    // Counter (Huu dung cho rate limit, luot xem)
    Long increment(String key, long delta);

    // Atomic Rate Limiting (Lua script execution)
    Long incrementAndExpire(String key, long expireSeconds);

    // Dynamic key discovery and decrement helpers
    java.util.Set<String> keys(String pattern);
    Long decrement(String key, long delta);
}
