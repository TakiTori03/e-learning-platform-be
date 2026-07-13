package com.hust.orderservice.utils;

import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.*;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class VNPAYUtils {

    /**
     * Hashing data using HMAC-SHA512
     */
    public static String hmacSHA512(final String key, final String data) {
        try {
            if (key == null || data == null) {
                throw new NullPointerException();
            }
            final Mac hmac512 = Mac.getInstance("HmacSHA512");
            byte[] hmacKeyBytes = key.getBytes(StandardCharsets.UTF_8);
            final SecretKeySpec secretKey = new SecretKeySpec(hmacKeyBytes, "HmacSHA512");
            hmac512.init(secretKey);
            byte[] dataBytes = data.getBytes(StandardCharsets.UTF_8);
            byte[] result = hmac512.doFinal(dataBytes);
            StringBuilder sb = new StringBuilder(2 * result.length);
            for (byte b : result) {
                sb.append(String.format("%02X", b & 0xff));
            }
            return sb.toString();
        } catch (Exception ex) {
            return "";
        }
    }

    /**
     * Build the raw data string for hashing (v2.1.0 standard: sorted keys, raw values)
     */
    public static String hashAllFields(String hashSecret, Map<String, String> fields) {
        try {
            List<String> fieldNames = new ArrayList<>(fields.keySet());
            Collections.sort(fieldNames);
            StringBuilder hashData = new StringBuilder();
            for (String fieldName : fieldNames) {
                String fieldValue = fields.get(fieldName);
                if (fieldValue != null && fieldValue.length() > 0) {
                    if (hashData.length() > 0) {
                        hashData.append('&');
                    }
                    hashData.append(fieldName);
                    hashData.append('=');
                    hashData.append(URLEncoder.encode(fieldValue, StandardCharsets.UTF_8.toString()));
                }
            }
            return hmacSHA512(hashSecret, hashData.toString());
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * Build the query string for URL (UTF-8, space encoded as +)
     */
    public static String buildQueryUrl(Map<String, String> fields) {
        try {
            List<String> fieldNames = new ArrayList<>(fields.keySet());
            Collections.sort(fieldNames);
            StringBuilder query = new StringBuilder();
            for (String fieldName : fieldNames) {
                String fieldValue = fields.get(fieldName);
                if (fieldValue != null && fieldValue.length() > 0) {
                    if (query.length() > 0) {
                        query.append('&');
                    }
                    query.append(URLEncoder.encode(fieldName, StandardCharsets.UTF_8.toString()));
                    query.append('=');
                    query.append(URLEncoder.encode(fieldValue, StandardCharsets.UTF_8.toString()));
                }
            }
            return query.toString();
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * Compress UUID (36 chars) to Base64 (22 chars) to fit VNPay's 30-char limit
     */
    public static String encodeOrderId(String orderId) {
        try {
            UUID uuid = UUID.fromString(orderId);
            ByteBuffer bb = ByteBuffer.wrap(new byte[16]);
            bb.putLong(uuid.getMostSignificantBits());
            bb.putLong(uuid.getLeastSignificantBits());
            return Base64.getUrlEncoder().withoutPadding().encodeToString(bb.array());
        } catch (Exception e) {
            return orderId; // Fallback if not a UUID
        }
    }

    /**
     * Decompress Base64 (22 chars) back to UUID (36 chars)
     */
    public static String decodeOrderId(String encoded) {
        if (encoded == null) return null;
        if (encoded.length() == 36) {
            try {
                UUID.fromString(encoded);
                return encoded;
            } catch (Exception ignored) {}
        }
        try {
            byte[] bytes = Base64.getUrlDecoder().decode(encoded);
            ByteBuffer bb = ByteBuffer.wrap(bytes);
            UUID uuid = new UUID(bb.getLong(), bb.getLong());
            return uuid.toString();
        } catch (Exception e) {
            return encoded; // Fallback
        }
    }
}
