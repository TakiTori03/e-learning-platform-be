package com.hust.interactionservice.utils;

import java.util.UUID;


public class AppUtils {
    public static String generateCode(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
