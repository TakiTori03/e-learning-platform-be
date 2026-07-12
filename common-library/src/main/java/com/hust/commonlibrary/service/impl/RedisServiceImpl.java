package com.hust.commonlibrary.service.impl;

import com.hust.commonlibrary.constants.AppConstants;
import com.hust.commonlibrary.service.RedisService;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

public class RedisServiceImpl implements RedisService {

    private static final RedisScript<Long> RATE_LIMIT_SCRIPT = new DefaultRedisScript<>(
            "local current = redis.call('INCR', KEYS[1])\n" +
            "if tonumber(current) == 1 then\n" +
            "    redis.call('EXPIRE', KEYS[1], tonumber(ARGV[1]))\n" +
            "end\n" +
            "return current",
            Long.class
    );

    private final RedisTemplate<String, Object> redisTemplate;
    private final String keyPrefix;

    public RedisServiceImpl(RedisTemplate<String, Object> redisTemplate, String applicationName) {
        this.redisTemplate = redisTemplate;
        this.keyPrefix = AppConstants.Redis_Constants.APP_PREFIX + applicationName + ":";
    }

    private String getFullKey(String key) {
        if (key == null) return null;
        if (key.startsWith(AppConstants.Redis_Constants.APP_PREFIX)) return key;
        return keyPrefix + key;
    }

    @Override
    public void set(String key, Object value) {
        redisTemplate.opsForValue().set(getFullKey(key), value);
    }

    @Override
    public void set(String key, Object value, long timeout, TimeUnit unit) {
        redisTemplate.opsForValue().set(getFullKey(key), value, timeout, unit);
    }

    @Override
    public boolean setIfAbsent(String key, Object value, long timeout, TimeUnit unit) {
        return Boolean.TRUE.equals(redisTemplate.opsForValue().setIfAbsent(getFullKey(key), value, timeout, unit));
    }

    @Override
    public Object get(String key) {
        return redisTemplate.opsForValue().get(getFullKey(key));
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T get(String key, Class<T> clazz) {
        Object value = redisTemplate.opsForValue().get(getFullKey(key));
        if (value == null) return null;
        return (T) value;
    }

    @Override
    public void delete(String key) {
        redisTemplate.delete(getFullKey(key));
    }

    @Override
    public boolean hasKey(String key) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(getFullKey(key)));
    }

    @Override
    public boolean expire(String key, long timeout, TimeUnit unit) {
        return Boolean.TRUE.equals(redisTemplate.expire(getFullKey(key), timeout, unit));
    }

    @Override
    public Long getExpire(String key) {
        return redisTemplate.getExpire(getFullKey(key));
    }

    @Override
    public Long increment(String key, long delta) {
        return redisTemplate.opsForValue().increment(getFullKey(key), delta);
    }

    @Override
    public Long incrementAndExpire(String key, long expireSeconds) {
        return redisTemplate.execute(
                RATE_LIMIT_SCRIPT,
                Collections.singletonList(getFullKey(key)),
                expireSeconds
        );
    }

    @Override
    public java.util.List<Object> multiGet(java.util.List<String> keys) {
        if (keys == null || keys.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        java.util.List<String> fullKeys = keys.stream().map(this::getFullKey).toList();
        return redisTemplate.opsForValue().multiGet(fullKeys);
    }

    @Override
    public java.util.Set<String> keys(String pattern) {
        return redisTemplate.keys(getFullKey(pattern));
    }

    @Override
    public Long decrement(String key, long delta) {
        return redisTemplate.opsForValue().decrement(getFullKey(key), delta);
    }
}
