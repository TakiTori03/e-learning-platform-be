package com.hust.learningservice.utils;

import java.util.UUID;

public class AppUtils {
    public static String generateCode(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
