package com.hust.commonlibrary.utils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry dùng để lưu trữ tạm thời JWT Token theo sessionId (MemoryId)
 * nhằm giải quyết vấn đề mất context (ThreadLocal) của Spring Security 
 * khi LangChain4j thực thi Tool trên background threads độc lập.
 */
public class UserTokenRegistry {

    private static final Map<String, String> sessionTokenMap = new ConcurrentHashMap<>();
    private static final ThreadLocal<String> currentToolToken = new ThreadLocal<>();

    public static void register(String sessionId, String token) {
        if (sessionId != null && token != null) {
            sessionTokenMap.put(sessionId, token);
        }
    }

    public static String getToken(String sessionId) {
        if (sessionId == null) {
            return null;
        }
        return sessionTokenMap.get(sessionId);
    }

    public static void unregister(String sessionId) {
        if (sessionId != null) {
            sessionTokenMap.remove(sessionId);
        }
    }

    public static void setCurrentToolToken(String token) {
        if (token != null) {
            currentToolToken.set(token);
        }
    }

    public static String getCurrentToolToken() {
        return currentToolToken.get();
    }

    public static void clearCurrentToolToken() {
        currentToolToken.remove();
    }
}
